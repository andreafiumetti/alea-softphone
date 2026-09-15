package org.linphone.alea

import android.content.Context
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.LinphoneApplication.Companion.corePreferences
import org.linphone.core.Friend
import org.linphone.core.FriendList
import org.linphone.core.tools.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Sincronizza interni ALEA in FriendList locale → Contatti / MagicSearch.
 * Applica anche presenza online (pallino verde) da API directory.
 */
object AleaDirectorySync {
    private const val TAG = "[ALEA Directory Sync]"
    const val FRIEND_LIST_NAME = "ALEA directory"
    private val running = AtomicBoolean(false)

    fun syncIfNeeded(context: Context) {
        if (!AleaEnrollStore.hasSession(context)) {
            Log.i("$TAG Skip: no enroll session")
            return
        }
        if (!running.compareAndSet(false, true)) {
            Log.i("$TAG Already running")
            return
        }
        val appCtx = context.applicationContext
        val apiBase = AleaEnrollStore.apiBase(appCtx)
        val token = AleaEnrollStore.appToken(appCtx)
        val sipHost = AleaEnrollStore.sipHost(appCtx)
        thread(name = "alea-directory-sync", isDaemon = true) {
            try {
                val entries = AleaDirectoryClient.fetch(apiBase, token)
                if (!coreContext.isReady()) {
                    Log.w("$TAG Core not ready, abort apply")
                    return@thread
                }
                coreContext.postOnCoreThread {
                    if (corePreferences.contactsFilter == "sip.linphone.org") {
                        Log.w("$TAG Clearing legacy contacts_filter=sip.linphone.org")
                        corePreferences.contactsFilter = ""
                    }
                    applyToCore(entries, sipHost)
                }
            } catch (e: Exception) {
                Log.e("$TAG Sync failed: $e")
            } finally {
                running.set(false)
            }
        }
    }

    private fun applyToCore(entries: List<AleaDirectoryClient.Entry>, sipHost: String) {
        val core = coreContext.core
        val list = core.getFriendListByName(FRIEND_LIST_NAME) ?: core.createFriendList().also {
            it.displayName = FRIEND_LIST_NAME
            it.isDatabaseStorageEnabled = true
            it.type = FriendList.Type.Default
            core.addFriendList(it)
            Log.i("$TAG Created friend list [$FRIEND_LIST_NAME]")
        }

        val friends = ArrayList<Friend>(entries.size)
        for (entry in entries) {
            val uri = "sip:${entry.extension}@$sipHost"
            val address = core.interpretUrl(uri, false) ?: continue
            val friend = core.createFriend()
            friend.name = entry.name
            friend.refKey = "alea:${entry.extension}"
            friend.address = address
            applyPresence(friend, entry.online)
            friends.add(friend)
        }
        val arr = friends.toTypedArray()
        val changed = list.synchronizeFriendsWith(arr)
        for (entry in entries) {
            val key = "alea:${entry.extension}"
            val existing = list.friends.firstOrNull { it.refKey == key } ?: continue
            applyPresence(existing, entry.online)
        }
        Log.i("$TAG Applied ${friends.size} friends (changed=$changed, online=${entries.count { it.online }})")
        coreContext.contactsManager.onNativeContactsLoaded()
    }

    private fun applyPresence(friend: Friend, online: Boolean) {
        try {
            val core = coreContext.core
            val model = core.createPresenceModel()
            model.basicStatus = if (online) {
                org.linphone.core.PresenceBasicStatus.Open
            } else {
                org.linphone.core.PresenceBasicStatus.Closed
            }
            friend.edit()
            friend.presenceModel = model
            friend.done()
        } catch (e: Exception) {
            Log.w("$TAG Presence apply failed for [${friend.name}]: $e")
        }
    }
}
