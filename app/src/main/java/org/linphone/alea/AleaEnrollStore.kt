package org.linphone.alea

import android.content.Context
import org.linphone.core.tools.Log
import java.util.Locale

/** Persistenza minima enroll (push/API/directory) — niente password SIP qui. */
object AleaEnrollStore {
    private const val PREFS = "alea_enroll"
    private const val TAG = "[ALEA Enroll Store]"

    data class Session(
        val extension: String,
        val sipHost: String,
        val apiBase: String,
        val appToken: String,
        val deviceId: String = ""
    )

    fun save(context: Context, result: AleaEnrollClient.Result) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("api_base", result.apiBase)
            .putString("app_token", result.appToken)
            .putString("device_id", result.deviceId)
            .putString("extension", result.extension)
            .putString("sip_host", result.sipHost)
            .apply()
        Log.i("$TAG Saved enroll meta for ext=${result.extension} api=${result.apiBase}")
    }

    fun apiBase(context: Context): String =
        prefs(context).getString("api_base", "").orEmpty()

    fun appToken(context: Context): String =
        prefs(context).getString("app_token", "").orEmpty()

    fun sipHost(context: Context): String =
        prefs(context).getString("sip_host", "").orEmpty()

    fun extension(context: Context): String =
        prefs(context).getString("extension", "").orEmpty()

    fun hasSession(context: Context): Boolean =
        apiBase(context).isNotBlank() && appToken(context).isNotBlank() && sipHost(context).isNotBlank()

    fun currentSession(context: Context): Session? {
        if (!hasSession(context)) return null
        return Session(
            extension = extension(context),
            sipHost = sipHost(context),
            apiBase = apiBase(context),
            appToken = appToken(context),
            deviceId = prefs(context).getString("device_id", "").orEmpty()
        )
    }

    /** Compat casella vocale / multi-identity: match sull'account SIP di default. */
    fun sessionForIdentity(context: Context, username: String?, domain: String?): Session? {
        val session = currentSession(context) ?: return null
        val user = username?.trim().orEmpty()
        val host = domain?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (user.isNotEmpty() && !user.equals(session.extension, ignoreCase = true)) {
            return null
        }
        if (host.isNotEmpty() && host != session.sipHost.lowercase(Locale.ROOT)) {
            return null
        }
        return session
    }

    /** Single-session store: setActive è no-op se già allineato. */
    fun setActive(context: Context, extension: String, sipHost: String) {
        val p = prefs(context)
        val curExt = p.getString("extension", "").orEmpty()
        val curHost = p.getString("sip_host", "").orEmpty()
        if (curExt.equals(extension, ignoreCase = true) &&
            curHost.equals(sipHost, ignoreCase = true)
        ) {
            return
        }
        p.edit()
            .putString("extension", extension.trim())
            .putString("sip_host", sipHost.trim())
            .apply()
        Log.i("$TAG setActive ext=$extension host=$sipHost")
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
