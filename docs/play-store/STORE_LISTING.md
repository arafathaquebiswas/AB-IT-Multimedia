# Google Play store listing — draft

Draft text and assets for the Play Console **Main store listing**. Review and edit before use.
Limits are from Google Play's store listing help page (checked 26 October 2026).

## Text

| Field | Value | Limit |
|---|---|---|
| App name | AB IT Multimedia | 30 characters |
| Short description | Broadcast receivers, pinch-to-zoom photos, video and audio in one demo app. | 80 characters |
| Full description | see below | 4000 characters |

### Full description

```
AB IT Multimedia is a small, ad-free demo of core Android features, built by AB IT.

FOUR FEATURES, ONE DRAWER
• Broadcast Receiver – choose a broadcast type and follow it across three screens:
  – Custom: type a message and watch it arrive at a receiver through a private broadcast.
  – Battery: see the live battery level, charging status and update time from Android's
    system battery broadcast.
• Image Scale – a high-resolution photo you can pinch to zoom (up to 5x), drag and double-tap.
• Video – watch a short film trailer with standard playback controls.
• Audio – play a music track with play/pause, stop and a seek bar.

PRIVATE BY DESIGN
• No accounts, ads, analytics or tracking.
• The only permission is internet access, used to download the Image Scale photo.
• Video and music are built into the app and work offline.

CREDITS
• Video: "Sintel" trailer © Blender Foundation | durian.blender.org (CC BY 3.0)
• Music: "Carefree" by Kevin MacLeod (incompetech.com), licensed under Creative Commons:
  By Attribution 4.0 License
• Photo: Alexey Topolyanskiy on Unsplash, served by Lorem Picsum
• The Android robot is reproduced or modified from work created and shared by Google and used
  according to terms described in the Creative Commons 3.0 Attribution License.
```

## Graphics in this folder

| Asset | File | Requirement | Status |
|---|---|---|---|
| App icon | `icon-512.png` | 512 × 512, 32-bit PNG with alpha, ≤ 1024 KB | generated from the supplied logo |
| Feature graphic | `feature-graphic.png` | 1024 × 500, JPEG or 24-bit PNG without alpha | generated from the supplied logo |
| Phone screenshots | `screenshots/*.png` | 2–8 images, 24-bit PNG/JPEG without alpha, sides 320–3840 px, long side ≤ 2 × short side | 8 images, 1101 × 2202 (2:1) |

The phone screenshots are the emulator screenshots with the status bar and gesture bar trimmed,
because the raw 1080 × 2400 images are longer than 2:1 and would be rejected.

## Suggested settings

- **Category:** Education or Tools — your choice; both fit a feature-demo app.
- **Contact email:** required by Play Console. Use an address you check; it is also the
  privacy contact named in the privacy policy.
- **Website / phone:** optional — leave empty rather than inventing one.
