package org.linphone.alea

import android.net.Uri
import java.util.Locale

/**
 * Parser link enroll ALEA — stessi formati del softphone Flutter:
 * - https://host[:port]/join/{token}
 * - alea://join/{token}?server=https://host
 */
object AleaJoinLink {
    data class Parsed(val apiBase: String, val token: String)

    fun parse(raw: String?): Parsed? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        return try {
            parseUri(Uri.parse(text)) ?: parseLoose(text)
        } catch (_: Exception) {
            parseLoose(text)
        }
    }

    fun parseUri(uri: Uri?): Parsed? {
        if (uri == null) return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null

        if (scheme == "alea" && uri.host.equals("join", ignoreCase = true)) {
            val token = uri.pathSegments.firstOrNull()?.trim().orEmpty()
            if (token.isEmpty()) return null
            val server = uri.getQueryParameter("server")?.trim().orEmpty()
            if (server.isEmpty()) return null
            return Parsed(apiBase = server.trimEnd('/'), token = token)
        }

        if (scheme == "http" || scheme == "https") {
            val segs = uri.pathSegments
            if (segs.size >= 2 && segs[0].equals("join", ignoreCase = true)) {
                val token = segs[1].trim()
                if (token.isEmpty()) return null
                val port = if (uri.port != -1) ":${uri.port}" else ""
                val apiBase = "${uri.scheme}://${uri.host}$port"
                return Parsed(apiBase = apiBase, token = token)
            }
        }
        return null
    }

    private fun parseLoose(text: String): Parsed? {
        // Fallback se Uri non parse-a bene path multipli
        val m = Regex("""^(https?://[^/\s]+)/join/([A-Za-z0-9._~-]+)$""", RegexOption.IGNORE_CASE)
            .find(text)
        if (m != null) {
            return Parsed(apiBase = m.groupValues[1].trimEnd('/'), token = m.groupValues[2])
        }
        return null
    }
}
