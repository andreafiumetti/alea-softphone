package org.linphone.alea

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import org.linphone.core.tools.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Registra il token FCM su ALEA PBX (`POST /api/softphone/push/register`).
 * No-op se enroll assente o Firebase non inizializzato (manca google-services.json ALEA).
 */
object AleaPushRegistrar {
    private const val TAG = "[ALEA Push]"

    fun registerAsync(context: Context) {
        val appCtx = context.applicationContext
        Thread({
            try {
                registerBlocking(appCtx)
            } catch (e: Exception) {
                Log.w("$TAG register failed: ${e.message}")
            }
        }, "alea-fcm-register").apply { isDaemon = true }.start()
    }

    fun registerBlocking(context: Context) {
        val session = AleaEnrollStore.currentSession(context) ?: run {
            Log.i("$TAG skip: no enroll session")
            return
        }
        if (session.deviceId.isBlank()) {
            Log.w("$TAG skip: empty device_id")
            return
        }
        val token = try {
            val task = FirebaseMessaging.getInstance().token
            // Block briefly on worker thread
            var out: String? = null
            var err: Exception? = null
            val latch = java.util.concurrent.CountDownLatch(1)
            task.addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    out = t.result
                } else {
                    err = t.exception ?: Exception("FCM token failed")
                }
                latch.countDown()
            }
            if (!latch.await(25, TimeUnit.SECONDS)) {
                throw Exception("FCM token timeout")
            }
            err?.let { throw it }
            out?.trim().orEmpty()
        } catch (e: Exception) {
            Log.w("$TAG Firebase token unavailable (configurare google-services.json ALEA): ${e.message}")
            return
        }
        if (token.isEmpty()) {
            Log.w("$TAG empty FCM token")
            return
        }
        postRegister(session, token)
    }

    private fun postRegister(session: AleaEnrollStore.Session, pushToken: String) {
        val url = URL("${session.apiBase.trimEnd('/')}/api/softphone/push/register")
        val body = JSONObject()
            .put("device_id", session.deviceId)
            .put("push_token", pushToken)
            .put("platform", "android")
            .put("kind", "voip")
            .toString()
        Log.i("$TAG POST $url")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${session.appToken}")
        }
        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() }.orEmpty()
            if (code !in 200..299) {
                Log.w("$TAG register HTTP $code: $text")
                return
            }
            Log.i("$TAG registered FCM token len=${pushToken.length}")
        } finally {
            conn.disconnect()
        }
    }
}
