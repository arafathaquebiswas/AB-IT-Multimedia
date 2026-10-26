# Project Guide — how AB IT Multimedia works (viva notes)

A beginner-friendly walk through the code of CSE 489 Assignment 2, in the order you would explain
it. File paths are relative to `app/src/main/java/com/abit/multimedia/` unless they start with `res/`.

---

## 1. The big picture

```
MainActivity  (DrawerLayout + Toolbar + NavHostFragment)
 ├─ BroadcastFragment ──Proceed──► BroadcastInputActivity (Activity 2) ──► CustomReceiverActivity (Activity 3)
 ├─ ImageScaleFragment            custom: text field                     custom: register receiver, send, receive
 ├─ VideoFragment                 battery: live battery level            battery: "nothing further to do"
 ├─ AudioFragment
 └─ AboutFragment  (toolbar menu / drawer header)
```

- The **four drawer screens are Fragments** inside one Activity.
- The **broadcast flow uses real Activities** (Activity 2 and 3), as the assignment asks, started
  with explicit Intents.
- Code is grouped by feature: `broadcast/`, `image/`, `video/`, `audio/`, `media/`, `about/`, `ui/`.

## 2. Activities and Fragments

- An **Activity** is a full screen with its own window and lifecycle
  (`onCreate → onStart → onResume … onPause → onStop → onDestroy`).
- A **Fragment** is a reusable part of a screen that lives *inside* an Activity and has its own
  lifecycle, plus a *view* lifecycle (`onViewCreated … onDestroyView`). Its view can be destroyed
  (for example when you switch drawer items) while the Fragment object and its ViewModel are kept.

That is why every Fragment keeps its ViewBinding in a nullable field and clears it:

```kotlin
private var _binding: FragmentAudioBinding? = null
private val binding get() = _binding!!          // only valid between onViewCreated and onDestroyView

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null                              // don't keep destroyed views in memory
}
```

**ViewBinding** (enabled with `buildFeatures { viewBinding = true }`) generates a class for each
layout, e.g. `activity_main.xml → ActivityMainBinding`, so views are accessed type-safely
(`binding.toolbar`) instead of with `findViewById`.

## 3. Navigation drawer + Navigation Component

Pieces (`res/layout/activity_main.xml`, `MainActivity.kt`):

| Piece | Job |
|---|---|
| `DrawerLayout` | the container that lets the drawer slide over the content |
| `NavigationView` | the drawer itself: header (`nav_header_main.xml`) + menu (`activity_main_drawer.xml`) |
| `MaterialToolbar` | the app bar, set as the action bar |
| `NavHostFragment` | shows the current destination from `res/navigation/nav_graph.xml` |
| `NavController` | moves between destinations and keeps the back stack |
| `AppBarConfiguration` | says which screens are *top-level* (hamburger) vs. secondary (Up arrow) |
| `NavigationUI` | connects the toolbar and drawer to the NavController |

```kotlin
appBarConfiguration = AppBarConfiguration(
    setOf(R.id.nav_broadcast, R.id.nav_image_scale, R.id.nav_video, R.id.nav_audio),
    binding.drawerLayout,
)
setupActionBarWithNavController(navController, appBarConfiguration) // title + hamburger/Up arrow
binding.navView.setupWithNavController(navController)               // drawer clicks + highlight
```

The key trick: **menu item IDs are the same as destination IDs** (`@id/nav_video` in both the
menu and the graph). NavigationUI uses that to open the right Fragment, highlight the right item
and set the toolbar title from the destination's `android:label`. It also pops back to the start
destination when you pick a drawer item, so the back stack never fills up with duplicates.

**Back closes the drawer first.** An `OnBackPressedCallback` is enabled only while the drawer is
open; because it is registered after the NavHostFragment's own callback, it gets Back first:

```kotlin
private val closeDrawerOnBack = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() { binding.drawerLayout.closeDrawer(GravityCompat.START) }
}
// enabled in onDrawerOpened(), disabled in onDrawerClosed()
```

`onPostCreate()` re-syncs it after rotation, because a drawer restored as open does not call
`onDrawerOpened()`.

**About** is not one of the four drawer items (the drawer must have exactly four). It is in the
toolbar overflow menu (`res/menu/main_overflow.xml`) and opens when the drawer header is tapped.

## 4. Broadcasts and BroadcastReceivers

A **broadcast** is a message (an `Intent` with an *action*) sent to every receiver that registered
for that action. A **BroadcastReceiver** gets it in `onReceive(context, intent)`, on the main thread.

