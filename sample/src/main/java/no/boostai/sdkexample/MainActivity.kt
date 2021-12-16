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

package no.boostai.sdkexample

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatConfig
import no.boostai.sdk.UI.ChatViewFragment

class MainActivity : AppCompatActivity(R.layout.activity_main), ChatBackend.ConfigObserver {

    private var toolbar: Toolbar? = null
    private var viewPager: ViewPager? = null
    private var tabLayout: TabLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbar = findViewById(R.id.toolbar)
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)

        tabLayout?.background = ColorDrawable(getColor(R.color.purple))

        // Create viewPager adapter
        val adapter = ViewPagerAdapter(supportFragmentManager)

        adapter.addFragment(FloatingAvatarFragment(), "Floating")
        adapter.addFragment(ChatViewFragment(), "Fullscreen")
        tabLayout?.setupWithViewPager(viewPager)
        viewPager?.adapter = adapter
        ChatBackend.domain = "sdk.boost.ai"
        ChatBackend.languageCode =
            "no-NO" // Default value – will potentially be overridden by the backend config
        setSupportActionBar(toolbar)

        updateStyling(ChatBackend.config)
        ChatBackend.addConfigObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        ChatBackend.removeConfigObserver(this)
    }

    internal class ViewPagerAdapter(manager: FragmentManager) :
        FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private var fragments: ArrayList<Fragment> = ArrayList()
        private var fragmentTitles: ArrayList<String> = ArrayList();

        // Add fragment to the viewPager
        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment);
            fragmentTitles.add(title);
        }

        override fun getPageTitle(position: Int): CharSequence = fragmentTitles[position]

        override fun getCount(): Int = fragments.size

        override fun getItem(position: Int): Fragment = fragments[position]
    }

    private fun updateStyling(config: ChatConfig?) {
        if (config == null) return

        config.primaryColor?.let {
            val primaryColorDrawable = ColorDrawable(it)
            toolbar?.background = primaryColorDrawable
            tabLayout?.background = primaryColorDrawable
            viewPager?.background = primaryColorDrawable
        }

        config.contrastColor?.let { contrastColor ->
            tabLayout?.tabTextColors = ColorStateList.valueOf(contrastColor)
            tabLayout?.setSelectedTabIndicatorColor(contrastColor)
        }
    }

    override fun onConfigReceived(backend: ChatBackend, config: ChatConfig) {
        updateStyling(config)
    }

    override fun onFailure(backend: ChatBackend, error: Exception) {}

}