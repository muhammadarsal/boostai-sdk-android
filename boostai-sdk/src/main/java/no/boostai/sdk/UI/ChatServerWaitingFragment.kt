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

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.ChatBackend.Objects.ChatConfigDefaults
import no.boostai.sdk.R

open class ChatServerWaitingFragment (var customConfig: ChatConfig? = null) :
    Fragment(R.layout.chat_server_waiting),
    ChatBackend.ConfigObserver {

    lateinit var dotsWrapper: ViewGroup
    var dots = arrayOfNulls<View>(3)

    val customConfigKey = "customConfig"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = savedInstanceState ?: arguments
        bundle?.let {
            customConfig = it.getParcelable(customConfigKey)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(customConfigKey, customConfig)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dotsWrapper = view.findViewById(R.id.dots);

        view.animation = AnimationUtils.loadAnimation(context, R.anim.chat_message_animate_in)
        for (index in 0 until dotsWrapper.childCount) {
            val dotAnimation = AnimationUtils.loadAnimation(context, R.anim.chat_waiting_animate)

            dots[index] = dotsWrapper.getChildAt(index)
            dotAnimation.repeatMode = Animation.REVERSE;
            dotAnimation.startOffset = 100.times(index).toLong();
            dots[index]?.startAnimation(dotAnimation)
        }
        ChatBackend.config?.let { updateStyling(it) }
        ChatBackend.addConfigObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        ChatBackend.removeConfigObserver(this)
    }

    fun updateStyling(config: ChatConfig) {
        val backgroundColor = Color.parseColor(
            customConfig?.serverMessageBackground ?: config.serverMessageBackground ?: ChatConfigDefaults.serverMessageBackground
        )
        val textColor = Color.parseColor(
            customConfig?.serverMessageColor ?: config.serverMessageColor ?: ChatConfigDefaults.serverMessageColor
        )

        backgroundColor.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                view?.background?.colorFilter =
                    BlendModeColorFilter(it, BlendMode.SRC_ATOP)
            else view?.background?.setColorFilter(it, PorterDuff.Mode.SRC_ATOP)
        }
        textColor.let { color: Int ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                dots.forEach {
                    it?.background?.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
                }
            else dots.forEach { it?.background?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP) }
        }
    }

    override fun onConfigReceived(backend: ChatBackend, config: ChatConfig) = updateStyling(config)

    override fun onFailure(backend: ChatBackend, error: Exception) {}

}