| | System broadcast | Custom broadcast |
|---|---|---|
| Sender | Android (BatteryService) | our own app |
| Action | `Intent.ACTION_BATTERY_CHANGED` | `<applicationId>.action.CUSTOM_MESSAGE` |
| Receiver | anonymous receiver in `BatteryStatusLiveData` | `CustomMessageReceiver` |
| Registered | while Activity 2 (battery) is visible | while Activity 3 (custom) is visible |

Both receivers are **context-registered** (in code, at runtime), not declared in the manifest.
Since Android 8, manifest receivers no longer get most implicit broadcasts, and the assignment asks
for the custom receiver to be created inside Activity 3.

### Export flags

Since Android 14, a runtime receiver must say whether other apps may send to it:

```kotlin
ContextCompat.registerReceiver(
    this, receiver, IntentFilter(CustomBroadcast.action(packageName)),
    ContextCompat.RECEIVER_NOT_EXPORTED,   // other apps can't reach it; the system still can
)
```

`ContextCompat` makes this work on older versions too (on Android 12L and lower it protects the
receiver with a permission only this app holds). In the manifest, only `MainActivity` has
`android:exported="true"` (the launcher must start it); Activity 2 and 3 are `exported="false"`.

### Battery: a *sticky* broadcast

`ACTION_BATTERY_CHANGED` is **sticky**: the system keeps the last one, and `registerReceiver()`
returns it immediately. So the percentage appears at once, without waiting for the next change.

```kotlin
override fun onActive() {                        // first observer became visible (STARTED)
    val stickyIntent = ContextCompat.registerReceiver(appContext, batteryReceiver,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
    if (stickyIntent != null) value = BatteryStatus.fromIntent(stickyIntent)
}
override fun onInactive() { appContext.unregisterReceiver(batteryReceiver) }   // screen hidden
```

`BatteryStatusLiveData` is a `LiveData`: LiveData calls `onActive()` when someone observes it from
a visible screen and `onInactive()` when that screen stops. So the receiver only runs while the
battery screen is on screen — no leaks and no wasted work.

The level arrives as `EXTRA_LEVEL` out of `EXTRA_SCALE` (usually 100, but not always):

```kotlin
fun percentageOf(level: Int, scale: Int): Int? {
    if (level < 0 || scale <= 0) return null      // missing value or division by zero
    return (level * 100 / scale).coerceIn(0, 100)
}
```

`EXTRA_STATUS` becomes Charging / Discharging / Full / Not charging (`ChargingStatus`). The time of
each update is stored with the status and shown as “Last update: …”. No permission is needed.

## 5. Why Activity 3 sends the custom broadcast

A runtime-registered receiver only hears broadcasts that arrive **while it is registered**.
If Activity 2 sent the broadcast and then started Activity 3, the broadcast could be delivered
before Activity 3 had even been created — and the message would be lost.

So the order is:

1. Activity 2 passes the text to Activity 3 in an **Intent extra** (just data, not a broadcast).
2. Activity 3 **registers** `CustomMessageReceiver` in `onStart()`.
3. Only **then** does it **send** the broadcast with the text.
4. The receiver's `onReceive()` gets it → “Received via broadcast: Hello CSE489”, a Toast and Logcat.

```kotlin
override fun onStart() {
    super.onStart()
    if (mode != BroadcastMode.CUSTOM) return       // battery option: no custom receiver at all
    registerCustomMessageReceiver()                // 1) register first...
    if (!viewModel.initialBroadcastSent) {         // 2) ...then send, only once
        viewModel.initialBroadcastSent = true
        sendCustomBroadcast()
    }
}
override fun onStop() { unregisterCustomMessageReceiver(); super.onStop() }
```

The broadcast is restricted to our own app with `setPackage()`:

```kotlin
Intent(action(applicationId))
    .setPackage(applicationId)                     // only this app can receive it
    .putExtra(messageExtra(applicationId), message)
```

The action and extra names are built from the application ID **at runtime** (`context.packageName`),
e.g. `com.abit.multimedia.action.CUSTOM_MESSAGE`.

**Rotation:** Android destroys and recreates the Activity. The receiver is unregistered in
`onStop()` and registered again in `onStart()`, but `initialBroadcastSent` (kept in the ViewModel's
`SavedStateHandle`) stops it from sending again, and the received message is restored from the
ViewModel. So rotating never causes a duplicate broadcast.

## 6. Intents and extras

An **explicit Intent** names the exact Activity class to start. Each Activity offers a small
`newIntent()` helper so the extras are always put in the same way:

