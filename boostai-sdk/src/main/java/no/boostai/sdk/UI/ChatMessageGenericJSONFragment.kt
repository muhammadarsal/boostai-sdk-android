

package no.boostai.sdk.UI

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.ChatBackend.Objects.Response.GenericCard
import no.boostai.sdk.R
import no.boostai.sdk.UI.Events.BoostUIEvents

open class ChatMessageGenericJSONFragment(
    var card: GenericCard? = null,
    val animated: Boolean = true,
    var customConfig: ChatConfig? = null
) :
    Fragment(R.layout.chat_server_message_generic_json_fragment) {

    lateinit var imageView: ImageView
    lateinit var headingTextView: TextView
    lateinit var textTextView: TextView
    lateinit var linkTextView: TextView

    val cardKey = "card"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = savedInstanceState ?: arguments
        bundle?.let {
            card = it.getParcelable(cardKey)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(cardKey, card)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.generic_json_imageview)
        headingTextView = view.findViewById(R.id.generic_json_heading_textview)
        textTextView = view.findViewById(R.id.generic_json_text_textview)
        linkTextView = view.findViewById(R.id.generic_json_link_textview)

        if (animated)
            view.animation = AnimationUtils.loadAnimation(context, R.anim.chat_message_animate_in)
        headingTextView.text = card?.heading?.text
        headingTextView.setTypeface(null, Typeface.BOLD)
        textTextView.text = card?.body?.text
        card?.image?.url?.let { Glide.with(this).load(it).into(imageView) }
        card?.link?.text?.let {
            val content = SpannableString(it)

            content.setSpan(UnderlineSpan(), 0, content.length, 0)
            linkTextView.setText(content)
        }
        card?.link?.url.let { url ->
            linkTextView.setOnClickListener {
                Intent(Intent.ACTION_VIEW).let {
                    it.data = Uri.parse(url)
                    startActivity(it)
                }

                BoostUIEvents.notifyObservers(BoostUIEvents.Event.externalLinkClicked, url)
            }
        }
        imageView.visibility = if (card?.image?.url.isNullOrEmpty()) View.GONE else View.VISIBLE
        headingTextView.visibility =
            if (headingTextView.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        textTextView.visibility = if (textTextView.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        linkTextView.visibility = if (linkTextView.text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    fun updateStyling() {
        @FontRes val bodyFont = customConfig?.chatPanel?.styling?.fonts?.bodyFont
            ?: ChatBackend.customConfig?.chatPanel?.styling?.fonts?.bodyFont
            ?: ChatBackend.config?.chatPanel?.styling?.fonts?.bodyFont
        @FontRes val headlineFont = customConfig?.chatPanel?.styling?.fonts?.headlineFont
            ?: ChatBackend.customConfig?.chatPanel?.styling?.fonts?.headlineFont
            ?: ChatBackend.config?.chatPanel?.styling?.fonts?.headlineFont

        bodyFont?.let {
            try {
                val typeface = ResourcesCompat.getFont(requireContext().applicationContext, it)
                textTextView.typeface = typeface
                linkTextView.typeface = typeface
            } catch (e: java.lang.Exception) {}
        }

        headlineFont?.let {
            try {
                val typeface = ResourcesCompat.getFont(requireContext().applicationContext, it)
                headingTextView.typeface = typeface
            } catch (e: java.lang.Exception) {}
        }
    }
}