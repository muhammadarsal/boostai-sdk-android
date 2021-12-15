//  boost.ai Android SDK
//  Copyright © 2021 boost.ai
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <https://www.gnu.org/licenses/>.
//
//  Please contact us at contact@boost.ai if you have any questions.
//

package no.boostai.sdk.UI

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.ChatBackend.Objects.ChatConfigDefaults
import no.boostai.sdk.ChatBackend.Objects.Response.APIMessage
import no.boostai.sdk.R

open class ChatViewSettingsFragment(
    var menuDelegate: ChatViewSettingsDelegate? = null,
    var isDialog: Boolean = false,
    var customConfig: ChatConfig? = null
) : Fragment(R.layout.chat_view_settings),
    ChatBackend.ConfigObserver {

    lateinit var feedbackButton: TextView
    lateinit var downloadButton: TextView
    lateinit var deleteButton: TextView
    lateinit var privacyPolicyButton: LinearLayout
    lateinit var privacyPolicyTextView: TextView
    lateinit var privacyPolicyImageView: ImageView
    lateinit var backButton: TextView
    lateinit var poweredByTextView: TextView
    lateinit var poweredByImageView: ImageView

    val isDialogKey = "isDialog"
    val customConfigKey = "customConfig"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = savedInstanceState ?: arguments
        bundle?.let {
            isDialog = it.getBoolean(isDialogKey)
            customConfig = it.getParcelable(customConfigKey)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(isDialogKey, isDialog)
        outState.putParcelable(customConfigKey, customConfig)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedbackButton = view.findViewById(R.id.give_feedback)
        downloadButton = view.findViewById(R.id.download_conversation)
        deleteButton = view.findViewById(R.id.delete_conversation)
        privacyPolicyButton = view.findViewById(R.id.privacy_policy)
        privacyPolicyTextView = view.findViewById(R.id.privacy_policy_textview)
        privacyPolicyImageView = view.findViewById(R.id.privacy_policy_imageview)
        backButton = view.findViewById(R.id.back)
        poweredByTextView = view.findViewById(R.id.powered_by_textview)
        poweredByImageView = view.findViewById(R.id.powered_by_imageview)

        val fadeAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_fast)
        view.animation = fadeAnimation

        val requestConversationFeedback = ChatBackend.config?.requestConversationFeedback ?: ChatConfigDefaults.requestConversationFeedback
        feedbackButton.visibility =
            if (requestConversationFeedback && !isDialog)
                View.VISIBLE else View.GONE
        deleteButton.visibility =
            if (ChatBackend.allowDeleteConversation) View.VISIBLE else View.GONE
        privacyPolicyButton.visibility =
            if (ChatBackend.privacyPolicyUrl.isNotEmpty()) View.VISIBLE else View.GONE
        feedbackButton.setOnClickListener { menuDelegate?.showFeedback() }
        downloadButton.setOnClickListener { downloadConversation() }
        deleteButton.setOnClickListener { deleteConversation() }
        privacyPolicyButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(ChatBackend.privacyPolicyUrl)
            startActivity(intent)
        }
        poweredByImageView.setOnClickListener {
            Intent(Intent.ACTION_VIEW).let {
                it.setData(Uri.parse("https://www.boost.ai/"))
                startActivity(it)
            }
        }
        backButton.setOnClickListener { menuDelegate?.hideMenu() }
        view.setOnClickListener {
            // Prevent clicks on the view to fall through to the background view
        }
        updateStyling(ChatBackend.config)
        ChatBackend.addConfigObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        ChatBackend.removeConfigObserver(this)
    }

    fun updateStyling(config: ChatConfig?) {
        if (config == null) return

        val primaryColor = Color.parseColor(
            customConfig?.primaryColor ?: config.primaryColor
        )
        val contrastColor = Color.parseColor(
            customConfig?.contrastColor ?: config.contrastColor
        )

        view?.background = ColorDrawable(primaryColor)
        feedbackButton.setTextColor(contrastColor)
        downloadButton.setTextColor(contrastColor)
        deleteButton.setTextColor(contrastColor)
        privacyPolicyTextView.setTextColor(contrastColor)
        privacyPolicyImageView.imageTintList = ColorStateList.valueOf(contrastColor)
        backButton.setTextColor(primaryColor)
        (backButton.background as? GradientDrawable)?.setColor(contrastColor)
        poweredByTextView.setTextColor(contrastColor)
        poweredByImageView.imageTintList = ColorStateList.valueOf(contrastColor)
    }

    fun downloadConversation() {
        ChatBackend.download(null, object : ChatBackend.APIMessageResponseListener {

            override fun onFailure(exception: Exception) {}

            override fun onResponse(apiMessage: APIMessage) {
                if (apiMessage.download != null) {
                    val intent = Intent(Intent.ACTION_SEND)

                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.conversation))
                    intent.putExtra(Intent.EXTRA_TEXT, apiMessage.download)
                    startActivity(Intent.createChooser(intent, getString(R.string.download_conversation)))
                }
            }

        })
    }

    fun deleteConversation() = menuDelegate?.deleteConversation()

    fun hide() {
        val fadeAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out_fast)

        view?.startAnimation(fadeAnimation)
    }

    override fun onConfigReceived(backend: ChatBackend, config: ChatConfig) = updateStyling(config)

    override fun onFailure(backend: ChatBackend, error: Exception) {}
}