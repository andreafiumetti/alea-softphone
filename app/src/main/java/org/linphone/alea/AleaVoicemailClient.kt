package org.linphone.alea

import org.json.JSONObject
import org.linphone.core.tools.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/** API casella vocale visuale (lista / play / letto / elimina). */
object AleaVoicemailClient {
    private const val TAG = "[ALEA Voicemail]"

    data class Message(
        val id: String,
        val callerId: String,
        val durationSec: Int,
        val createdAt: String,
        val read: Boolean,
        val transcript: String?,
        val transcriptStatus: String
    )

    data class Inbox(
        val unread: Int,
        val items: List<Message>
    )

    class VoicemailException(message: String) : Exception(message)

    fun list(apiBase: String, appToken: String): Inbox {
        val base = apiBase.trimEnd('/')
        val text = request("GET", "$base/api/softphone/voicemail", appToken)
        val json = JSONObject(text)
        val arr = json.optJSONArray("items")
        val out = ArrayList<Message>(arr?.length() ?: 0)
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id").trim()
                if (id.isEmpty()) continue
                out.add(
                    Message(
                        id = id,
                        callerId = o.optString("caller_id").ifBlank { "Sconosciuto" },
                        durationSec = o.optInt("duration_sec", 0),
                        createdAt = o.optString("created_at"),
                        read = o.optBoolean("read", false),
                        transcript = o.optString("transcript").takeIf { it.isNotBlank() },
                        transcriptStatus = o.optString("transcript_status", "pending")
                    )
                )
            }
        }
        return Inbox(unread = json.optInt("unread", 0), items = out)
    }

    fun audioUrl(apiBase: String, messageId: String): String =
        "${apiBase.trimEnd('/')}/api/softphone/voicemail/$messageId/audio"

    fun markRead(apiBase: String, appToken: String, messageId: String) {
        request("POST", "${apiBase.trimEnd('/')}/api/softphone/voicemail/$messageId/read", appToken)
    }

    fun delete(apiBase: String, appToken: String, messageId: String) {
        request("DELETE", "${apiBase.trimEnd('/')}/api/softphone/voicemail/$messageId", appToken)
    }

    private fun request(method: String, urlStr: String, appToken: String): String {
        Log.i("$TAG $method $urlStr")
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(30).toInt()
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $appToken")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() }.orEmpty()
            if (code !in 200..299 && code != 204) {
                val err = try {
                    JSONObject(text).optString("error").ifBlank { "Voicemail fallita ($code)" }
                } catch (_: Exception) {
                    "Voicemail fallita ($code)"
                }
                throw VoicemailException(err)
            }
            return text
        } finally {
            conn.disconnect()
        }
    }
}
