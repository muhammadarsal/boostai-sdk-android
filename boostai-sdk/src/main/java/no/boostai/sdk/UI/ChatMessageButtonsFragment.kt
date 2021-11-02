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
import androidx.fragment.app.Fragment
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.ChatBackend.Objects.Response.FunctionType
import no.boostai.sdk.ChatBackend.Objects.Response.Link
import no.boostai.sdk.R

open class ChatMessageButtonsFragment(
    val links: List<Link>,
    val animated: Boolean = true,
    val customConfig: ChatConfig? = null
) : Fragment(R.layout.chat_server_message_buttons) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val function = links.first().function

        if (function == FunctionType.APPROVE || function == FunctionType.DENY)
            childFragmentManager.beginTransaction()
                .add(R.id.chat_message_buttons, getChatMessageConsentFragment(links))
                .commitAllowingStateLoss()
        else links.forEachIndexed { index, link ->
            childFragmentManager.beginTransaction()
                .add(R.id.chat_message_buttons, getChatMessageButtonFragment(link, index))
                .commitAllowingStateLoss()
        }
    }

    fun getChatMessageConsentFragment(links: List<Link>): Fragment =
        ChatMessageConsentFragment(links, animated)

    fun getChatMessageButtonFragment(link: Link, index: Int): Fragment =
        ChatMessageButtonFragment(link, index, animated, customConfig)

}