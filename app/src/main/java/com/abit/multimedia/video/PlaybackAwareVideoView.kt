package com.abit.multimedia.video

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

/**
 * A normal VideoView that also runs [onBeforeStart] right before playback starts, whether it was
 * started by our code or by the MediaController's Play button. VideoFragment uses this to pause
 * the audio player first, so audio and video never play together.
 */
class PlaybackAwareVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : VideoView(context, attrs, defStyleAttr) {

    var onBeforeStart: (() -> Unit)? = null

    override fun start() {
        onBeforeStart?.invoke()
        super.start()
    }
}