```kotlin
fun newIntent(context: Context, mode: BroadcastMode): Intent =
    Intent(context, BroadcastInputActivity::class.java)
        .putExtra(BroadcastMode.EXTRA_MODE, mode.name)       // "CUSTOM" or "BATTERY"
```

The receiving Activity reads it back safely; a missing or wrong value gives `null` and the
Activity closes instead of crashing:

```kotlin
when (BroadcastMode.fromExtraValue(intent.getStringExtra(BroadcastMode.EXTRA_MODE))) {
    BroadcastMode.CUSTOM -> showCustomMessageForm()
    BroadcastMode.BATTERY -> showBatteryStatus()
    null -> finish()
}
```

The Proceed / Send / Next buttons ignore further presses until the screen is shown again, so a
double tap (or a keyboard reporting Send twice) cannot open the next Activity twice.

## 7. ViewModel and lifecycle

On rotation Android destroys the Activity/Fragment and creates a new one. A **ViewModel** survives
this, so it is the place for state that must not be lost:

| ViewModel | Keeps |
|---|---|
| `BatteryViewModel` | the battery LiveData |
| `CustomReceiverViewModel` | received message, time, count, “already sent” flag (in `SavedStateHandle`, which even survives the app being killed in the background) |
| `VideoViewModel` | video position and whether to keep playing |
| `AudioPlayerViewModel` | the `MediaPlayer` itself and the screen state |

