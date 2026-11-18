# MM Tube 0.1.32 remote fullscreen repair

Baseline: canonical main 6ee84c8, 0.1.31-background-session. VersionCode 33, unchanged package/signature.

## Reproduction and fix

The phone fullscreen path previously gave all matching YouTube player containers fixed viewport dimensions and the same high z-index. An unrelated later container could cover the actual video with a black rectangle while playback continued. It also briefly marked the WebView invisible and hid the native toolbar before a guarded JavaScript result arrived.

A new native WebView regression on the old signed harness failed: the viewport center hit a DIV rather than VIDEO while the media remained playing. An adb screenshot confirmed a black car panel. The synthetic page includes a second black YouTube-style container plus a transformed/clipped ancestor and inline video dimensions. This reproduces a concrete defect; it does not establish the exact DOM on the user's Samsung device.

The replacement promotes only the observed video and its ancestor path, neutralizes clipping/containing blocks, and hides unrelated page content during remote fullscreen. It does not move, clone, reload, seek or restart the video. Removing scoped style/markers restores the page without overwriting inline styles. Document-start lease and execution-time ownership guards remain intact.

Android keeps the WebView surface visible throughout. It hides chrome only after a positive layout acknowledgement. A generation and bounded acknowledgement timeout prevent late callbacks from hiding chrome after exit, Stop, replacement, navigation or disconnect. Native WebView fullscreen clears remote fullscreen before taking over. Diagnostics now include remote fullscreen in the existing fullscreen flag, without logging content identifiers or lease tokens.

## Validation

- 93 Java/Robolectric tests passed: 5 new fullscreen tests cover failed/missing acknowledgement, late callback expiry, successful entry/exit visibility, rapid double taps, Stop/disconnect/replacement and navigation cleanup.
- 7 JavaScript suites passed, including rejection of stale/revoked remote-fullscreen document ownership.
- Android 16/API 36 emulator, WebView 133.0.6943.137: 37 synthetic integration checks passed: 27 existing native controller scenarios, 1 native fullscreen-cover scenario, 9 new remote-fullscreen scenarios. New checks assert viewport geometry and topmost video, unchanged media element/parent/document with advancing time, actual native Play/Pause enablement, cover/return, original style restoration, safe rejection of a hidden video, rapid toggling, replacement and Stop returning Home.
- adb screenshots inspected before/after: black car panel on the old implementation; visible moving test-pattern video after the repair. Aspect ratio is preserved with letterboxing.
- Release/debug builds and release Lint passed: 0 errors, 27 warnings.
- Native tests use synthetic media and injected page layouts, not real YouTube playback or physical Android Auto. They do not prove audible sound or Samsung/WebView 157 compatibility.

APK: MM-Tube-0.1.32.apk; package com.magdi.tube; versionCode=33.
SHA-256: 77c2d452228f80f7ecf16fcc9e47c9d34a129bd1ef7cd7fd01821326430508d8.
Certificate SHA-256: 6d9d30d8f427a6cfc0241831d1e324e684fe2aefda72925c89ef872bf3a5ab65.

## Device check

Update over the existing app. Open the car player and select a video from the phone. Tap Car fullscreen, verify the picture and continued audio, then exit and repeat. Test Pause/Play, a different video, and covering/returning to the car view. Stop must return Home; actual disconnection/reconnection must not autoplay. If the picture is still black, copy/export the new Car session to identify the host/WebView-specific behavior.

Signed 0.1.31 -> 0.1.32 install-r upgrade passed on the emulator without uninstalling; the existing private app-data sentinel survived. aapt/dumpsys confirmed package and code 33.
