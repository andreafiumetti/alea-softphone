package org.linphone.alea

import org.linphone.LinphoneApplication.Companion.corePreferences
import org.linphone.core.Account
import org.linphone.core.Core
import org.linphone.core.Factory
import org.linphone.core.TransportType
import org.linphone.core.tools.Log
import java.util.Locale

/**
 * Crea/aggiorna Account SIP Linphone da credenziali enroll ALEA.
 * Il re-scan QR deve sempre aggiornare password/host (mai lasciare credenziali stale).
 * Transport default = TCP (UDP spesso bloccato su 4G / Wi‑Fi esterne).
 */
object AleaSipAccountInstaller {
    private const val TAG = "[ALEA SIP Install]"

    sealed class Outcome {
        data class Ok(val account: Account) : Outcome()

        data class Error(val message: String) : Outcome()
    }

    /** Preferisci TCP; TLS solo se esplicito; UDP solo se l'enroll lo chiede davvero. */
    fun resolveTransport(raw: String?): TransportType {
        return when (raw?.trim()?.lowercase(Locale.ROOT).orEmpty()) {
            "tls", "sips" -> TransportType.Tls
            "udp" -> TransportType.Udp
            else -> TransportType.Tcp // tcp, blank, unknown
        }
    }

    /**
     * Migra account ALEA già enrollati da UDP → TCP senza riscan QR.
     * Chiamare sul core thread all'avvio / onResume.
     */
    fun ensureTcpForEnrolledAccounts(context: android.content.Context, core: Core) {
        if (!AleaEnrollStore.hasSession(context)) return
        val enrollHost = AleaEnrollStore.sipHost(context).trim().lowercase(Locale.ROOT)
        val enrollExt = AleaEnrollStore.extension(context).trim()
        if (enrollHost.isEmpty()) return
        for (account in core.accountList) {
            val id = account.params.identityAddress ?: continue
            val user = id.username ?: continue
            val domain = (id.domain ?: "").lowercase(Locale.ROOT)
            if (domain != enrollHost) continue
            if (enrollExt.isNotEmpty() && !user.equals(enrollExt, ignoreCase = true)) continue
            val params = account.params.clone() ?: continue
            val server = params.serverAddress?.clone() ?: run {
                Factory.instance().createAddress("sip:$enrollHost") ?: return@run null
            } ?: continue
            if (server.transport == TransportType.Tcp || server.transport == TransportType.Tls) {
                continue
            }
            server.transport = TransportType.Tcp
            params.serverAddress = server
            params.isRegisterEnabled = true
            account.params = params
            account.refreshRegister()
            Log.i("$TAG Migrated ${id.asStringUriOnly()} to TCP (was UDP)")
        }
    }

    fun install(core: Core, enroll: AleaEnrollClient.Result): Outcome {
        core.loadConfigFromXml(corePreferences.thirdPartyDefaultValuesPath)

        val user = enroll.sipUsername.trim()
        val host = enroll.sipHost.trim()
        if (user.isEmpty() || host.isEmpty() || enroll.sipPassword.isEmpty()) {
            return Outcome.Error("Credenziali SIP incomplete dall'enroll")
        }

        val identity = "sip:$user@$host"
        val identityAddress = Factory.instance().createAddress(identity)
            ?: return Outcome.Error("Identity SIP non valida: $identity")
        if (enroll.displayName.isNotBlank()) {
            identityAddress.displayName = enroll.displayName.trim()
        }

        val transport = resolveTransport(enroll.transport)
        val serverUri = if (enroll.sipPort > 0 && enroll.sipPort != 5060) {
            "sip:$host:${enroll.sipPort}"
        } else {
            "sip:$host"
        }
        val server = Factory.instance().createAddress(serverUri)
            ?: return Outcome.Error("Server SIP non valido: $serverUri")
        server.transport = transport

        for (old in core.authInfoList.toList()) {
            if (old.username.equals(user, ignoreCase = true)) {
                core.removeAuthInfo(old)
            }
        }
        val auth = Factory.instance().createAuthInfo(
            user,
            null,
            enroll.sipPassword,
            null,
            null,
            host
        )
        core.addAuthInfo(auth)

        val found = core.accountList.find { account ->
            val id = account.params.identityAddress
            id != null && (
                id.weakEqual(identityAddress) ||
                    (
                        id.username.equals(user, ignoreCase = true) &&
                            id.domain.equals(host, ignoreCase = true)
                    )
                )
        }

        if (found != null) {
            Log.i("$TAG Updating existing account ${identityAddress.asStringUriOnly()} from QR enroll")
            val params = found.params.clone()
            params.identityAddress = identityAddress
            params.serverAddress = server
            params.isRegisterEnabled = true
            found.params = params
            core.defaultAccount = found
            found.refreshRegister()
            return Outcome.Ok(found)
        }

        val params = core.createAccountParams()
        params.identityAddress = identityAddress
        params.serverAddress = server
        params.isRegisterEnabled = true

        val account = core.createAccount(params)
        core.addAccount(account)
        core.defaultAccount = account
        Log.i(
            "$TAG Added account ${identityAddress.asStringUriOnly()} " +
                "server=${server.asStringUriOnly()} transport=${server.transport}"
        )
        return Outcome.Ok(account)
    }
}