Screens get them with `by viewModels()`. UI state from the audio player is a `StateFlow`, collected
only while the screen is visible:

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {   // stops in onStop, restarts in onStart
        viewModel.uiState.collect { state -> render(state) }
    }
}
```

The Spinner keeps its selection on rotation by itself, because Android saves the state of views
that have an `android:id`.

## 8. Image Scale: ScaleGestureDetector and Matrix

`image/ZoomableImageView.kt` is an `ImageView` with `scaleType = MATRIX`: it draws the image
through a `Matrix` (a 3 × 3 transform for scale + translate) that we control.

- `fitMatrix` — scales and centres the image so it fits the view (this is **1x**).
  Recalculated whenever the view size changes or a new image arrives from Coil.
- `userMatrix` — the user's zoom and pan, applied on top.

**Pinch**: `ScaleGestureDetector` gives a `scaleFactor` (how much the fingers spread) and a focus
point (between the fingers). Scaling *around the focus point* keeps the spot under your fingers in
place:

```kotlin
private fun zoomBy(factor: Float, focusX: Float, focusY: Float) {
    val newZoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)    // 1x .. 5x
    val appliedFactor = newZoom / zoom
    userMatrix.postScale(appliedFactor, appliedFactor, focusX - paddingLeft, focusY - paddingTop)
    zoom = newZoom
    keepImageInsideView()
    applyMatrix()
}
```

**Pan**: `GestureDetector.onScroll()` moves the image with one finger, only while zoomed.
**Double-tap**: `onDoubleTap()` animates to 2.5x at the tapped point, or back to 1x.
**Bounds**: `keepImageInsideView()` maps the image rectangle through the matrix and moves it back
so it can't be dragged off screen (or centres it when it's smaller than the view).
While zoomed, `requestDisallowInterceptTouchEvent(true)` stops the DrawerLayout from stealing the drag.

The photo is loaded with **Coil 3** at its full 2000 × 1300 size (`size(Size.ORIGINAL)`) so it
stays sharp when zoomed; Coil downloads in the background, caches it and cancels automatically if
the screen closes. Loading, error (with Retry) and the zoom label are handled with Coil's listener.

## 9. Video: VideoView and MediaController

`VideoView` = a `MediaPlayer` + a `SurfaceView` to draw on. `MediaController` = the floating
play/pause/seek bar. `VideoView` keeps the video's aspect ratio itself; `layout_gravity="center"`
centres it on the black stage.

```kotlin
binding.videoView.setMediaController(controller)       // controls appear when the video is tapped
binding.videoView.setOnErrorListener { _, what, extra -> onVideoError(what, extra) }  // returns true
```

Returning `true` from the error listener means “handled”, so Android doesn't show its own dialog;
we show a friendly message and a Retry button instead.

Lifecycle:
- **onStart** → open the video (`setVideoURI`). When prepared: seek to the saved position, and
  resume if it was playing before a rotation.
- **onPause** → save the position. (When the app goes to the background, the video's surface can be
  destroyed — and VideoView releases its player — *before* `onStop()`, when the position would read 0.)
- **onStop** → pause, so nothing plays behind another screen.
- **onDestroyView** → `mediaController.hide()` (its window must not outlive the Activity) and
  `stopPlayback()` to free the decoder.

`PlaybackAwareVideoView` is a VideoView that runs a callback in `start()`, so even the
MediaController's own Play button tells the PlaybackCoordinator first.

## 10. Audio: MediaPlayer states, coordination and cleanup

```
Idle ─setDataSource─► Initialized ─prepareAsync─► Preparing ─► Prepared
Prepared/Paused ─start─► Started ─pause─► Paused
Started/Paused ─stop─► Stopped ─prepareAsync─► Prepared      (must prepare again after stop)
end of file ─► PlaybackCompleted          release() ─► End
```

```kotlin
mediaPlayer.setAudioAttributes(AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
resources.openRawResourceFd(R.raw.sample_audio).use { mediaPlayer.setDataSource(it) }
mediaPlayer.prepareAsync()          // never blocks the main thread
```

- The player lives in `AudioPlayerViewModel`, so **rotation doesn't restart the music**.
- The progress bar is updated by a coroutine in `viewModelScope` (4× per second, only while playing).
- `AudioFragment.onStop()` pauses — unless `isChangingConfigurations` (a rotation).
- `onCleared()` calls `release()` when the screen is gone for good.

**Why a coordinator?** NavigationUI keeps the state (and ViewModels) of drawer screens you left, so
the audio player still exists while you're on the Video screen. `media/PlaybackCoordinator.kt`
remembers which player is active; when one starts, the other is paused:

```kotlin
fun onPlaybackStarted(owner: PlaybackOwner) {
    val previous = activeOwner
    activeOwner = owner
    if (previous != null && previous !== owner) previous.pausePlayback()
}
```

## 11. Edge-to-edge, footer and branding

`enableEdgeToEdge()` lets the app draw behind the status and navigation bars.
`ui/WindowInsetsExtensions.kt` then adds the bar sizes as padding: the toolbar gets the top inset,
the content gets the bottom inset (and the keyboard height on Activity 2), so nothing hides
behind the bars — including the footer.

The **“Developed by AB IT”** footer is one layout (`res/layout/view_footer_watermark.xml`) that
every screen `<include>`s *after* its scrolling content in a vertical layout, so it always sits at
the bottom and can never cover buttons or media controls.

The launcher icon is generated from the supplied logo by `branding/generate_launcher_icons.sh`
(adaptive foreground + navy background, monochrome layer, legacy icons, Play Store icon).

## 12. Tests

- **Unit tests** (`app/src/test`, run on the computer): battery percentage and status, broadcast
  mode parsing, broadcast names, `mm:ss` formatting, PlaybackCoordinator — 22 tests.
- **Espresso tests** (`app/src/androidTest`, run on the emulator): drawer items, header, the four
  destinations, Back; the full “Hello CSE489” broadcast flow; blank input; battery flow; pinch,
  double-tap and pan on the image (real two-finger touch events) — 11 tests.

## 13. Likely viva questions

**Why Fragments for the drawer but Activities for the broadcast flow?** The drawer switches parts of
one screen, which is what Fragments + Navigation are for; the assignment explicitly asks for separate
Activities for the broadcast flow.

**Manifest vs. context-registered receiver?** Manifest receivers are declared in XML and can start
the app, but most implicit broadcasts no longer reach them (Android 8+). Context-registered
receivers live only while registered — here, only while the screen is visible.

**What does `RECEIVER_NOT_EXPORTED` do?** Other apps can't send to the receiver; the system still can.

**What is a sticky broadcast?** One the system keeps after sending; registering returns the latest
copy immediately. `ACTION_BATTERY_CHANGED` is one.

**Why not send the broadcast from Activity 2?** It could arrive before Activity 3 has registered its
receiver, and would be lost.

**How is the duplicate broadcast on rotation prevented?** A flag in the ViewModel's
`SavedStateHandle` records that the first broadcast was sent.

**What happens to the MediaPlayer on rotation?** Nothing — it lives in the ViewModel. It is released
in `onCleared()`.

**Why `prepareAsync()` and not `prepare()`?** `prepare()` blocks the main thread and can freeze the UI.

**How does pinch zoom keep the point under the fingers still?** It scales around the gesture's focus
point: `postScale(factor, factor, focusX, focusY)`.

**What does `AppBarConfiguration` decide?** Which destinations are top-level (hamburger icon) and
which show an Up arrow.
