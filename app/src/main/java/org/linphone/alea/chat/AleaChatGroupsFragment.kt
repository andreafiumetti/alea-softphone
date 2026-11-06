package org.linphone.alea.chat

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.linphone.R
import org.linphone.alea.AleaEnrollStore
import kotlin.concurrent.thread

class AleaChatGroupsFragment : Fragment() {
    private val items = ArrayList<AleaChatClient.Group>()
    private lateinit var adapter: GroupsAdapter
    private lateinit var empty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.alea_chat_groups_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        empty = view.findViewById(R.id.empty_label)
        adapter = GroupsAdapter(
            items,
            onClick = { g -> (activity as? AleaChatActivity)?.openThread(g.id, g.name) },
            onDelete = { g -> confirmDelete(g) }
        )
        val list = view.findViewById<RecyclerView>(R.id.groups_list)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        view.findViewById<View>(R.id.btn_back).setOnClickListener { activity?.finish() }
        view.findViewById<View>(R.id.btn_refresh).setOnClickListener { refresh() }
        view.findViewById<View>(R.id.btn_create).setOnClickListener {
            (activity as? AleaChatActivity)?.openCreate()
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val ctx = requireContext().applicationContext
        val session = AleaEnrollStore.currentSession(ctx)
        if (session == null) {
            empty.visibility = View.VISIBLE
            empty.setText(R.string.alea_chat_need_enroll)
            items.clear()
            adapter.notifyDataSetChanged()
            return
        }
        thread(name = "alea-chat-groups", isDaemon = true) {
            try {
                val list = AleaChatClient.listGroups(session.apiBase, session.appToken)
                activity?.runOnUiThread {
                    items.clear()
                    items.addAll(list)
                    adapter.notifyDataSetChanged()
                    empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    empty.setText(R.string.alea_chat_groups_empty)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(ctx, e.message ?: "Errore", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmDelete(group: AleaChatClient.Group) {
        val session = AleaEnrollStore.currentSession(requireContext()) ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.alea_chat_delete_group)
            .setMessage(getString(R.string.alea_chat_delete_group_confirm, group.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.alea_chat_delete_group) { _, _ ->
                thread(name = "alea-chat-del", isDaemon = true) {
                    try {
                        AleaChatClient.deleteOrLeaveGroup(session.apiBase, session.appToken, group.id)
                        activity?.runOnUiThread { refresh() }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), e.message ?: "Errore", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private class GroupsAdapter(
        private val data: List<AleaChatClient.Group>,
        private val onClick: (AleaChatClient.Group) -> Unit,
        private val onDelete: (AleaChatClient.Group) -> Unit
    ) : RecyclerView.Adapter<GroupsAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.group_name)
            val meta: TextView = v.findViewById(R.id.group_meta)
            val delete: View? = v.findViewById(R.id.btn_delete_group)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.alea_chat_group_row, parent, false)
            return VH(v)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val g = data[position]
            holder.name.text = g.name
            holder.meta.text = holder.itemView.context.getString(
                R.string.alea_chat_group_meta,
                g.memberCount
            )
            holder.itemView.setOnClickListener { onClick(g) }
            holder.delete?.setOnClickListener { onDelete(g) }
        }
    }
}
