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

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.ChatBackend.Objects.ChatPanelDefaults
import no.boostai.sdk.ChatBackend.Objects.Response.FunctionType
import no.boostai.sdk.ChatBackend.Objects.Response.Link
import no.boostai.sdk.R
import no.boostai.sdk.UI.Events.BoostUIEvents
import no.boostai.sdk.UI.Helpers.TimingHelper
import java.util.*
import kotlin.concurrent.schedule

open class ChatMessageConsentFragment(
    var links: ArrayList<Link>? = null,
    val animated: Boolean = true,
    val customConfig: ChatConfig? = null,
) : Fragment(R.layout.chat_server_message_consent) {

    lateinit var approveButton: Button
    lateinit var denyButton: Button

    val linksKey = "links"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = savedInstanceState ?: arguments
        bundle?.let {
            links = it.getParcelableArrayList(linksKey)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelableArrayList(linksKey, links)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        approveButton = view.findViewById(R.id.approve_button)
        denyButton = view.findViewById(R.id.deny_button)

        if (animated) {
            val pace = customConfig?.chatPanel?.styling?.pace
                ?: ChatBackend.config?.chatPanel?.styling?.pace
                ?: ChatPanelDefaults.Styling.pace
            val staggerDelay = TimingHelper.calculateStaggerDelay(pace = pace, idx = 0)

            Timer().schedule(staggerDelay) {
                view.alpha = 1.0F
                val fadeInAnimation =
                    AnimationUtils.loadAnimation(context, R.anim.chat_message_animate_in)
                view.animation = fadeInAnimation
            }
        } else view.alpha = 1.0F
        approveButton.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.consentApproveButtonTextColor)
        )
        (approveButton.background as? GradientDrawable)?.setColor(
            ContextCompat.getColor(requireContext(), R.color.consentApproveButtonBackgroundColor)
        )
        denyButton.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.consentDenyButtonTextColor)
        )
        (denyButton.background as? GradientDrawable)?.setColor(
            ContextCompat.getColor(requireContext(), R.color.consentDenyButtonBackgroundColor)
        )
        links?.let { links ->
            links.find { it.function == FunctionType.APPROVE }?.let { link ->
                approveButton.text = link.text
                approveButton.setOnClickListener {
                    ChatBackend.actionButton(link.id)
                    BoostUIEvents.notifyObservers(BoostUIEvents.Event.actionLinkClicked, link.id)
                    approveButton.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.consentApproveButtonTextColor
                        )
                    )
                    (approveButton.background as? GradientDrawable)?.alpha = 255
                    denyButton.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.consentDenyButtonDisabledTextColor
                        )
                    )
                    (denyButton.background as? GradientDrawable)?.alpha = 64
                }
            }
            links.find { it.function == FunctionType.DENY }?.let { link ->
                denyButton.text = link.text
                denyButton.setOnClickListener {
                    ChatBackend.actionButton(link.id)
                    BoostUIEvents.notifyObservers(BoostUIEvents.Event.actionLinkClicked, link.id)
                    denyButton.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.consentDenyButtonTextColor)
                    )
                    (denyButton.background as? GradientDrawable)?.alpha = 255
                    approveButton.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.consentApproveButtonDisabledTextColor
                        )
                    )
                    (approveButton.background as? GradientDrawable)?.alpha = 64
                }
            }
        }
    }

}