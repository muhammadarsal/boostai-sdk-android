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
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.R
import no.boostai.sdk.UI.Helpers.handleUrlClicks
import no.boostai.sdk.UI.Helpers.trimTrailingWhitespace

open class ChatMessageTextFragment(
    val text: String,
    val isHtml: Boolean,
    val isClient: Boolean,
    val animated: Boolean = true,
    val customConfig: ChatConfig? = null
) : Fragment(
        if (isClient) R.layout.chat_client_message_text_fragment
        else R.layout.chat_server_message_text_fragment
    ),
    ChatBackend.ConfigObserver {

    lateinit var textView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textView = view.findViewById(R.id.chat_message_textview)

        if (isHtml) {
            val content: CharSequence = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)

            textView.text = trimTrailingWhitespace(content)
            textView.handleUrlClicks { url ->
                if (url.startsWith("boostai://")) {
                    val id = url.removePrefix("boostai://")

                    ChatBackend.actionButton(id)
                } else
                    Intent(Intent.ACTION_VIEW).let {
                        it.setData(Uri.parse(url))
                        startActivity(it)
                    }
            }
        } else textView.text = text
        if (animated)
            view.animation = AnimationUtils.loadAnimation(context, R.anim.chat_message_animate_in)
        updateStyling(ChatBackend.config)
        ChatBackend.addConfigObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        ChatBackend.removeConfigObserver(this)
    }

    fun updateStyling(config: ChatConfig?) {
        if (config == null) return

        val backgroundColor: Int
        val textColor: Int

        if (isClient) {
            backgroundColor = Color.parseColor(
                customConfig?.clientMessageBackground ?: config.clientMessageBackground
            )
            textColor = Color.parseColor(
                customConfig?.clientMessageColor ?: config.clientMessageColor
            )
        } else {
            backgroundColor = Color.parseColor(
                customConfig?.serverMessageBackground ?: config.serverMessageBackground
            )
            textColor = Color.parseColor(
                customConfig?.serverMessageColor ?: config.serverMessageColor
            )
        }

        textView.setTextColor(textColor)
        textView.setLinkTextColor(textColor)
        (view?.background as? GradientDrawable)?.setColor(backgroundColor)
    }

    override fun onConfigReceived(backend: ChatBackend, config: ChatConfig) = updateStyling(config)

    override fun onFailure(backend: ChatBackend, error: Exception) {}

}