package org.linphone.alea.voicemail

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import kotlin.concurrent.thread
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.alea.AleaEnrollStore
import org.linphone.alea.AleaVoicemailClient
import org.linphone.core.tools.Log
import org.linphone.ui.main.viewmodel.AbstractMainViewModel
import org.linphone.utils.AppUtils
import org.linphone.utils.Event

class AleaVoicemailListViewModel
    @UiThread
    constructor() : AbstractMainViewModel() {
    companion object {
        private const val TAG = "[ALEA Voicemail List]"
    }

    val messages = MutableLiveData<ArrayList<AleaVoicemailMessageModel>>()
    val fetchInProgress = MutableLiveData<Boolean>()
    val emptyInbox = MutableLiveData<Boolean>()
    val noSession = MutableLiveData<Boolean>()
    val unreadCount = MutableLiveData<Int>()
    val playingId = MutableLiveData<String?>()

    val refreshDoneEvent: MutableLiveData<Event<Boolean>> by lazy { MutableLiveData() }
    val errorEvent: MutableLiveData<Event<String>> by lazy { MutableLiveData() }

    private var player: MediaPlayer? = null
    private var allMessages = ArrayList<AleaVoicemailMessageModel>()

    init {
        title.value = AppUtils.getString(R.string.bottom_navigation_meetings_label)
        fetchInProgress.value = true
        emptyInbox.value = false
        noSession.value = false
        unreadCount.value = 0
        refresh()
    }

    @UiThread
    fun refresh() {
        val ctx = coreContext.context
        val identity = coreContext.core.defaultAccount?.params?.identityAddress
        val session = AleaEnrollStore.sessionForIdentity(ctx, identity?.username, identity?.domain)
        if (session == null) {
            noSession.value = true
            emptyInbox.value = false
            fetchInProgress.value = false
            messages.value = arrayListOf()
            return
        }
        AleaEnrollStore.setActive(ctx, session.extension, session.sipHost)
        noSession.value = false
        fetchInProgress.value = true
        val apiBase = session.apiBase
        val token = session.appToken
        thread(name = "alea-voicemail-list", isDaemon = true) {
            try {
                val inbox = AleaVoicemailClient.list(apiBase, token)
                coreContext.postOnMainThread {
                    allMessages = ArrayList(inbox.items.map { AleaVoicemailMessageModel(it) })
                    unreadCount.value = inbox.unread
                    applyFilterInternal()
                    emptyInbox.value = allMessages.isEmpty()
                    fetchInProgress.value = false
                    refreshDoneEvent.value = Event(true)
                }
            } catch (e: Exception) {
                Log.e("$TAG Refresh failed: $e")
                coreContext.postOnMainThread {
                    fetchInProgress.value = false
                    errorEvent.value = Event(e.message ?: "Errore casella vocale")
                }
            }
        }
    }

    @UiThread
    override fun filter() {
        applyFilterInternal()
    }

    @UiThread
    private fun applyFilterInternal() {
        val filterText = searchFilter.value.orEmpty().trim().lowercase()
        val list = if (filterText.isEmpty()) {
            ArrayList(allMessages)
        } else {
            ArrayList(
                allMessages.filter {
                    it.callerId.lowercase().contains(filterText) ||
                        it.transcript.lowercase().contains(filterText)
                }
            )
        }
        isFilterEmpty.value = filterText.isEmpty()
        messages.value = list
        emptyInbox.value = allMessages.isEmpty()
    }

    @UiThread
    fun playOrPause(model: AleaVoicemailMessageModel) {
        val ctx = coreContext.context
        val session = resolveSession(ctx) ?: return

        if (playingId.value == model.id && player?.isPlaying == true) {
            player?.pause()
            model.isPlaying.value = false
            return
        }

        stopPlayback()
        playingId.value = model.id
        model.isPlaying.value = true

        val apiBase = session.apiBase
        val token = session.appToken
        val url = AleaVoicemailClient.audioUrl(apiBase, model.id)

        thread(name = "alea-voicemail-play", isDaemon = true) {
            try {
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                mp.setDataSource(
                    ctx,
                    Uri.parse(url),
                    mapOf("Authorization" to "Bearer $token")
                )
                mp.setOnCompletionListener {
                    coreContext.postOnMainThread {
                        model.isPlaying.value = false
                        playingId.value = null
                        releasePlayer()
                    }
                }
                mp.setOnErrorListener { _, what, extra ->
                    Log.e("$TAG MediaPlayer error what=$what extra=$extra")
                    coreContext.postOnMainThread {
                        model.isPlaying.value = false
                        playingId.value = null
                        releasePlayer()
                    }
                    true
                }
                mp.prepare()
                mp.start()
                coreContext.postOnMainThread {
                    player = mp
                    if (model.isUnread.value == true) {
                        markRead(model)
                    }
                }
            } catch (e: Exception) {
                Log.e("$TAG Play failed: $e")
                coreContext.postOnMainThread {
                    model.isPlaying.value = false
                    playingId.value = null
                    errorEvent.value = Event(e.message ?: "Riproduzione fallita")
                }
            }
        }
    }

    @UiThread
    fun markRead(model: AleaVoicemailMessageModel) {
        val ctx = coreContext.context
        val session = resolveSession(ctx) ?: return
        thread(name = "alea-voicemail-read", isDaemon = true) {
            try {
                AleaVoicemailClient.markRead(session.apiBase, session.appToken, model.id)
                coreContext.postOnMainThread {
                    model.isUnread.value = false
                    unreadCount.value = allMessages.count { it.isUnread.value == true }
                }
            } catch (e: Exception) {
                Log.w("$TAG markRead failed: $e")
            }
        }
    }

    @UiThread
    fun delete(model: AleaVoicemailMessageModel) {
        val ctx = coreContext.context
        if (playingId.value == model.id) stopPlayback()
        val session = resolveSession(ctx) ?: return
        thread(name = "alea-voicemail-delete", isDaemon = true) {
            try {
                AleaVoicemailClient.delete(session.apiBase, session.appToken, model.id)
                coreContext.postOnMainThread {
                    allMessages.removeAll { it.id == model.id }
                    applyFilterInternal()
                    emptyInbox.value = allMessages.isEmpty()
                    unreadCount.value = allMessages.count { it.isUnread.value == true }
                }
            } catch (e: Exception) {
                Log.e("$TAG delete failed: $e")
                coreContext.postOnMainThread {
                    errorEvent.value = Event(e.message ?: "Eliminazione fallita")
                }
            }
        }
    }

    private fun resolveSession(ctx: android.content.Context): AleaEnrollStore.Session? {
        val identity = coreContext.core.defaultAccount?.params?.identityAddress
        val session = AleaEnrollStore.sessionForIdentity(ctx, identity?.username, identity?.domain)
        if (session != null) {
            AleaEnrollStore.setActive(ctx, session.extension, session.sipHost)
        }
        return session
    }

    @UiThread
    fun stopPlayback() {
        allMessages.forEach { it.isPlaying.value = false }
        playingId.value = null
        releasePlayer()
    }

    private fun releasePlayer() {
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }
}
