package com.abit.multimedia.media

/** Something that plays audio or video and can be asked to pause. */
fun interface PlaybackOwner {
    fun pausePlayback()
}

/**
 * Makes sure the audio player and the video player never play at the same time.
 *
 * NavigationUI keeps the state (and the ViewModels) of drawer screens the user has left, so the
 * audio player still exists while the Video screen is open. Each player tells this object when it
 * starts; the one that was playing before is paused.
 *
 * Only used from the main thread.
 */
object PlaybackCoordinator {

    /** The player that is currently playing, or null when nothing is playing. */
    var activeOwner: PlaybackOwner? = null
        private set

    /** Call right before [owner] starts playing. Pauses whichever other player was playing. */
    fun onPlaybackStarted(owner: PlaybackOwner) {
        val previous = activeOwner
        // Update first: pausing the previous owner may call onPlaybackStopped() for it.
        activeOwner = owner
        if (previous != null && previous !== owner) {
            previous.pausePlayback()
        }
    }

    /** Call when [owner] pauses, stops or is destroyed. */
    fun onPlaybackStopped(owner: PlaybackOwner) {
        if (activeOwner === owner) {
            activeOwner = null
        }
    }
}
