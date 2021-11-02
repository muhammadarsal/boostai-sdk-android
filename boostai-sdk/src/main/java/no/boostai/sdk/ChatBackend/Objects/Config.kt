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

package no.boostai.sdk.ChatBackend.Objects
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ConfigMessages (
    val back: String = "Back",

    @SerialName("close.window")
    val closeWindow: String = "Close",

    @SerialName("compose.characters.used")
    val composeCharactersUsed: String = "{0} out of {1} characters used",

    @SerialName("compose.placeholder")
    val composePlaceholder: String = "Type in here",

    @SerialName("delete.conversation")
    val deleteConversation: String = "Delete conversation",

    @SerialName("download.conversation")
    val downloadConversation: String = "Download conversation",

    @SerialName("feedback.placeholder")
    val feedbackPlaceholder: String = "Write in your feedback here",

    @SerialName("feedback.prompt")
    val feedbackPrompt: String = "Do you want to give me feedback?",

    @SerialName("feedback.thumbs.down")
    val feedbackThumbsDown: String = "Not satisfied with conversation",

    @SerialName("feedback.thumbs.up")
    val feedbackThumbsUp: String = "Satisfied with conversation",

    @SerialName("filter.select")
    val filterSelect: String = "Select user group",

    @SerialName("header.text")
    val headerText: String = "Conversational AI",

    @SerialName("logged.in")
    val loggedIn: String = "Secure chat",

    @SerialName("message.thumbs.down")
    val messageThumbsDown: String = "Not satisfied with answer",

    @SerialName("message.thumbs.up")
    val messageThumbsUp: String = "Satisfied with answer",

    @SerialName("minimize.window")
    val minimizeWindow: String = "Minimize window",

    @SerialName("open.menu")
    val openMenu: String = "Open menu",

    @SerialName("opens.in.new.tab")
    val opensInNewTab: String = "Opens in new tab",

    @SerialName("privacy.policy")
    val privacyPolicy: String = "Privacy policy",

    @SerialName("submit.feedback")
    val submitFeedback: String = "Send",

    @SerialName("submit.message")
    val submitMessage: String = "Send",

    @SerialName("text.too.long")
    val textTooLong: String = "The message cannot be longer than {0} characters",

    @SerialName("upload.file")
    val uploadFile: String = "Upload image",

    @SerialName("upload.file.error")
    val uploadFileError: String = "Upload failed",

    @SerialName("upload.file.progress")
    val uploadFileProgress: String = "Uploading ...",

    @SerialName("upload.file.success")
    val uploadFileSuccess: String = "Upload successful"
) : Parcelable

@Serializable
@Parcelize
data class ConfigFilter (
    val id: Int,
    val title: String,
    val values: List<String>
) : Parcelable

@Serializable
@Parcelize
data class ChatConfig (
    var avatarStyle: String = "square",
    var clientMessageBackground: String = "#ede5ed",
    var clientMessageColor: String = "#363636",
    var contrastColor: String = "#ffffff",
    var fileUploadServiceEndpointUrl: String = "",
    var filters: List<ConfigFilter>? = null,
    var hasFilterSelector: Boolean = false,
    var linkBelowBackground: String = "#552a55",
    var linkBelowColor: String = "#ffffff",
    var linkDisplayStyle: String = "below",
    var primaryColor: String = "#552a55",
    var requestConversationFeedback: Boolean = true,
    var serverMessageBackground: String = "#f2f2f2",
    var serverMessageColor: String = "#363636",
    var spacingBottom: Int = 0,
    var spacingRight: Int = 80,
    var windowStyle: String = "rounded",
    var pace: String? = null,
    var messages: Map<String, ConfigMessages>? = null
) : Parcelable