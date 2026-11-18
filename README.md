# MM Tube

Android Auto YouTube player with a phone browsing and remote-control interface. Package: `com.magdi.tube`.

## Install and use

Download the APK from [GitHub Releases](https://github.com/moustafamagdi/MagdiTube/releases). Install an update signed with the same certificate over the existing app to keep your settings and account session.

From **0.1.25**, the phone is a controller only: browse, search and sign in there, then select a video for the available car player. The native panel shows connection, actual video title, state and progress, with play/pause, stop, seek, supported next/previous, retry and car fullscreen. Selections without an available car are rejected immediately and never queued.

Open MM Tube on Android Auto before selecting a video. From **0.1.31**, temporarily covering/leaving the car view retains its playback session while the car service remains bound (with Background audio enabled). Returning restores the same document and fullscreen state. Service unbind/disconnection still stops playback and cancels pending requests; reconnecting never autoplays. Closing the phone interface leaves an available car player running.

From **0.1.33**, the phone's **Car fullscreen** action expands the active YouTube player including its own controls, without moving or reloading the video. Touch the picture to reveal YouTube's controls. Only the selected player's ancestor path is expanded; unrelated containers stay outside the fullscreen display. Unknown layouts use reversible browser video controls. This remains a remote page-layout mode, distinct from YouTube's native fullscreen action, which can require a gesture inside the car document.

The phone uses a separate WebView profile, native renderer mute, native media-request rejection and Android-injected HTML response CSP (including data/blob media). Service-worker network access is blocked in that profile. Unsupported WebView features disable phone browsing and request a WebView update. The car remains the only playback and audio-service target.

Existing YouTube cookies migrate into the phone profile once; selections synchronize YouTube cookies and deletions back to the car via native APIs, preserving cookie attributes. Google login cookies remain in the phone profile. Cookie synchronization is **not** a claim that car authentication or playback succeeded: status comes from the actual car player. Google may reject embedded sign-in. No passwords, cookie values or session tokens are logged.

Ad filtering remains experimental. The embedded car keyboard, King Installer setup, separate audio icon and diagnostic copy/export are preserved.

## Android Auto

Projection uses a legacy integration and compatibility varies with the host and device. The car interface includes an Arabic/English search keyboard. The separate **MM Tube Audio** entry controls existing playback and shows video information and available playback actions.

On the phone, **Log sessions** lets you copy or export a selected recording part, or start a new session before testing. New app processes and car connections start sessions; car reconnections within five minutes share the same session. Long sessions split into 128 KiB parts. The app keeps the latest 40 parts for up to seven days, removing older parts during recording or when opening the list. Saved exports outside the app are unaffected.

If the screen becomes stuck or unusually small, hold **Reload** to rebuild the view and return to YouTube Home without clearing your account cookies. Fullscreen uses an overlay that preserves the underlying page's measured size; diagnostics record native and page dimensions to investigate device-specific failures. This recovery closes current playback; open the video again.

Regional Google account redirects are supported using an exact list of official Google domains; sign-in remains phone-only.

## Build and test

Use JDK 17 and the Android SDK with API 36 and Build Tools 36.0.0. Set `ANDROID_HOME` or configure `sdk.dir` in `local.properties`, then run:

```sh
./gradlew :tube:assembleDebug :tube:testDebugUnitTest
```

On Windows, use `gradlew.bat`. For an existing signed release, set `MAGDI_KEYSTORE` to the PKCS12 key file and `MAGDI_STORE_PASSWORD` to its password, then run `./gradlew :tube:assembleRelease :tube:lintRelease`. Keep signing files outside the repository.

The JavaScript suites in `tube/tests/` can be run individually with Node.js. See [third-party notices](THIRD_PARTY.md) for dependency sources.

## Controller validation (0.1.25)

Validated on an Android 36 emulator with WebView 133.0.6943.137, using two production TubeViews and synthetic media: 15 native integration scenarios covering routing, actual decoded frames, no local playback, native network/data/blob blocking without JS media overrides, renderer mute, cookie attributes/deletions, state/title, controls, supported/unsupported next/previous, fullscreen, last-selection wins, stop, disconnect during loading, reconnect without autoplay and phone closure. Anonymous live YouTube home/search were also checked separately. The debug-only fixture and interface are absent from the release APK.

Run `ControllerHarnessActivity` in a debug APK, forward `localabstract:webview_devtools_remote_<pid>` to TCP 9226, then run `node tube/tests/controller-native.mjs`. The fixture is a generated test pattern and tone, not downloaded video. 59 passing Java unit tests use Robolectric (not an emulator); the `.cjs` tests exercise the JavaScript helpers.

Physical-car acceptance, actual sound/routing, projected fullscreen sizing, interruptions/reconnect behavior on the Samsung device, and real Google account sign-in still require device testing. The emulator does not establish Android Auto host compatibility or audible sound in the car.

A signed release upgrade from 0.1.24 to 0.1.25 was installed successfully on the emulator without uninstalling the old release; an existing app-data file remained byte-for-byte unchanged. The release certificate matches the existing application.

## Controller state recovery (0.1.27)

Car documents now receive their request binding from Android at document start, instead of a URL fragment that YouTube can rewrite before the playback helper runs. The first report must match the selected player; older document/connection reports are rejected, including after Stop. Rejected or missing sample callbacks no longer leave polling permanently occupied. Diagnostic events report acceptance/rejection reasons without video identities, titles, URLs or request tokens.

Validation: the new JavaScript regression fails on the old fragment-based helper and passes with native document binding. 62 Robolectric tests, eight JavaScript suites and release Lint pass. The Android 36 / WebView 133 emulator passes 17 native scenarios, including fragment-free navigation, actual phone-button enablement, native WebView video fullscreen with advancing progress and working Play/Pause, rapid selection, Stop, disconnect during loading, reconnect without autoplay, and native phone media blocking. The test player is synthetic; this does not establish the fix on Samsung WebView 157 / Android Auto. That final reproduction needs the same device/host session from the report.

## State-aware buttons (0.1.28)

The phone disables/dims Play while the car is playing and disables/dims Pause while it is paused. Button state follows observed car playback, including changes made on the car; it does not assume a command succeeded. Existing readiness/disconnection guards remain in place. The native integration assertions cover both transitions, including paused state in native video fullscreen.

## Controller hardening (0.1.29)

Rejected observations now stop before reaching resume intent, the audio card or playback services. Current documents with an unready selection publish neutral loading state. Android document-start leases remain in place, with an additional document identity for commands executed by Chromium. Missing or rejected reports expire transport controls after 6.5 seconds (plus the phone's 500 ms repaint interval).

Controls distinguish playing, paused, buffering, ended and media errors. Play/Pause are visibly dimmed when unavailable; replay, seeking and skipping require observed support. Commands and seek gestures bind to the displayed request and observed player. **Stop is connection-wide**, canceling the current selection and pending work even from an older selection display. It cannot cross a reconnection. Commands do not optimistically declare playback success.

Late cover/resume callbacks cannot restore intent after Pause, Stop, replacement or disconnection. Repeated taps on an already-loading selection preserve its start; A/B/A still uses a fresh final request. No IPTV local-player allocation mechanism is used.

Validation and limits: [0.1.29 verification](research/CONTROLLER_HARDENING_0.1.29.md).

## Compact controller and Stop recovery (0.1.30)

Phone controls now reflow into two rows on narrow screens and one row when wide enough, without horizontal transport scrolling. Play/Pause remain separate, observed-state buttons with disabled dimming. Account, Subscriptions, Reload, Ads block, Background audio, Android Auto setup and Log sessions are under **More**. **More > Reset browsing screen** preserves the previous phone long-press Reload reset. Car Back/Home/Reload/Search remain visible; the ad control reads **Ads block: ON/OFF**.

Stop is disabled when there is no selection or observed car media. It cancels a selected/loading/current video and returns the connected car to YouTube Home. A repeated idle Stop is a no-op. Disconnect still cancels and unloads without queuing playback; Stop stays connection-wide and cannot cross reconnection.

Validation details and limits: [0.1.30 test record](research/CONTROLLER_LAYOUT_0.1.30.md).

## Background session recovery (0.1.31)

Car activity onStop is now a temporary cover, not a disconnect. Host recreation can reuse the same player while its car-service binding is alive. Sampling, request/document ownership and media controls stay active while covered; return avoids reloading Home even during an in-flight selection. Explicit Pause/Stop and disconnect invalidate delayed resume work. Service unbind/destroy still cancels playback immediately, with no phone fallback or reconnect queue.

See [0.1.31 validation](research/BACKGROUND_SESSION_0.1.31.md). To reproduce the native fullscreen cover test, run `controller-native.mjs` followed by `controller-cover-fullscreen.mjs` against the same debug harness.
