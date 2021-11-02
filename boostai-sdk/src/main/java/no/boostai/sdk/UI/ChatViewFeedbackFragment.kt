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

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.ChatBackend.Objects.FeedbackValue
import no.boostai.sdk.R

open class ChatViewFeedbackFragment(
    val settingsDelegate: ChatViewSettingsDelegate?,
    val isDialog: Boolean,
    val customConfig: ChatConfig? = null
) : Fragment(R.layout.chat_view_feedback),
    ChatBackend.ConfigObserver {

    lateinit var feedbackInputWrapper: LinearLayout
    lateinit var feedbackTextView: TextView
    lateinit var feedbackEditText: EditText
    lateinit var feedbackButtonsWrapper: LinearLayout
    lateinit var feedbackResponseTextView: TextView
    lateinit var backButton: TextView
    lateinit var submitButton: ImageButton
    lateinit var thumbsUpButton: ImageButton
    lateinit var thumbsDownButton: ImageButton
    var feedbackState: FeedbackState = FeedbackState.INITIAL
    var feedbackValue: FeedbackValue? = null

    enum class FeedbackState {
        INITIAL,
        PROMPT_FOR_TEXT,
        COMPLETE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        feedbackInputWrapper = view.findViewById(R.id.feedback_input)
        feedbackTextView = view.findViewById(R.id.give_feedback)
        feedbackEditText = view.findViewById(R.id.feedback_edittext)
        feedbackButtonsWrapper = view.findViewById(R.id.feedback_buttons_wrapper)
        feedbackResponseTextView = view.findViewById(R.id.feedback_response)
        backButton = view.findViewById(R.id.back)
        submitButton = view.findViewById(R.id.submit_button)
        thumbsUpButton = view.findViewById(R.id.thumbs_up)
        thumbsDownButton = view.findViewById(R.id.thumbs_down)

        val fadeAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_fast)

        view.animation = fadeAnimation
        feedbackEditText.hint = getString(R.string.feedback_text_hint)
        feedbackInputWrapper.visibility = View.GONE
        thumbsUpButton.setOnClickListener { userGavePositiveFeedback(thumbsUpButton) }
        thumbsDownButton.setOnClickListener { userGaveNegativeFeedback(thumbsDownButton) }
        backButton.setOnClickListener {
            if (isDialog) settingsDelegate?.closeChat()
            else {
                settingsDelegate?.hideFeedback()
                settingsDelegate?.hideMenu()
            }
        }
        submitButton.setOnClickListener { submitFeedback() }
        view.setOnClickListener {
            // Prevent clicks on the view to fall through to the background view
        }
        updateState()
        updateStyling(ChatBackend.config)
        ChatBackend.addConfigObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        ChatBackend.removeConfigObserver(this)
    }

    fun updateState() {
        when (feedbackState) {
            FeedbackState.INITIAL -> {
                feedbackTextView.visibility = View.VISIBLE
                feedbackButtonsWrapper.visibility = View.VISIBLE
                feedbackInputWrapper.visibility = View.GONE
                feedbackResponseTextView.visibility = View.GONE
                backButton.visibility = View.VISIBLE
            }
            FeedbackState.PROMPT_FOR_TEXT -> {
                feedbackTextView.visibility = View.VISIBLE
                feedbackButtonsWrapper.visibility = View.VISIBLE
                feedbackInputWrapper.visibility = View.VISIBLE
                feedbackResponseTextView.visibility = View.GONE
                backButton.visibility = View.GONE
            }
            FeedbackState.COMPLETE -> {
                feedbackTextView.visibility = View.GONE
                feedbackButtonsWrapper.visibility = View.GONE
                feedbackInputWrapper.visibility = View.GONE
                feedbackResponseTextView.visibility = View.VISIBLE
                backButton.visibility = View.VISIBLE
                hideKeyboard()
            }
        }
    }

    fun userGavePositiveFeedback(sender: ImageButton) =
        setFeedbackValue(FeedbackValue.positive, sender)

    fun userGaveNegativeFeedback(sender: ImageButton) =
        setFeedbackValue(FeedbackValue.negative, sender)

    fun setFeedbackValue(feedbackValue: FeedbackValue, sender: ImageButton) {
        when (feedbackValue) {
            FeedbackValue.positive -> {
                thumbsUpButton.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_thumbs_up_filled)
                )
                thumbsDownButton.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_thumbs_down)
                )
                thumbsUpButton.alpha = 1.0f
                thumbsDownButton.alpha = 0.5f
            }
            FeedbackValue.negative -> {
                thumbsUpButton.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_thumbs_up)
                )
                thumbsDownButton.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_thumbs_down_filled)
                )
                thumbsUpButton.alpha = 0.5f
                thumbsDownButton.alpha = 1.0f
            }
            else -> {}
        }
        ChatBackend.conversationFeedback(
            if (feedbackValue == FeedbackValue.positive) 1 else -1, feedbackEditText.text.toString()
        )
        this.feedbackValue = feedbackValue
        feedbackState = FeedbackState.PROMPT_FOR_TEXT
        updateState()
    }

    fun submitFeedback() {
        if (feedbackValue == null) return

        ChatBackend.conversationFeedback(
            if (feedbackValue == FeedbackValue.positive) 1 else -1, feedbackEditText.text.toString()
        )

        if (isDialog && settingsDelegate != null) {
            settingsDelegate.closeChat()
            return
        }

        feedbackState = FeedbackState.COMPLETE
        updateState()
    }

    fun hide() {
        val fadeAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out_fast)

        view?.startAnimation(fadeAnimation)
    }

    fun hideKeyboard() {
        // Hide keyboard if visible
        requireActivity().currentFocus?.let { view ->
            val imm = requireActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun updateStyling(config: ChatConfig?) {
        if (config == null) return

        val primaryColor = Color.parseColor(
            customConfig?.primaryColor ?: config.primaryColor
        )
        val contrastColor = Color.parseColor(
            customConfig?.contrastColor ?: config.contrastColor
        )
        val messages = config.messages?.get(ChatBackend.languageCode)

        view?.background = ColorDrawable(primaryColor)
        feedbackTextView.setTextColor(contrastColor)
        feedbackResponseTextView.setTextColor(contrastColor)
        thumbsUpButton.imageTintList = ColorStateList.valueOf(contrastColor)
        thumbsDownButton.imageTintList = ColorStateList.valueOf(contrastColor)
        backButton.setTextColor(primaryColor)
        (backButton.background as? GradientDrawable)?.setColor(contrastColor)
        submitButton.imageTintList = ColorStateList.valueOf(contrastColor)
        if (isDialog) backButton.text = messages?.closeWindow ?: getString(R.string.close)
        else backButton.text = messages?.back ?: getString(R.string.back)
    }

    override fun onConfigReceived(backend: ChatBackend, config: ChatConfig) = updateStyling(config)

    override fun onFailure(backend: ChatBackend, error: Exception) {}

}