# MM Tube

Android Auto YouTube player with a phone browsing and remote-control interface. Package: `com.magdi.tube`.

## Install and use

Download the APK from [GitHub Releases](https://github.com/moustafamagdi/MagdiTube/releases). Install an update signed with the same certificate over the existing app to keep your settings and account session.

From **0.1.25**, the phone is a controller only: browse, search and sign in there, then select a video for the available car player. The native panel shows connection, actual video title, state and progress, with play/pause, stop, seek, supported next/previous, retry and car fullscreen. Selections without an available car are rejected immediately and never queued.

Open MM Tube on Android Auto before selecting a video. Leaving/closing its car interface or disconnecting stops playback and cancels pending requests. Reopening does not resume automatically; make a fresh selection. Closing the phone interface leaves an available car player running.

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
