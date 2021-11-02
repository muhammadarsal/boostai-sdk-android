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
import android.view.animation.AnimationUtils
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import no.boostai.sdk.R

open class ChatMessageVideoFragment(val source: String, val url: String) :
    Fragment(R.layout.chat_server_message_video) {

    var contentWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contentWebView = view.findViewById(R.id.chat_server_message_webview)

        var videoEmbedHTML: String? = null

        contentWebView?.webChromeClient = WebChromeClient()
        contentWebView?.webViewClient = WebViewClient()
        contentWebView?.settings?.javaScriptEnabled = true
        view.animation = AnimationUtils.loadAnimation(context, R.anim.chat_message_animate_in)
        when (source) {
            "youtube" -> videoEmbedHTML = youTubeVideo(url)
            "vimeo" -> videoEmbedHTML = vimeoVideo(url)
            "wistia" -> videoEmbedHTML = wistiaVideo(url)
        }
        videoEmbedHTML?.let {
            contentWebView?.loadData(it, "text/html; charset=UTF-8", null)
            contentWebView?.visibility = View.VISIBLE
        }
    }

    /// Message view for a YouTube video
    /// - Parameter url: A URL for a single YouTube video, i.e. `https://www.youtube.com/watch?v=gcugRVtkBtU`
    /// - Returns: HTML of a YouTube embedded player
    fun youTubeVideo(url: String): String? {
        val id = extractYouTubeID(url) ?: return null
        val embedURL = "https://www.youtube.com/embed/$id"

        return videoEmbedHTML(embedURL)
    }

    /// Message view for a Vimeo video
    /// - Parameter url: A URL for a single Vimeo video, i.e. `https://vimeo.com/316511760`
    /// - Returns: HTML of a Vimeo embedded player
    fun vimeoVideo(url: String): String? {
        val id = extractVimeoID(url) ?: return null
        val embedURL = "https://player.vimeo.com/video/$id?title=0&byline=0&styling=portrait"

        return videoEmbedHTML(embedURL)
    }

    /// Message view for a Wistia video
    /// - Parameter url: A URL for a single Wistia video, i.e. `https://wistia.com/lbo2kwzc81`
    /// - Returns: HTML of a Wistia embedded player
    fun wistiaVideo(url: String): String? {
        val id = extractWistiaURL(url) ?: return null
        val embedURL = "https://fast.wistia.net/embed/iframe/$id?seo=false&videoFoam=true"

        return videoEmbedHTML(embedURL)
    }

    fun extractYouTubeID(url: String): String? {
        val pattern = Regex("(.*?)(^|/|v=)([a-z0-9_-]{11})(.*)?", RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(url)

        matches.forEach { matchResult ->
            if (matchResult.groups.size >= 4) return matchResult.groups[3]?.value
        }

        return null
    }

    fun extractVimeoID(url: String): String? {
        val pattern = Regex(
            "(http|https)?://(www\\.|player\\.)?vimeo\\.com/(?:channels/(?:\\w+/)?|groups/([^/]*)/videos/|video/|)(\\d+)(?:|/\\?)",
            RegexOption.IGNORE_CASE
        )
        val matches = pattern.findAll(url)

        matches.forEach { matchResult ->
            if (matchResult.groups.size >= 5) return matchResult.groups[4]?.value
        }

        return null
    }

    fun extractWistiaURL(url: String): String? {
        val pattern = Regex(
            "(http|https)?:?//(www)?wistia.com/([a-z0-9]+)/?",
            RegexOption.IGNORE_CASE
        )
        val matches = pattern.findAll(url)

        matches.forEach { matchResult ->
            if (matchResult.groups.size >= 4) { return matchResult.groups[3]?.value }
        }

        return null
    }

    fun videoEmbedHTML(url: String): String = """
        <!doctype html>
        <html>
            <head>
                <style>
                    html,
                    body {
                        overflow: hidden;
                    }
                    
                    body {
                        margin: 0;
                        padding: 0;
                        width: 100vw;
                        height: 100vh;
                    }
        
                    iframe {
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                    }
                </style>
            </head>
            <body>
                <iframe src="$url" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" scrolling="no" playsinline webkitAllowFullScreen mozallowfullscreen allowfullscreen></iframe>
            </body>
        </html>
    """.trimIndent()

}