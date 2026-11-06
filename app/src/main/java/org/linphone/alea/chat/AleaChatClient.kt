package org.linphone.alea.chat

import org.json.JSONArray
import org.json.JSONObject
import org.linphone.core.tools.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

/** REST client for ALEA PBX group chat (no Linphone/Flexisip). */
object AleaChatClient {
    private const val TAG = "[ALEA Chat]"

    data class Group(
        val id: String,
        val name: String,
        val createdByExtensionId: String,
        val memberCount: Int,
        val updatedAt: String
    )

    data class Member(
        val extensionId: String,
        val extension: String,
        val displayName: String
    )

    data class Attachment(
        val id: String,
        val filename: String,
        val mime: String,
        val sizeBytes: Long,
        val expiresAt: String?,
        val available: Boolean
    )

    data class Message(
        val id: String,
        val senderExtensionId: String,
        val senderExtension: String,
        val senderDisplayName: String,
        val body: String?,
        val createdAt: String,
        val attachment: Attachment?
    )

    class ChatException(message: String) : Exception(message)

    fun listGroups(apiBase: String, appToken: String): List<Group> {
        val json = JSONObject(request("GET", "${apiBase.trimEnd('/')}/api/softphone/chat/groups", appToken))
        val arr = json.optJSONArray("groups") ?: JSONArray()
        val out = ArrayList<Group>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").trim()
            if (id.isEmpty()) continue
            out.add(
                Group(
                    id = id,
                    name = o.optString("name"),
                    createdByExtensionId = o.optString("created_by_extension_id"),
                    memberCount = o.optInt("member_count", 0),
                    updatedAt = o.optString("updated_at")
                )
            )
        }
        return out
    }

    fun createGroup(apiBase: String, appToken: String, name: String, memberExtensions: List<String>): String {
        val body = JSONObject()
            .put("name", name)
            .put("member_extensions", JSONArray(memberExtensions))
        val text = request(
            "POST",
            "${apiBase.trimEnd('/')}/api/softphone/chat/groups",
            appToken,
            body.toString(),
            "application/json"
        )
        return JSONObject(text).optString("id")
    }

    /** Creator deletes group; other members leave. Returns "deleted" or "left". */
    fun deleteOrLeaveGroup(apiBase: String, appToken: String, groupId: String): String {
        val text = request(
            "DELETE",
            "${apiBase.trimEnd('/')}/api/softphone/chat/groups/$groupId",
            appToken
        )
        if (text.isBlank()) return "deleted"
        return JSONObject(text).optString("status", "deleted")
    }

    fun listMessages(apiBase: String, appToken: String, groupId: String, since: String? = null): List<Message> {
        var url = "${apiBase.trimEnd('/')}/api/softphone/chat/groups/$groupId/messages?limit=200"
        if (!since.isNullOrBlank()) {
            url += "&since=${java.net.URLEncoder.encode(since, "UTF-8")}"
        }
        val json = JSONObject(request("GET", url, appToken))
        val arr = json.optJSONArray("messages") ?: JSONArray()
        val out = ArrayList<Message>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").trim()
            if (id.isEmpty()) continue
            val attObj = o.optJSONObject("attachment")
            val att = if (attObj != null && !attObj.optString("id").isNullOrBlank()) {
                Attachment(
                    id = attObj.optString("id"),
                    filename = attObj.optString("filename"),
                    mime = attObj.optString("mime"),
                    sizeBytes = attObj.optLong("size_bytes", 0),
                    expiresAt = attObj.optString("expires_at").takeIf { it.isNotBlank() },
                    available = attObj.optBoolean("available", false)
                )
            } else {
                null
            }
            out.add(
                Message(
                    id = id,
                    senderExtensionId = o.optString("sender_extension_id"),
                    senderExtension = o.optString("sender_extension"),
                    senderDisplayName = o.optString("sender_display_name"),
                    body = o.optString("body").takeIf { it.isNotBlank() && o.has("body") && !o.isNull("body") },
                    createdAt = o.optString("created_at"),
                    attachment = att
                )
            )
        }
        return out
    }

    fun postText(apiBase: String, appToken: String, groupId: String, body: String): Message {
        val payload = JSONObject().put("body", body)
        val text = request(
            "POST",
            "${apiBase.trimEnd('/')}/api/softphone/chat/groups/$groupId/messages",
            appToken,
            payload.toString(),
            "application/json"
        )
        return parsePosted(text)
    }

    fun postFile(
        apiBase: String,
        appToken: String,
        groupId: String,
        body: String?,
        file: File,
        mime: String
    ): Message {
        val boundary = "alea" + UUID.randomUUID().toString().replace("-", "")
        val url = URL("${apiBase.trimEnd('/')}/api/softphone/chat/groups/$groupId/messages")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = TimeUnit.SECONDS.toMillis(30).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(120).toInt()
            setRequestProperty("Authorization", "Bearer $appToken")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            DataOutputStream(conn.outputStream).use { out ->
                if (!body.isNullOrBlank()) {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"body\"\r\n\r\n")
                    out.write(body.toByteArray(Charsets.UTF_8))
                    out.writeBytes("\r\n")
                }
                out.writeBytes("--$boundary\r\n")
                out.writeBytes(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n"
                )
                out.writeBytes("Content-Type: $mime\r\n\r\n")
                FileInputStream(file).use { it.copyTo(out) }
                out.writeBytes("\r\n--$boundary--\r\n")
                out.flush()
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() }.orEmpty()
            if (code !in 200..299) {
                throw ChatException(parseError(text, code))
            }
            return parsePosted(text)
        } finally {
            conn.disconnect()
        }
    }

    fun downloadAttachment(
        apiBase: String,
        appToken: String,
        attachmentId: String,
        destFile: File
    ) {
        val url = URL("${apiBase.trimEnd('/')}/api/softphone/chat/attachments/$attachmentId")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TimeUnit.SECONDS.toMillis(30).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(120).toInt()
            setRequestProperty("Authorization", "Bearer $appToken")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.let {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText()
                }.orEmpty()
                throw ChatException(parseError(err, code))
            }
            destFile.parentFile?.mkdirs()
            conn.inputStream.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parsePosted(text: String): Message {
        val o = JSONObject(text)
        val attObj = o.optJSONObject("attachment")
        val att = if (attObj != null) {
            Attachment(
                id = attObj.optString("id"),
                filename = attObj.optString("filename"),
                mime = attObj.optString("mime"),
                sizeBytes = attObj.optLong("size_bytes", 0),
                expiresAt = attObj.optString("expires_at").takeIf { it.isNotBlank() },
                available = attObj.optBoolean("available", true)
            )
        } else {
            null
        }
        return Message(
            id = o.optString("id"),
            senderExtensionId = "",
            senderExtension = "",
            senderDisplayName = "",
            body = o.optString("body").takeIf { it.isNotBlank() && !o.isNull("body") },
            createdAt = o.optString("created_at"),
            attachment = att
        )
    }

    private fun request(
        method: String,
        urlStr: String,
        appToken: String,
        body: String? = null,
        contentType: String? = null
    ): String {
        Log.i("$TAG $method $urlStr")
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(60).toInt()
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $appToken")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType ?: "application/json")
            }
        }
        try {
            if (body != null) {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() }.orEmpty()
            if (code !in 200..299 && code != 204) {
                throw ChatException(parseError(text, code))
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun parseError(text: String, code: Int): String =
        try {
            JSONObject(text).optString("error").ifBlank { "Chat errore ($code)" }
        } catch (_: Exception) {
            "Chat errore ($code)"
        }
}
