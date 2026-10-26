package com.abit.multimedia.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * With edge-to-edge (enableEdgeToEdge()) the app draws behind the status bar and the navigation
 * bar. This adds the size of those system bars (and of any display cutout) to the view's padding,
 * so its content is not hidden behind them.
 *
 * Left and right insets are always applied (they matter in landscape). [applyTop] and
 * [applyBottom] choose whether the top and bottom insets are applied as well.
 * Set [includeKeyboard] to also leave room for the on-screen keyboard.
 */
fun View.applySystemBarInsetsAsPadding(
    applyTop: Boolean = false,
    applyBottom: Boolean = false,
    includeKeyboard: Boolean = false,
) {
    // Remember the padding from XML so the insets are added to it instead of replacing it.
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        var types = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        if (includeKeyboard) types = types or WindowInsetsCompat.Type.ime()
        val insets = windowInsets.getInsets(types)
        view.updatePadding(
            left = initialLeft + insets.left,
            top = initialTop + if (applyTop) insets.top else 0,
            right = initialRight + insets.right,
            bottom = initialBottom + if (applyBottom) insets.bottom else 0,
        )
        // Return the insets unchanged so that other views can use them too.
        windowInsets
    }
}
