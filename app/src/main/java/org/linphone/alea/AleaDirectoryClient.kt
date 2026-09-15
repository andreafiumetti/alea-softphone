package org.linphone.alea

import org.json.JSONObject
import org.linphone.core.tools.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/** GET /api/softphone/directory — rubrica interni ALEA (+ online). */
object AleaDirectoryClient {
    private const val TAG = "[ALEA Directory]"

    data class Entry(
        val extension: String,
        val name: String,
        val online: Boolean = false
    )

    class DirectoryException(message: String) : Exception(message)

    fun fetch(apiBase: String, appToken: String): List<Entry> {
        val base = apiBase.trimEnd('/')
        val url = URL("$base/api/softphone/directory")
        Log.i("$TAG GET $url")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $appToken")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() }.orEmpty()
            if (code !in 200..299) {
                val err = try {
                    JSONObject(text).optString("error").ifBlank { "Directory fallita ($code)" }
                } catch (_: Exception) {
                    "Directory fallita ($code)"
                }
                throw DirectoryException(err)
            }
            val json = JSONObject(text)
            val arr = json.optJSONArray("items") ?: return emptyList()
            val out = ArrayList<Entry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val ext = o.optString("extension").trim()
                if (ext.isEmpty()) continue
                val name = o.optString("name").ifBlank { ext }
                val online = o.optBoolean("online", false)
                out.add(Entry(extension = ext, name = name, online = online))
            }
            Log.i("$TAG Loaded ${out.size} entries")
            return out
        } finally {
            conn.disconnect()
        }
    }
}
