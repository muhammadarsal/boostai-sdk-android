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

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.ChatBackend.Objects.Response.*
import no.boostai.sdk.R
import no.boostai.sdk.UI.Helpers.TimingHelper
import java.util.*
import kotlin.concurrent.schedule

open class ChatMessageFragment(
    val response: Response?,
    val animated: Boolean,
    val isBlocked: Boolean,
    val isClient: Boolean,
    val isWelcomeMessage: Boolean,
    val isWaitingForServerResponse: Boolean = false,
    val isAwaitingFiles: Boolean = false,
    val avatarUrl: String? = null,
    val customConfig: ChatConfig? = null,
    val delegate: ChatViewFragmentDelegate?
) : Fragment(if (isClient) R.layout.chat_client_message else R.layout.chat_server_message) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isClient)
            view.findViewById<ImageView>(R.id.avatar).let {
                it.clipToOutline = true
                if (response?.avatarUrl != null)
                    Glide.with(this).load(response.avatarUrl).into(it)
                if (avatarUrl != null) Glide.with(this).load(avatarUrl).into(it)
            }
        response?.elements?.forEachIndexed { index, element ->
            val pace = ChatBackend.config?.pace ?: "normal"
            val paceFactor = TimingHelper.calculatePace(pace)
            val staggerDelay = TimingHelper.calculateStaggerDelay(pace, 1)
            val timeUntilReveal = if (isClient) 0 else TimingHelper.calcTimeToRead(paceFactor)

            if (animated)
                Timer().schedule(timeUntilReveal * index) {
                    addMessagePart(element, index)
                    // If we have more elements to show, display a waiting indicator before showing it
                    if (!isClient && index < response.elements.size - 1)
                        Timer().schedule(staggerDelay) { addWaitingIndicator() }
                }
            else addMessagePart(element, index)
        }
        if (isAwaitingFiles) {
            val element = Element(
                Payload(
                    links = Arrays.asList(Link("upload", getString(R.string.upload_file), LinkType.action_link))
                ),
                ElementType.links
            )

            if (animated)
                Timer().schedule(
                    TimingHelper.timeUntilReveal() * (response?.elements?.size ?: 0)
                ) { addMessagePart(element, response?.elements?.size ?: 0) }
            else addMessagePart(element, response?.elements?.size ?: 0)
        }
        // If set, render a waiting indicator while waiting for server response
        if (isWaitingForServerResponse)
            childFragmentManager
                .beginTransaction()
                .add(R.id.chat_server_message_parts, getWaitingFragment(), "waitingIndicator")
                .commitAllowingStateLoss()
    }

    fun addMessagePart(element: Element, index: Int) {
        val fragmentTransaction = childFragmentManager.beginTransaction();

        // Remove possible visible waiting indicator
        childFragmentManager.findFragmentByTag("waitingIndicator")?.let {
            fragmentTransaction.remove(it)
        }

        val fragment = delegate?.getChatMessagePartFragment(
            element,
            response?.id,
            animated) ?:
        getMessagePartFragment(element, index)

        // Show the message
        fragmentTransaction.add(
            if (isClient) R.id.chat_client_message_parts else R.id.chat_server_message_parts,
            fragment
        )
        fragmentTransaction.commitAllowingStateLoss()
    }

    fun addWaitingIndicator() =
        childFragmentManager
            .beginTransaction()
            .add(R.id.chat_server_message_parts, getWaitingFragment(), "waitingIndicator")
            .commitAllowingStateLoss()

    fun getMessagePartFragment(element: Element, index: Int): Fragment =
        ChatMessagePartFragment(
            element,
            responseId = response?.id,
            isClient,
            isBlocked,
            isWelcomeMessage,
            index == response?.elements?.size?.minus(if (isAwaitingFiles) 0 else 1) ?: -1,
            animated,
            customConfig
        )

    fun getWaitingFragment(): Fragment = ChatServerWaitingFragment(customConfig)

}