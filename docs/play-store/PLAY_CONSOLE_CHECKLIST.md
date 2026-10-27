# Google Play Console checklist — AB IT Multimedia

What is already prepared in this project, and what you still have to do yourself in Play Console.
Requirements were checked against the official Google Play / Android Developers pages on
26 October 2026 (sources at the end). **Nothing has been uploaded or published.**

## 1. Technical requirements (verified in this project)

| Requirement | Official rule | This app | Status |
|---|---|---|---|
| Target API level | New apps and updates must target Android 16 (API 36) or higher from 31 Aug 2026 | `targetSdk 37` | ✅ |
| Minimum Android version | Your choice | `minSdk 24` (Android 7.0) | ✅ |
| Publishing format | Android App Bundle (AAB) with Play App Signing | `app/build/outputs/bundle/release/app-release.aab`, signed with the upload key | ✅ |
| 16 KB memory page size | Required for apps targeting API 35+ (updates blocked from 1 Feb 2027); Kotlin/Java-only apps already comply | no native `.so` libraries in the AAB | ✅ |
| Permissions | Only what is needed | `INTERNET` only (+ AndroidX's own signature-level permission) | ✅ |
| Exported components | Must be deliberate | only the launcher Activity, plus AndroidX's `ProfileInstallReceiver` (guarded by the system-only `DUMP` permission) | ✅ |
| Runtime receivers (Android 14+) | Must declare an export flag | both use `ContextCompat.RECEIVER_NOT_EXPORTED` | ✅ |
| Release build | Not debuggable, shrunk | R8 enabled, not debuggable, cleartext traffic not allowed | ✅ |

## 2. Before creating the app in Play Console

1. **Developer account.** If you use a *personal* account created after 13 November 2023, you must
   run a **closed test with at least 12 testers who stay opted in for 14 continuous days** before
   you can apply for production access (Dashboard → *Apply for production*). Plan for this:
   collect 12+ Google accounts (classmates, family) willing to install the app and stay opted in.
   Organisation accounts are not subject to this rule.
2. **Developer verification.** Google has registered almost all Play apps automatically. Check the
   Play Console home page for any app registration task; the first enforcement date was
   30 September 2026 (Brazil, Indonesia, Singapore, Thailand), with a global rollout in 2027.
3. **Privacy policy URL.** Play requires a public, non-PDF, non-editable URL. Host
   `docs/privacy-policy.md` — for example with GitHub Pages (repository → Settings → Pages →
   deploy from the `docs` folder) or Google Sites — and note the URL. The same policy is already
   shown inside the app (About screen), which Play also requires.

## 3. Create the app and upload

1. *Create app* → name **AB IT Multimedia**, default language, **App** (not game), **Free**.
2. Enroll in **Play App Signing** (default for new apps). Google keeps the final app signing key;
   you sign uploads with your **upload key** (see README → *Release signing*).
3. Upload `app-release.aab` to the **Internal testing** track first, install it from Play on a phone
   and check every screen.
4. Then create the **Closed testing** track, add your 12+ testers, and keep it running for 14 days.

## 4. App content declarations (Policy → App content)

Answer from the facts below. These are recommendations based on the code, not legal advice — you
are responsible for the final answers.

| Declaration | Recommended answer | Evidence |
|---|---|---|
| Privacy policy | your hosted URL from step 2.3 | `docs/privacy-policy.md`, About screen |
| Ads | **No ads** | no ad SDKs in `gradle/libs.versions.toml` |
| App access | **All functionality is available without special access** | no login or accounts; Image Scale needs internet |
| Content rating | Fill in the IARC questionnaire truthfully (no violence, sexual content, gambling, user interaction or purchases). The rating is assigned by IARC. | — |
| Target audience | **13 and over** (or 18+). Do *not* pick under-13 age groups unless you want the Families policy to apply. | app is a technical demo, not designed for children |
| News app / Government app / Financial features / Health | **No** / **No** / **None** / **None** | — |
| Data safety | see below | — |

### Data safety (what the code actually does)

- The app has **no** accounts, analytics, crash reporting, advertising or tracking SDKs.
  Dependencies: AndroidX, Material, Navigation, Lifecycle, Coil + OkHttp (image loading only).
- User-entered text (the custom broadcast message) and the battery level **never leave the device**.
- The **only** network request is an HTTPS GET for one photo from `picsum.photos`
  (`image/ImageScaleFragment.kt`). It contains no identifiers or user data; like any web request it
  exposes the device's IP address to Lorem Picsum's servers. AB IT never receives it.

**Recommended answer:** “Does your app collect or share any of the required user data types?” →
**No**. Google defines “collected” as data transmitted off the device by your app; the app transmits
no user data of its own, and the IP address is only seen by the image server as part of the
download. If you prefer to be maximally cautious, you can review Google's guidance on IP addresses
in the Data safety help page before answering. Google requires the form (and a privacy policy) even
when an app collects nothing.

## 5. Store listing

Use `docs/play-store/STORE_LISTING.md` and the graphics in `docs/play-store/`.

## 6. Branding and content rights

| Item | Licence / rule | Where it is credited |
|---|---|---|
| Sintel trailer (video) | CC BY 3.0, © Blender Foundation | About screen, README, store description |
| “Carefree” by Kevin MacLeod (audio) | CC BY 4.0 | Audio screen, About, README, store description |
| Photo 1015 (Picsum/Unsplash) | Unsplash licence (free use) | About, README, store description |
| Android robot in the AB IT logo | Google brand guidelines: may be used/modified **with the attribution line** below | About screen, README, store description |
| Libraries | Apache 2.0 (AndroidX, Material, Coil, OkHttp) | — |

Android robot attribution (required wherever the robot appears in a creative):
“The Android robot is reproduced or modified from work created and shared by Google and used
according to terms described in the Creative Commons 3.0 Attribution License.”

Google's brand guidelines also say marketing creatives that include Android trademarks should be
submitted to the Android brand team for review, and that “Android” must not be used in an app's
name (the name “AB IT Multimedia” is fine). The feature graphic in this folder contains the logo, and
therefore the robot; it carries the attribution line in small print.

## 7. Risks to be aware of

- **Minimum functionality:** Google may reject apps that offer very limited functionality. This is a
  learning/demo app, so a rejection on those grounds is possible.
- **Third-party image service:** Image Scale depends on picsum.photos being online.
- **Closed testing:** production access is impossible without the 12-tester / 14-day closed test on a
  new personal account.

## Sources

- Target API level requirements — https://developer.android.com/google/play/requirements/target-sdk
- Target API level policy (Play Console Help) — https://support.google.com/googleplay/android-developer/answer/11926878
- Testing requirements for new personal accounts — https://support.google.com/googleplay/android-developer/answer/14151465
- Data safety form — https://support.google.com/googleplay/android-developer/answer/10787469
- User Data policy (privacy policy rules) — https://support.google.com/googleplay/android-developer/answer/10144311
- Store listing assets — https://support.google.com/googleplay/android-developer/answer/9866151
- 16 KB page sizes — https://developer.android.com/guide/practices/page-sizes
- Android brand guidelines — https://developer.android.com/distribute/marketing-tools/brand-guidelines
- Android developer verification — https://android-developers.googleblog.com/2026/03/android-developer-verification-rolling-out-to-all-developers.html
