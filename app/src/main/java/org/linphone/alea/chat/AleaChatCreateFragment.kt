package org.linphone.alea.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.linphone.R
import org.linphone.alea.AleaDirectoryClient
import org.linphone.alea.AleaEnrollStore
import kotlin.concurrent.thread

class AleaChatCreateFragment : Fragment() {
    private data class Pick(val extension: String, val displayName: String, var selected: Boolean = false)

    private val picks = ArrayList<Pick>()
    private lateinit var adapter: PickAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.alea_chat_create_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) activity?.finish()
        }
        adapter = PickAdapter(picks)
        val list = view.findViewById<RecyclerView>(R.id.members_list)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        loadDirectory()
        view.findViewById<View>(R.id.btn_submit).setOnClickListener { submit(view) }
    }

    private fun loadDirectory() {
        val session = AleaEnrollStore.currentSession(requireContext()) ?: return
        thread(name = "alea-chat-dir", isDaemon = true) {
            try {
                val entries = AleaDirectoryClient.fetch(session.apiBase, session.appToken)
                activity?.runOnUiThread {
                    picks.clear()
                    picks.addAll(entries.map { Pick(it.extension, it.name) })
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun submit(view: View) {
        val name = view.findViewById<EditText>(R.id.input_name).text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.alea_chat_group_name_hint, Toast.LENGTH_SHORT).show()
            return
        }
        val members = picks.filter { it.selected }.map { it.extension }
        if (members.isEmpty()) {
            Toast.makeText(requireContext(), R.string.alea_chat_pick_members, Toast.LENGTH_SHORT).show()
            return
        }
        val session = AleaEnrollStore.currentSession(requireContext()) ?: return
        thread(name = "alea-chat-create", isDaemon = true) {
            try {
                val id = AleaChatClient.createGroup(session.apiBase, session.appToken, name, members)
                activity?.runOnUiThread {
                    (activity as? AleaChatActivity)?.openThread(id, name, addToBack = false)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private class PickAdapter(private val data: List<Pick>) : RecyclerView.Adapter<PickAdapter.VH>() {
        class VH(val box: CheckBox) : RecyclerView.ViewHolder(box)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val box = CheckBox(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(16, 24, 16, 24)
            }
            return VH(box)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = data[position]
            holder.box.text = "${p.displayName} (${p.extension})"
            holder.box.setOnCheckedChangeListener(null)
            holder.box.isChecked = p.selected
            holder.box.setOnCheckedChangeListener { _, checked -> p.selected = checked }
        }
    }
}
