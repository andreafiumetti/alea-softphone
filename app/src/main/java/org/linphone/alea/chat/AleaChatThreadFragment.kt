package org.linphone.alea.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.linphone.R
import org.linphone.alea.AleaEnrollStore
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

class AleaChatThreadFragment : Fragment() {
    private var groupId: String = ""
    private var groupName: String = ""
    private val messages = ArrayList<AleaChatClient.Message>()
    private lateinit var adapter: MsgAdapter
    private lateinit var list: RecyclerView

    private val pickFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        sendFile(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = arguments?.getString(ARG_ID).orEmpty()
        groupName = arguments?.getString(ARG_NAME).orEmpty()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.alea_chat_thread_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.thread_title).text = groupName
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) activity?.finish()
        }
        list = view.findViewById(R.id.messages_list)
        adapter = MsgAdapter(messages) { att -> downloadAttachment(att) }
        list.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        list.adapter = adapter

        val input = view.findViewById<EditText>(R.id.input_message)
        view.findViewById<View>(R.id.btn_send).setOnClickListener {
            val text = input.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@setOnClickListener
            input.setText("")
            sendText(text)
        }
        view.findViewById<View>(R.id.btn_attach).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            pickFile.launch(Intent.createChooser(intent, getString(R.string.alea_chat_attach)))
        }
        refresh()
    }

    private fun sessionOrToast(): AleaEnrollStore.Session? {
        val s = AleaEnrollStore.currentSession(requireContext())
        if (s == null) {
            Toast.makeText(requireContext(), R.string.alea_chat_need_enroll, Toast.LENGTH_SHORT).show()
        }
        return s
    }

    private fun refresh() {
        val session = sessionOrToast() ?: return
        thread(name = "alea-chat-msgs", isDaemon = true) {
            try {
                val msgs = AleaChatClient.listMessages(session.apiBase, session.appToken, groupId)
                activity?.runOnUiThread {
                    messages.clear()
                    messages.addAll(msgs)
                    adapter.notifyDataSetChanged()
                    if (messages.isNotEmpty()) list.scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendText(text: String) {
        val session = sessionOrToast() ?: return
        thread(name = "alea-chat-send", isDaemon = true) {
            try {
                AleaChatClient.postText(session.apiBase, session.appToken, groupId, text)
                activity?.runOnUiThread { refresh() }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendFile(uri: Uri) {
        val session = sessionOrToast() ?: return
        val ctx = requireContext()
        thread(name = "alea-chat-file", isDaemon = true) {
            try {
                val name = queryName(uri) ?: "file"
                val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
                val tmp = File(ctx.cacheDir, "upload_$name")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmp).use { output -> input.copyTo(output) }
                } ?: throw AleaChatClient.ChatException("Impossibile leggere il file")
                AleaChatClient.postFile(session.apiBase, session.appToken, groupId, null, tmp, mime)
                tmp.delete()
                activity?.runOnUiThread { refresh() }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(ctx, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadAttachment(att: AleaChatClient.Attachment) {
        val session = sessionOrToast() ?: return
        val ctx = requireContext()
        val local = AleaChatLocalStore.localPath(ctx, att.id)
        if (local != null && File(local).exists()) {
            openLocal(File(local), att.mime)
            return
        }
        if (!att.available) {
            Toast.makeText(ctx, R.string.alea_chat_attachment_expired, Toast.LENGTH_SHORT).show()
            return
        }
        thread(name = "alea-chat-dl", isDaemon = true) {
            try {
                val dest = AleaChatLocalStore.attachmentFile(ctx, att.id, att.filename)
                AleaChatClient.downloadAttachment(session.apiBase, session.appToken, att.id, dest)
                AleaChatLocalStore.markDownloaded(ctx, att.id, dest.absolutePath)
                activity?.runOnUiThread { openLocal(dest, att.mime) }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(ctx, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openLocal(file: File, mime: String) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.alea_chat_open_file)))
        } catch (_: Exception) {
            Toast.makeText(requireContext(), file.absolutePath, Toast.LENGTH_LONG).show()
        }
    }

    private fun queryName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && it.moveToFirst()) return it.getString(idx)
        }
        return null
    }

    private class MsgAdapter(
        private val data: List<AleaChatClient.Message>,
        private val onAttachment: (AleaChatClient.Attachment) -> Unit
    ) : RecyclerView.Adapter<MsgAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val sender: TextView = v.findViewById(R.id.sender)
            val body: TextView = v.findViewById(R.id.body)
            val attachment: TextView = v.findViewById(R.id.attachment)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.alea_chat_message_cell, parent, false)
            return VH(v)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = data[position]
            holder.sender.text = "${m.senderDisplayName} (${m.senderExtension})"
            if (m.body.isNullOrBlank()) {
                holder.body.visibility = View.GONE
            } else {
                holder.body.visibility = View.VISIBLE
                holder.body.text = m.body
            }
            val att = m.attachment
            if (att == null) {
                holder.attachment.visibility = View.GONE
            } else {
                holder.attachment.visibility = View.VISIBLE
                holder.attachment.text = "📎 ${att.filename}"
                holder.attachment.setOnClickListener { onAttachment(att) }
            }
        }
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_NAME = "name"

        fun newInstance(groupId: String, name: String) = AleaChatThreadFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ID, groupId)
                putString(ARG_NAME, name)
            }
        }
    }
}
