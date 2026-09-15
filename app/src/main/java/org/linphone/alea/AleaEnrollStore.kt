package org.linphone.alea

import android.content.Context
import org.linphone.core.tools.Log

/** Persistenza minima enroll (push/API/directory) — niente password SIP qui. */
object AleaEnrollStore {
    private const val PREFS = "alea_enroll"
    private const val TAG = "[ALEA Enroll Store]"

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

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
