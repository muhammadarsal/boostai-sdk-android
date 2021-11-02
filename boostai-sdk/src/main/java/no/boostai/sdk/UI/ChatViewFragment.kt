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
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.ChatBackend.Objects.Response.APIMessage
import no.boostai.sdk.ChatBackend.Objects.Response.ChatStatus
import no.boostai.sdk.ChatBackend.Objects.Response.Response
import no.boostai.sdk.ChatBackend.Objects.Response.SourceType
import no.boostai.sdk.R
import no.boostai.sdk.UI.Helpers.TimingHelper
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

open class ChatViewFragment(
    val isDialog: Boolean = false,
    val customConfig: ChatConfig? = null,
    val delegate: ChatViewFragmentDelegate? = null
) :
    Fragment(R.layout.chat_view),
    ChatBackend.MessageObserver,
    ChatBackend.ConfigObserver,
    ChatViewSettingsDelegate {
    
    val errorId = "error"
    val settingsFragmentId = "settings"
    val feedbackFragmentId = "feedback"
    lateinit var chatContent: FrameLayout
    lateinit var secureChatWrapper: LinearLayout
    lateinit var editText: EditText
    lateinit var submitButton: ImageButton
    lateinit var characterCountTextView: TextView
    lateinit var characterCountWrapper: FrameLayout
    lateinit var scrollView: ScrollView
    lateinit var chatMessagesLayout: LinearLayout
    lateinit var chatInputBorder: FrameLayout
    var lastAvatarURL: String? = null
    var maxCharacterCount = 110
    var messages: ArrayList<APIMessage> = ArrayList()
    var responses: ArrayList<Response> = ArrayList()
    var waitingForAgentResponseFragmentTags: ArrayList<String> = ArrayList()
    var isBlocked = false
    var isSecureChat = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chatContent = view.findViewById(R.id.chat_content)
        secureChatWrapper = view.findViewById(R.id.secure_chat_wrapper)
        editText = view.findViewById(R.id.chat_input_editText)
        submitButton = view.findViewById(R.id.chat_input_submit_button)
        characterCountTextView = view.findViewById(R.id.chat_input_character_count_textview)
        characterCountWrapper = view.findViewById(R.id.chat_input_character_count_wrapper)
        scrollView = view.findViewById(R.id.chat_messages_scrollview)
        chatMessagesLayout = view.findViewById(R.id.chat_messages)
        chatInputBorder = view.findViewById(R.id.chat_input_border)

        scrollView.isSmoothScrollingEnabled = true
        editText.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                sequence: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { updateInputStates(it) }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, after: Int) {}

        })
        editText.setOnKeyListener(object : View.OnKeyListener {

            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event?.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    val text = editText.text.toString().trim()
                    if (text.isNotEmpty()) {
                        submitText(text)
                        return true
                    }
                }

                return false
            }

        })
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            // Update chat input color
            val hexColor = customConfig?.primaryColor ?: ChatBackend.config?.primaryColor
            var color = hexColor?.let { Color.parseColor(it) } ?: ContextCompat.getColor(requireContext(), R.color.purple)

            if (hasFocus) {
                color = color and 0x00FFFFFF or 0x77000000 // Set opacity
            } else {
                color = color and 0x00FFFFFF // Transparent
            }

            val gradientDrawable = chatInputBorder.background as GradientDrawable
            gradientDrawable.setColor(color)// Update chat input color
        }
        submitButton.setOnClickListener {
            val text = editText.text.toString().trim()

            if (text.isNotEmpty()) submitText(text)
        }
        // Set up menu
        setHasOptionsMenu(true)
        ChatBackend.addConfigObserver(this)
        ChatBackend.addMessageObserver(this)
        ChatBackend.onReady(object : ChatBackend.ConfigReadyListener {

            override fun onFailure(exception: Exception) {}

            override fun onReady(config: ChatConfig) {
                updateStyling(config)
                startConversation()
            }

        })
    }

    override fun onDestroy() {
        super.onDestroy()

        ChatBackend.removeConfigObserver(this)
        ChatBackend.removeMessageObserver(this)
    }

    fun startConversation() {
        val messages = ChatBackend.messages

        setIsBlocked(ChatBackend.isBlocked)
        messages.forEach { handleReceivedMessage(it, false) }
        isBlocked = ChatBackend.isBlocked
        // Start new conversation if no messages exists
        if (messages.size == 0) {
            showWaitingForAgentResponseIndicator()
            ChatBackend.start()
        } else
            // Scroll to bottom after x ms
            Timer().schedule(200) { scrollToBottom() }
    }

    fun setIsBlocked(isBlocked: Boolean) {
        this.isBlocked = isBlocked
        editText.inputType =
            if (isBlocked) InputType.TYPE_NULL else InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
        editText.hint =
            if (isBlocked) null
            else ChatBackend.config?.messages?.get(ChatBackend.languageCode)?.composePlaceholder ?:
                getString(R.string.chat_input_placeholder)
        updateSubmitButtonState()
    }

    override fun onMessageReceived(backend: ChatBackend, message: APIMessage) {
        setIsBlocked(ChatBackend.isBlocked)
        handleReceivedMessage(message)
    }

    override fun onConfigReceived(backend: ChatBackend, config: ChatConfig) {
        updateStyling(config)
        activity?.invalidateOptionsMenu()
    }

    override fun onFailure(backend: ChatBackend, error: Exception) {
        hideWaitingForAgentResponseIndicator()
        showStatusMessage(error.localizedMessage ?: getString(R.string.unknown_error), true)
    }

    fun handleReceivedMessage(message: APIMessage, animated: Boolean = true) {
        val messageResponses =
            message.responses?.let { ArrayList(message.responses) } ?: ArrayList()

        messages.add(message)
        message.response?.let { messageResponses.add(it) }

        messageResponses.forEachIndexed { index, response ->
            // Add it to our response list
            responses.add(response)
            // Store the last avatar URL for later re-use
            lastAvatarURL = response.avatarUrl ?: lastAvatarURL
            // Render the response
            render(
                response,
                animated,
                !(
                    messages.indexOf(message) >
                        messages.indexOfFirst {
                            (conversation) -> conversation?.state?.isBlocked == null
                        }
                ),
                message.conversation?.state?.awaitingFiles != null &&
                    index == messageResponses.size - 1
            )
            // Are we waiting for an agent response? Show a "waiting view"
            val chatStatus =
                ChatBackend.lastResponse?.conversation?.state?.chatStatus ?:
                    ChatStatus.virtual_agent

            if (
                response.source == SourceType.client &&
                    chatStatus == ChatStatus.virtual_agent && animated
            )
                showWaitingForAgentResponseIndicator()
            else hideWaitingForAgentResponseIndicator()
            // Should we show the "Secure chat" badge?
            val isSecure =
                message.conversation?.state?.authenticatedUserId != null ||
                    (
                        isSecureChat && messageResponses.size > 0 &&
                            messageResponses[0].source == SourceType.client
                    )

            secureChatWrapper.visibility = if (isSecure) View.VISIBLE else View.GONE
        }

        hideStatusMessage()

        // Show human typing indicator if applicable
        if (message.conversation?.state?.humanIsTyping == true) {
            if (messageResponses.size > 0)
                hideHumanTypingIndicator()
            showHumanTypingIndicator()
        } else {
            hideHumanTypingIndicator()
        }
    }

    fun submitText(text: String) {
        ChatBackend.message(text)
        // Update view state
        editText.setText("")
        updateInputStates("")
    }

    fun updateStyling(config: ChatConfig?) {
        if (config == null) return

        config.messages?.get(ChatBackend.languageCode)?.composePlaceholder?.let {
            editText.hint = it
        }

        updateSubmitButtonState()
    }

    @SuppressLint("SetTextI18n")
    fun updateInputStates(text: String) {
        val textLength = text.length
        val value = ChatBackend.clientTyping(text)

        maxCharacterCount = value.maxLength
        characterCountTextView.text = "$textLength / $maxCharacterCount"
        characterCountWrapper.visibility = if (editText.lineCount >= 3) View.VISIBLE else View.GONE

        editText.filters = arrayOf(InputFilter.LengthFilter(maxCharacterCount))

        updateSubmitButtonState(text)
    }

    fun updateSubmitButtonState(text: String? = null) {
        val currentText = text ?: editText.text.toString()
        val configPrimaryColor = customConfig?.primaryColor ?: ChatBackend.config?.primaryColor
        val primaryColor =
            configPrimaryColor?.let { Color.parseColor(configPrimaryColor) } ?: R.color.purple
        val isEnabled = currentText.trim().isNotEmpty() && !isBlocked

        submitButton.isEnabled = isEnabled
        submitButton.backgroundTintList = ColorStateList.valueOf(
            if (isEnabled) primaryColor else ContextCompat.getColor(requireContext(), R.color.gray)
        )
    }

    fun render(
        response: Response,
        animated: Boolean = true,
        isWelcomeMessage: Boolean,
        isAwaitingFiles: Boolean
    ) {
        if (childFragmentManager.findFragmentByTag(response.id) == null) {
            val fragment = delegate?.getChatMessageFragment(response, animated) ?:
                getMessageFragment(
                    response,
                    animated,
                    response.source == SourceType.client,
                    isWelcomeMessage,
                    isAwaitingFiles
                )

            // Add the message
            childFragmentManager.beginTransaction()
                .add(
                    R.id.chat_messages,
                    fragment,
                    response.id
                )
                .commitAllowingStateLoss()
            if (animated) {
                val pace = ChatBackend.config?.pace ?: "normal"
                val paceFactor = TimingHelper.calculatePace(pace)
                val staggerDelay = TimingHelper.calculateStaggerDelay(pace, 1)
                val timeUntilReveal = TimingHelper.calcTimeToRead(paceFactor)

                // Animate the scroll view to the bottom after each element display
                response.elements.forEachIndexed { index, _ ->
                    // Scroll to bottom after x ms
                    Timer().schedule(timeUntilReveal * index + staggerDelay + 100) {
                        scrollToBottom()
                    }
                }
                if (isAwaitingFiles)
                    Timer().schedule(
                        timeUntilReveal * response.elements.size + staggerDelay + 100
                    ) {
                        scrollToBottom()
                    }
            } else {
                scrollToBottom()
                Timer().schedule(100) { scrollToBottom(false) }
            }
        }
    }

    fun scrollToBottom(smoothScroll: Boolean = true) =
        scrollView.post {
            if (smoothScroll) scrollView.smoothScrollTo(0, chatMessagesLayout.bottom)
            else scrollView.scrollTo(0, chatMessagesLayout.bottom)
        }

    fun showHumanTypingIndicator() {
        if (childFragmentManager.findFragmentByTag("humanTyping") != null)
            return

        childFragmentManager
            .beginTransaction()
            .add(
                R.id.chat_messages,
                getHumanTypingFragment(),
                "humanTyping"
            )
            .commitAllowingStateLoss()

        Timer().schedule(200) {
            scrollToBottom(true)
        }
    }

    fun hideHumanTypingIndicator() {
        // Remove any visible "human typing" waiting indicators
        childFragmentManager.findFragmentByTag("humanTyping")?.let {
            childFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
        }
    }

    fun showWaitingForAgentResponseIndicator() {
        val uuid = UUID.randomUUID().toString()
        childFragmentManager
            .beginTransaction()
            .add(
                R.id.chat_messages,
                getWaitingForServerResponseFragment(),
                uuid
            )
            .commitAllowingStateLoss()
        waitingForAgentResponseFragmentTags.add(uuid)

        Timer().schedule(200) {
            scrollToBottom(true)
        }
    }

    fun hideWaitingForAgentResponseIndicator() {
        // Remove any visible "waiting for agent response" waiting indicators
        val transaction = childFragmentManager.beginTransaction()
        waitingForAgentResponseFragmentTags.forEach { tag ->
            childFragmentManager.findFragmentByTag(tag)?.let {
                transaction.remove(it)
            }
        }
        waitingForAgentResponseFragmentTags.clear()
        transaction.commitAllowingStateLoss()
    }

    fun toggleSettingsFragment() {
        val settingsFragment =
            childFragmentManager.findFragmentByTag(settingsFragmentId)
        val feedbackFragment =
            childFragmentManager.findFragmentByTag(feedbackFragmentId)

        if (settingsFragment != null) {
            hideMenu()
        } else showMenu()

        if (feedbackFragment != null) {
            hideFeedback()
        }
    }

    fun showStatusMessage(message: String, isError: Boolean) {
        hideStatusMessage()
        childFragmentManager
            .beginTransaction()
            .add(
                R.id.chat_messages,
                getStatusMessageFragment(message, isError),
                errorId
            )
            .commitAllowingStateLoss()

        Timer().schedule(200) {
            scrollToBottom(true)
        }
    }

    fun hideStatusMessage() {
        // Remove any visible status messages
        childFragmentManager.findFragmentByTag(errorId)?.let {
            childFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.chat_toolbar_menu, menu)

        val tintColorString = customConfig?.contrastColor ?: ChatBackend.config?.contrastColor
        val tintColor =
            if (tintColorString != null) Color.parseColor(tintColorString)
            else ContextCompat.getColor(requireContext(), R.color.white)

        // Set correct color on settings item
        val settingsItem = menu.findItem(R.id.action_settings)
        val settingsDrawable = DrawableCompat.wrap(settingsItem.icon!!)
        DrawableCompat.setTint(settingsDrawable, tintColor)
        settingsItem.icon = settingsDrawable

        // Set correct color on close item
        val closeItem = menu.findItem(R.id.action_close)
        val closeDrawable = DrawableCompat.wrap(closeItem.icon!!)
        DrawableCompat.setTint(closeDrawable, tintColor)
        closeItem.icon = closeDrawable

        // Set correct color on minimize item
        val minimizeItem = menu.findItem(R.id.action_minimize)
        val minimizeDrawable = DrawableCompat.wrap(minimizeItem.icon!!)
        DrawableCompat.setTint(minimizeDrawable, tintColor)
        minimizeItem.icon = minimizeDrawable

        // Set correct color on filter item
        val filterItem = menu.findItem(R.id.action_filter)
        filterItem.title = ChatBackend.filter?.title ?: getString(R.string.filter)
        val s = SpannableString(filterItem.title)
        s.setSpan(ForegroundColorSpan(tintColor), 0, s.length, 0)
        filterItem.title = s

        // Add filters as a submenu
        val filterMenu = filterItem.subMenu
        filterMenu.clear()
        ChatBackend.config?.filters?.forEach { filterMenu.add(0, it.id, Menu.NONE, it.title) }

        // Only show the filter selector if we have any filters
        filterItem.isVisible = filterMenu.size() > 0

        minimizeItem.isVisible = isDialog
        closeItem.isVisible = isDialog
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> toggleSettingsFragment()
            R.id.action_filter -> {}
            R.id.action_close -> closeChatWithFeedback()
            R.id.action_minimize -> activity?.finish()
            else -> {
                // Submenu was clicked
                ChatBackend.filter = ChatBackend.config?.filters?.find { it.id == item.itemId }
                requireActivity().invalidateOptionsMenu()
            }
        }

        return true
    }

    override fun deleteConversation() {
        ChatBackend.delete(null, object : ChatBackend.APIMessageResponseListener {

            override fun onFailure(exception: java.lang.Exception) {}

            override fun onResponse(apiMessage: APIMessage) {
                // Remove all message fragments
                val transaction = childFragmentManager.beginTransaction()

                responses.forEach {
                    val fragment = childFragmentManager.findFragmentByTag(it.id)

                    if (fragment != null) transaction.remove(fragment)
                }
                transaction.commitAllowingStateLoss()
                // Empty the responses list
                responses = ArrayList()
                // Hide menu
                hideMenu()
                // Start a new conversation
                startConversation()
            }

        })
    }

    override fun showMenu() {
        hideKeyboard()
        childFragmentManager
            .beginTransaction()
            .add(
                R.id.chat_content,
                delegate?.getSettingsFragment() ?: getChatViewSettingsFragment(),
                settingsFragmentId
            )
            .commitAllowingStateLoss()
    }

    override fun hideMenu() {
        val settingsFragment =
            childFragmentManager.findFragmentByTag(settingsFragmentId) as? ChatViewSettingsFragment

        if (settingsFragment != null) {
            settingsFragment.hide()
            Timer().schedule(150) {
                childFragmentManager
                    .beginTransaction()
                    .remove(settingsFragment)
                    .commitAllowingStateLoss()
            }
        }
    }

    override fun showFeedback() {
        hideKeyboard()
        childFragmentManager
            .beginTransaction()
            .add(
                R.id.chat_content,
                delegate?.getFeedbackFragment() ?: getChatViewFeedbackFragment(),
                feedbackFragmentId
            )
            .commitAllowingStateLoss()
    }

    override fun hideFeedback() {
        val feedbackFragment =
            childFragmentManager.findFragmentByTag(feedbackFragmentId) as? ChatViewFeedbackFragment

        if (feedbackFragment != null) {
            feedbackFragment.hide()
            Timer().schedule(150) {
                childFragmentManager
                    .beginTransaction()
                    .remove(feedbackFragment)
                    .commitAllowingStateLoss()
            }
        }
    }

    override fun closeChat() { activity?.finish() }

    fun closeChatWithFeedback() {
        // If the feedback window is open and the user taps X again, close the dialog
        val feedbackFragment = childFragmentManager.findFragmentByTag(feedbackFragmentId)
        if (feedbackFragment != null) {
            activity?.finish();
            return;
        }

        // Should we request conversation feedback? Show feedback dialogue
        val hasClientMessages = ChatBackend.messages.filter { apiMessage ->
            val source = apiMessage.response?.source
                ?: apiMessage.responses?.first()?.source
                ?: SourceType.bot

            source == SourceType.client
        }.isNotEmpty()

        if (ChatBackend.config?.requestConversationFeedback == true && hasClientMessages)
            showFeedback()
        else
            // If all else fails, close the window
            activity?.finish()
    }

    fun hideKeyboard() =
        // Hide keyboard if visible
        requireActivity().currentFocus?.let { view ->
            val imm =
                requireActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }

    fun getMessageFragment(
        response: Response,
        animated: Boolean,
        isClient: Boolean,
        isWelcomeMessage: Boolean,
        isAwaitingFiles: Boolean
    ): Fragment = ChatMessageFragment(
        response,
        animated,
        isBlocked,
        isClient,
        isWelcomeMessage,
        isWaitingForServerResponse = false,
        isAwaitingFiles,
        avatarUrl = lastAvatarURL,
        customConfig = customConfig,
        delegate
    )

    fun getHumanTypingFragment(): Fragment = ChatHumanTypingFragment()

    fun getWaitingForServerResponseFragment(): Fragment = ChatMessageFragment(
        response = null,
        animated = false,
        isBlocked = false,
        isClient = false,
        isWelcomeMessage = false,
        isWaitingForServerResponse = true,
        avatarUrl = lastAvatarURL,
        customConfig = customConfig,
        delegate = delegate
    )

    fun getStatusMessageFragment(message: String, isError: Boolean): Fragment =
        StatusMessageFragment(message, isError)

    fun getChatViewFeedbackFragment(): Fragment =
        ChatViewFeedbackFragment(this, isDialog, customConfig)

    fun getChatViewSettingsFragment(): Fragment =
        ChatViewSettingsFragment(this, isDialog, customConfig)

}