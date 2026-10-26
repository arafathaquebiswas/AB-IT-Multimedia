package com.abit.multimedia.about

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.abit.multimedia.BuildConfig
import com.abit.multimedia.R
import com.abit.multimedia.databinding.FragmentAboutBinding

/**
 * About screen: the AB IT logo, the app version, the privacy policy and the credits.
 * Opened from the toolbar overflow menu or by tapping the drawer header.
 */
class AboutFragment : Fragment(R.layout.fragment_about) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The binding is only needed here, so it isn't kept in a field.
        val binding = FragmentAboutBinding.bind(view)
        binding.aboutVersionText.text = getString(R.string.about_version_format, BuildConfig.VERSION_NAME)
    }
}
