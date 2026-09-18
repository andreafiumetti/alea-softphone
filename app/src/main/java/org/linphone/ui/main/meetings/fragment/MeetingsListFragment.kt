/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 * ALEA: tab Casella = casella vocale visuale (lista messaggi, non *97).
 */
package org.linphone.ui.main.meetings.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.linphone.R
import org.linphone.alea.voicemail.AleaVoicemailListAdapter
import org.linphone.alea.voicemail.AleaVoicemailListViewModel
import org.linphone.core.tools.Log
import org.linphone.databinding.AleaVoicemailListFragmentBinding
import org.linphone.ui.GenericActivity
import org.linphone.ui.main.fragment.AbstractMainFragment

@UiThread
class MeetingsListFragment : AbstractMainFragment() {
    companion object {
        private const val TAG = "[ALEA Voicemail List Fragment]"
    }

    private lateinit var binding: AleaVoicemailListFragmentBinding
    private lateinit var listViewModel: AleaVoicemailListViewModel
    private lateinit var adapter: AleaVoicemailListAdapter

    override fun onDefaultAccountChanged() {
        Log.i("$TAG Default account changed, refreshing voicemail")
        listViewModel.refresh()
    }

    override fun onSlidingPaneClosed() {
        adapter.resetSelection()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = AleaVoicemailListAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AleaVoicemailListFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listViewModel = ViewModelProvider(this)[AleaVoicemailListViewModel::class.java]
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = listViewModel

        binding.voicemailList.setHasFixedSize(true)
        binding.voicemailList.layoutManager = LinearLayoutManager(requireContext())
        binding.voicemailList.outlineProvider = outlineProvider
        binding.voicemailList.clipToOutline = true
        binding.voicemailList.adapter = adapter

        binding.setRefreshClicked {
            listViewModel.refresh()
        }

        adapter.messageClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                listViewModel.playOrPause(model)
            }
        }

        adapter.messageLongClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                showDeleteDialog {
                    listViewModel.delete(model)
                }
            }
        }

        listViewModel.messages.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            Log.i("$TAG Voicemail list ready with [${it.size}] items")
        }

        listViewModel.errorEvent.observe(viewLifecycleOwner) {
            it.consume { msg ->
                (requireActivity() as GenericActivity).showRedToast(msg, R.drawable.warning_circle)
            }
        }

        listViewModel.title.value = getString(R.string.bottom_navigation_meetings_label)
        setViewModel(listViewModel)
        initViews(
            binding.slidingPaneLayout,
            binding.topBar,
            binding.bottomNavBar,
            R.id.meetingsListFragment
        )
    }

    override fun onResume() {
        super.onResume()
        listViewModel.refresh()
    }

    override fun onPause() {
        listViewModel.stopPlayback()
        super.onPause()
    }

    private fun showDeleteDialog(onConfirm: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage(R.string.alea_voicemail_delete_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
