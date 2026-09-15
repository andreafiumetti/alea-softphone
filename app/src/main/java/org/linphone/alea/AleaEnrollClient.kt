package org.linphone.alea

import android.os.Build
import org.json.JSONObject
import org.linphone.core.tools.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * POST /api/softphone/enroll — contratto PBX ALEA (invariato).
 */
object AleaEnrollClient {
    private const val TAG = "[ALEA Enroll]"

    data class Result(
        val extension: String,
        val displayName: String,
        val sipUsername: String,
        val sipPassword: String,
        val sipHost: String,
        val sipPort: Int,
        val transport: String,
        val deviceId: String,
        val appToken: String,
        val apiBase: String
    )

    class EnrollException(message: String) : Exception(message)

    fun enroll(apiBase: String, token: String): Result {
        val base = apiBase.trimEnd('/')
        val url = URL("$base/api/softphone/enroll")
        val body = JSONObject()
            .put("token", token)
            .put("device_name", Build.MODEL ?: "Android")
            .put("device_model", Build.DEVICE ?: "")
            .put("platform", "android")
            .put("device_uuid", UUID.randomUUID().toString())
            .toString()

        Log.i("$TAG POST $url")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() }.orEmpty()
            if (code !in 200..299) {
                val err = try {
                    JSONObject(text).optString("error").ifBlank { "Enroll fallito ($code)" }
                } catch (_: Exception) {
                    "Enroll fallito ($code)"
                }
                throw EnrollException(err)
            }
            val json = JSONObject(text)
            return Result(
                extension = json.optString("extension"),
                displayName = json.optString("display_name"),
                sipUsername = json.optString("sip_username").ifBlank { json.optString("extension") },
                sipPassword = json.optString("sip_password"),
                sipHost = json.optString("sip_host"),
                sipPort = json.optInt("sip_port", 5060),
                transport = json.optString("transport", "udp").ifBlank { "udp" },
                deviceId = json.optString("device_id"),
                appToken = json.optString("app_token"),
                apiBase = json.optString("api_base").ifBlank { base }
            )
        } finally {
            conn.disconnect()
        }
    }
}
