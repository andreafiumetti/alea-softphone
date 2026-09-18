package org.linphone.alea.voicemail

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import org.linphone.alea.AleaVoicemailClient

/** Riga lista casella vocale (binding). */
class AleaVoicemailMessageModel
    @UiThread
    constructor(
        val raw: AleaVoicemailClient.Message
    ) {
    val id: String = raw.id
    val callerId: String = raw.callerId
    val durationLabel: String = formatDuration(raw.durationSec)
    val dateLabel: String = formatDate(raw.createdAt)
    val transcript: String = raw.transcript.orEmpty()
    val hasTranscript: Boolean = !raw.transcript.isNullOrBlank()
    val isUnread: MutableLiveData<Boolean> = MutableLiveData(!raw.read)
    val isPlaying: MutableLiveData<Boolean> = MutableLiveData(false)

    companion object {
        private fun formatDuration(sec: Int): String {
            if (sec <= 0) return "0:00"
            val m = sec / 60
            val s = sec % 60
            return "%d:%02d".format(m, s)
        }

        private fun formatDate(raw: String): String {
            // "2026-09-15 14:32:01.123+00" o ISO — mostra solo data/ora local-friendly
            val cleaned = raw.replace('T', ' ').take(16)
            return cleaned.ifBlank { raw }
        }
    }
}
