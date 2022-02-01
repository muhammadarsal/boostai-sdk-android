package no.boostai.sdk.UI.Helpers

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView

fun TextView.handleUrlClicks(onClicked: ((String, String) -> Unit)? = null) {
    // Create span builder and replace current text with it
    text = SpannableStringBuilder.valueOf(text).apply {
        // Search for all URL spans and replace all spans with our own clickable spans
        getSpans(0, length, URLSpan::class.java).forEach {
            // Get the text clicked
            val clickedText = text.subSequence(getSpanStart(it), getSpanEnd(it)).toString()

            // Add new clickable span at the same position
            setSpan(
                object : ClickableSpan() {

                    override fun onClick(widget: View) {
                        onClicked?.invoke(it.url, clickedText)
                    }

                },
                getSpanStart(it),
                getSpanEnd(it),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            // Remove old URL span
            removeSpan(it)
        }
    }
    // Make sure movement method is set
    movementMethod = LinkMovementMethod.getInstance()
}

fun trimTrailingWhitespace(source: CharSequence?): CharSequence {
    if (source == null) return ""

    var i = source.length

    // Loop back to the first non-whitespace character
    while (--i >= 0 && Character.isWhitespace(source[i])) {}

    return source.subSequence(0, i + 1)
}