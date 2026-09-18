package org.linphone.alea.voicemail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.linphone.R
import org.linphone.databinding.AleaVoicemailListCellBinding
import org.linphone.ui.GenericListAdapter
import org.linphone.utils.Event

class AleaVoicemailListAdapter :
    GenericListAdapter<AleaVoicemailMessageModel, RecyclerView.ViewHolder>(DiffCallback()) {

    val messageClickedEvent: MutableLiveData<Event<AleaVoicemailMessageModel>> by lazy {
        MutableLiveData()
    }

    val messageLongClickedEvent: MutableLiveData<Event<AleaVoicemailMessageModel>> by lazy {
        MutableLiveData()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: AleaVoicemailListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.alea_voicemail_list_cell,
            parent,
            false
        )
        val holder = ViewHolder(binding)
        binding.root.setOnClickListener {
            binding.model?.let { messageClickedEvent.value = Event(it) }
        }
        binding.root.setOnLongClickListener {
            binding.model?.let {
                messageLongClickedEvent.value = Event(it)
                true
            } ?: false
        }
        binding.playPause.setOnClickListener {
            binding.model?.let { messageClickedEvent.value = Event(it) }
        }
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(getItem(position))
    }

    class ViewHolder(val binding: AleaVoicemailListCellBinding) : RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(model: AleaVoicemailMessageModel) {
            with(binding) {
                this.model = model
                lifecycleOwner = root.findViewTreeLifecycleOwner()
                executePendingBindings()
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AleaVoicemailMessageModel>() {
        override fun areItemsTheSame(
            oldItem: AleaVoicemailMessageModel,
            newItem: AleaVoicemailMessageModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AleaVoicemailMessageModel,
            newItem: AleaVoicemailMessageModel
        ): Boolean = oldItem.id == newItem.id &&
            oldItem.isUnread.value == newItem.isUnread.value &&
            oldItem.transcript == newItem.transcript
    }
}
