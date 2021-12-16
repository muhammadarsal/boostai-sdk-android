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
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.R
import no.boostai.sdk.UI.Helpers.handleUrlClicks
import no.boostai.sdk.UI.Helpers.trimTrailingWhitespace

open class ChatMessageTextFragment(
    var text: String? = null,
    var isHtml: Boolean = false,
    var isClient: Boolean = false,
    val animated: Boolean = true,
    var customConfig: ChatConfig? = null
) : Fragment(R.layout.chat_message_text_fragment),
    ChatBackend.ConfigObserver {

    val textKey = "text"
    val isHtmlKey = "isHtml"
    val isClientKey = "isClient"
    val customConfigKey = "customConfig"

    lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = savedInstanceState ?: arguments
        bundle?.let {
            text = it.getString(textKey)
            isHtml = it.getBoolean(isHtmlKey)
            isClient = it.getBoolean(isClientKey)
            customConfig = it.getParcelable(customConfigKey)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(textKey, text)
        outState.putBoolean(isHtmlKey, isHtml)
        outState.putBoolean(isClientKey, isClient)
        outState.putParcelable(customConfigKey, customConfig)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textView = view.findViewById(R.id.chat_message_textview)

        if (isClient) {
            view.background = ContextCompat.getDrawable(requireContext(), R.drawable.semi_rounded_alt)

            val layoutParams = view.layoutParams as FrameLayout.LayoutParams
            layoutParams.bottomMargin = 0
            view.layoutParams = layoutParams
        }

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
        /*if (animated)
            view.animation = AnimationUtils.loadAnimation(context, R.anim.chat_message_animate_in)*/

        updateStyling(ChatBackend.config)
        ChatBackend.addConfigObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        ChatBackend.removeConfigObserver(this)
    }

    fun updateStyling(config: ChatConfig?) {
        if (config == null) return

        @ColorRes val backgroundColor: Int
        @ColorRes val textColor: Int

        if (isClient) {
            backgroundColor = customConfig?.clientMessageBackground
                ?: config.clientMessageBackground
                ?: ContextCompat.getColor(requireContext(), R.color.clientMessageBackground)
            textColor = customConfig?.clientMessageColor ?: customConfig?.clientMessageColor
                ?: ContextCompat.getColor(requireContext(), R.color.clientMessageColor)
        } else {
            backgroundColor = customConfig?.serverMessageBackground
                ?: config.serverMessageBackground
                ?: ContextCompat.getColor(requireContext(), R.color.serverMessageBackground)
            textColor = customConfig?.serverMessageColor ?: config.serverMessageColor
                ?: ContextCompat.getColor(requireContext(), R.color.serverMessageColor)
        }

        textView.setTextColor(textColor)
        textView.setLinkTextColor(textColor)
        (view?.background as? GradientDrawable)?.setColor(backgroundColor)
    }

    override fun onConfigReceived(backend: ChatBackend, config: ChatConfig) = updateStyling(config)

    override fun onFailure(backend: ChatBackend, error: Exception) {}

}