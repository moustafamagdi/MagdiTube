# MM Tube 0.1.29 controller hardening

Baseline: canonical MagdiTube v0.1.28-control-buttons (ac97830); reviewed canonical MagdiIPTV v0.1.12-control-buttons CarController, PlayerView and controller.js. Older workspace copies were not modified.

## Regression evidence

All eight initial tests failed on 0.1.28: rejected-document side effects, late cover after Pause, stale native buttons, commands from an outdated phone selection, repeated loading taps resetting the request, buffering classification, media-error controls and ended/replay controls.

Rejected reports now exit before all consumers. A current leased document with an unready selected player publishes neutral loading state. Transport expires after 6500 ms without a valid report; phone controls repaint every 500 ms. Commands/gestures carry connection, request and observed owner. JavaScript additionally checks document-start lease, document identity, video identity and intent generation at execution. Stop is explicitly connection-wide but cannot cross reconnection. Duplicate loading taps and page-finished callbacks cannot repeatedly restart a selection. No IPTV local-player request allocation was copied.

## Validation

- 76 Java/Robolectric tests passed, including 14 new hardening tests. These are JVM tests, not emulator tests. They cover actual native buttons, seek ownership, rejected-sample consumers, freshness and late cover after Pause/Stop/replacement/disconnect.
- 7 Node.js helper suites passed, including execution-time identity, empty-token rejection, intent generation, buffering, media errors, replay and duplicate-start regressions.
- Signed release/debug builds passed. Release Lint: 0 errors, 27 warnings.
- Android 16 / API 36 emulator, WebView 133.0.6943.137: 22 native integration scenarios passed using production phone/car TubeViews and synthetic MP4 decoding. Covered native phone CSP/mute/fresh-iframe blocking, cookie attributes/deletion, controls and native fullscreen progress, supported/unsupported transport, injected buffering/error/ended, rejected-report expiry/recovery, rapid A/B/A, repeated loading taps, Stop, loading disconnect, reconnect without autoplay and phone closure.
- An initial extended run encountered a synthetic fixture read error; the app disabled transport correctly. The fixture now handles EOF ranges and loops its short clip. Stale-status testing pauses it first to avoid natural completion changing the expected button state. The full revised suite passed.
- Signed upgrade installed release 0.1.28/code 29 then 0.1.29/code 30 on the emulator with install -r and no uninstall. Existing app-data sentinel stayed unchanged.
- Certificate SHA-256: `6d9d30d8f427a6cfc0241831d1e324e684fe2aefda72925c89ef872bf3a5ab65`.
- APK `MM-Tube-0.1.29.apk`, package `com.magdi.tube`, code 30. SHA-256: `0a7189061bcade4830057a541d90047b7cebd453436416e2b33e4935827e4e21`.

## Limits and device check

No real YouTube playback or physical Android Auto run was performed for this change. Emulator results do not establish Samsung Android 17/WebView 157 compatibility, car sound/routing, authentication or projected rendering. No software was installed on the attached physical phone.

Update without clearing data. With the car parked, open MM Tube on Android Auto; select a video from the phone and check Play/Pause dimming, seek and fullscreen progress. Try rapid A/B/A and repeated loading taps. Pause before covering/restoring the player. Disconnect while loading; reconnect must not autoplay. Closing the phone must leave available car playback running. Copy a fresh log session if behavior differs.

## Published artifact verification

The stable GitHub release is v0.1.29-controller-hardening. Downloading its APK reproduced the local SHA-256 above; apksigner verified the downloaded file against the existing signing certificate. GitHub asset name is MM-Tube-0.1.29.apk and its actual asset label is versionCode=30 (not only release-note text).

The release metadata workflow independently downloaded the APK, checked GitHub's digest, verified the certificate/package and read versionCode from its manifest before applying the label. [Successful verification run](https://github.com/moustafamagdi/MagdiTube/actions/runs/44282801767).

An additional passing Robolectric test compiled the existing MM Auto Releases.java source unchanged against the live releases/latest JSON: it selects 0.1.29/code 30, accepts upgrading code 29 or a missing installation, rejects an already-installed code 30 and a newer code 31, and checks the exact asset URL and digest. This verifies MM Auto's selection logic; it is not a physical-phone UI installation test.
