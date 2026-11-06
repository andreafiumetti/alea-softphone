package org.linphone.alea.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import org.linphone.R

/**
 * Host for ALEA group chat UI (PBX API, device-local files).
 * Not Linphone Flexisip chat.
 */
class AleaChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alea_chat_activity)
        if (savedInstanceState == null) {
            val groupId = intent.getStringExtra(EXTRA_GROUP_ID)
            if (!groupId.isNullOrBlank()) {
                openThread(groupId, intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty(), addToBack = false)
            } else if (intent.getBooleanExtra(EXTRA_CREATE, false)) {
                supportFragmentManager.commit {
                    replace(R.id.alea_chat_container, AleaChatCreateFragment())
                }
            } else {
                supportFragmentManager.commit {
                    replace(R.id.alea_chat_container, AleaChatGroupsFragment())
                }
            }
        }
    }

    fun openThread(groupId: String, groupName: String, addToBack: Boolean = true) {
        supportFragmentManager.commit {
            replace(R.id.alea_chat_container, AleaChatThreadFragment.newInstance(groupId, groupName))
            if (addToBack) addToBackStack("thread")
        }
    }

    fun openCreate() {
        supportFragmentManager.commit {
            replace(R.id.alea_chat_container, AleaChatCreateFragment())
            addToBackStack("create")
        }
    }

    companion object {
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_GROUP_NAME = "group_name"
        const val EXTRA_CREATE = "create"

        fun openGroups(ctx: Context) {
            ctx.startActivity(Intent(ctx, AleaChatActivity::class.java))
        }

        fun openCreate(ctx: Context) {
            ctx.startActivity(
                Intent(ctx, AleaChatActivity::class.java).putExtra(EXTRA_CREATE, true)
            )
        }

        fun openThread(ctx: Context, groupId: String, name: String) {
            ctx.startActivity(
                Intent(ctx, AleaChatActivity::class.java)
                    .putExtra(EXTRA_GROUP_ID, groupId)
                    .putExtra(EXTRA_GROUP_NAME, name)
            )
        }
    }
}
