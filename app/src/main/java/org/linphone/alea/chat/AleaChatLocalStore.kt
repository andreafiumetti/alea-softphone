package org.linphone.alea.chat

import android.content.Context
import org.json.JSONObject
import java.io.File

/** Local-only history index + downloaded attachment files (device sandbox). */
object AleaChatLocalStore {
    private const val PREFS = "alea_chat_local"

    fun filesDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, "alea-chat")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun attachmentFile(context: Context, attachmentId: String, filename: String): File {
        val safe = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(filesDir(context), "${attachmentId}_$safe")
    }

    fun markDownloaded(context: Context, attachmentId: String, path: String) {
        prefs(context).edit().putString("att_$attachmentId", path).apply()
    }

    fun localPath(context: Context, attachmentId: String): String? =
        prefs(context).getString("att_$attachmentId", null)

    fun cacheMessages(context: Context, groupId: String, messagesJson: String) {
        prefs(context).edit().putString("msgs_$groupId", messagesJson).apply()
    }

    fun cachedMessages(context: Context, groupId: String): String? =
        prefs(context).getString("msgs_$groupId", null)

    fun appendCachedMessage(context: Context, groupId: String, message: AleaChatClient.Message) {
        val existing = cachedMessages(context, groupId)
        val arr = try {
            if (existing != null) org.json.JSONArray(JSONObject(existing).optJSONArray("messages") ?: org.json.JSONArray())
            else org.json.JSONArray()
        } catch (_: Exception) {
            org.json.JSONArray()
        }
        // Keep simple: refresh from network preferred; cache is best-effort last list
        val root = JSONObject().put("messages", arr)
        cacheMessages(context, groupId, root.toString())
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
