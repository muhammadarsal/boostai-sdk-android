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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import kotlinx.serialization.json.decodeFromJsonElement
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.ChatBackend.Objects.FeedbackValue
import no.boostai.sdk.ChatBackend.Objects.Response.Element
import no.boostai.sdk.ChatBackend.Objects.Response.ElementType
import no.boostai.sdk.ChatBackend.Objects.Response.GenericCard
import no.boostai.sdk.ChatBackend.Objects.Response.Link
import no.boostai.sdk.R

open class ChatMessagePartFragment(
    val element: Element,
    val responseId: String?,
    val isClient: Boolean,
    val isBlocked: Boolean,
    val isWelcomeMessage: Boolean,
    val isLastMessagePart: Boolean,
    val animated: Boolean = true,
    val customConfig: ChatConfig? = null
) : Fragment(
        if (isClient) R.layout.chat_client_message_part else R.layout.chat_server_message_part
    ) {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var fragment: Fragment? = null

        when (element.type) {
            ElementType.text -> fragment =
                element.payload.text?.let { getChatMessageTextFragment(it, false) }
            ElementType.html -> fragment =
                element.payload.html?.let { getChatMessageTextFragment(it, true) }
            ElementType.links -> {
                if (ChatBackend.config?.linkDisplayStyle == "inside") {
                    val stringBuilder = StringBuilder()

                    stringBuilder.append("<ul style=\"padding-left: 10px\">")
                    element.payload.links?.forEach { link ->
                        val url = link.url ?: "boostai://${link.id}"
                        stringBuilder.append("<li><a href=\"$url\">${link.text}</a>")
                    }
                    stringBuilder.append("</ul>")
                    fragment = getChatMessageTextFragment(stringBuilder.toString(), true)
                } else fragment = element.payload.links?.let { getChatMessageButtonsFragment(it) }
            }
            ElementType.image -> fragment =
                element.payload.url?.let { getChatMessageImageFragment(it) }
            ElementType.video -> fragment =
                if (element.payload.source != null && element.payload.url != null)
                    getChatMessageVideoFragment(element.payload.source, element.payload.url)
                else null
            ElementType.json -> {
                element.payload.json?.let {
                    try {
                        val genericCard =
                            ChatBackend.chatbackendJson.decodeFromJsonElement<GenericCard>(it)
                        fragment = getGenericJSONFragment(genericCard)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            ElementType.UNKNOWN -> {} // TODO
        }
        fragment?.let {
            childFragmentManager.beginTransaction().add(R.id.chat_message_part, it).commitAllowingStateLoss()
        }
        // Show feedback buttons when conditions are met
        if (
            responseId != null && !isClient && !isBlocked && !isWelcomeMessage && isLastMessagePart
        ) {
            var activeFeedbackValue: FeedbackValue? = null
            val thumbOnAnimation = AnimationUtils.loadAnimation(context, R.anim.feedback_thumb_on)
            val thumbOffAnimation = AnimationUtils.loadAnimation(context, R.anim.feedback_thumb_off)
            val viewTreeObserver = view.viewTreeObserver

            view.findViewById<View>(R.id.chat_message_feedback_positive)?.let { button: View ->
                val sibling = view.findViewById<View>(R.id.chat_message_feedback_negative)

                button.setOnClickListener {
                    if (activeFeedbackValue == FeedbackValue.positive) {
                        activeFeedbackValue = null
                        ChatBackend.feedback(responseId, FeedbackValue.removePositive)
                        button.setBackgroundResource(R.drawable.ic_thumbs_up)
                        button.startAnimation(thumbOffAnimation)
                        sibling.animation = null
                    }
                    else {
                        if (activeFeedbackValue == FeedbackValue.negative) {
                            ChatBackend.feedback(responseId, FeedbackValue.removeNegative)
                            sibling.setBackgroundResource(R.drawable.ic_thumbs_down)
                            sibling.startAnimation(thumbOffAnimation)
                        }
                        activeFeedbackValue = FeedbackValue.positive
                        ChatBackend.feedback(responseId, FeedbackValue.positive)
                        button.setBackgroundResource(R.drawable.ic_thumbs_up_filled)
                        button.startAnimation(thumbOnAnimation)
                    }
                }
            }
            view.findViewById<View>(R.id.chat_message_feedback_negative)?.let { button: View ->
                val sibling = view.findViewById<View>(R.id.chat_message_feedback_positive)

                button.setOnClickListener {
                    if (activeFeedbackValue == FeedbackValue.negative) {
                        activeFeedbackValue = null
                        ChatBackend.feedback(responseId, FeedbackValue.removeNegative)
                        button.setBackgroundResource(R.drawable.ic_thumbs_down)
                        button.startAnimation(thumbOffAnimation)
                        sibling.animation = null
                    }
                    else {
                        if (activeFeedbackValue == FeedbackValue.positive) {
                            ChatBackend.feedback(responseId, FeedbackValue.removePositive)
                            sibling.setBackgroundResource(R.drawable.ic_thumbs_up)
                            sibling.startAnimation(thumbOffAnimation)
                        }
                        activeFeedbackValue = FeedbackValue.negative
                        ChatBackend.feedback(responseId, FeedbackValue.negative)
                        button.setBackgroundResource(R.drawable.ic_thumbs_down_filled)
                        button.startAnimation(thumbOnAnimation)
                    }
                }
            }
            // Set width of feedback button container to equal rendered width of the message part
            if (viewTreeObserver.isAlive)
                viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {

                        override fun onGlobalLayout() {
                            view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            view.findViewById<LinearLayout>(R.id.chat_message_feedback)
                                .layoutParams =
                                    LinearLayout.LayoutParams(
                                        view.findViewById<FrameLayout>(R.id.chat_message_part)
                                            .width,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                        }

                    }
                )
        } else view.findViewById<LinearLayout>(R.id.chat_message_feedback)?.visibility = View.GONE
    }

     fun getChatMessageTextFragment(text: String, isHtml: Boolean): Fragment =
        ChatMessageTextFragment(text, isHtml, isClient, animated, customConfig)

     fun getChatMessageButtonsFragment(links: List<Link>): Fragment =
        ChatMessageButtonsFragment(links, animated, customConfig)

     fun getChatMessageImageFragment(url: String): Fragment =
        ChatMessageImageFragment(url, animated)

     fun getChatMessageVideoFragment(source: String, url: String): Fragment =
        ChatMessageVideoFragment(source, url)

     fun getGenericJSONFragment(card: GenericCard): Fragment =
        ChatMessageGenericJSONFragment(card, animated)

}