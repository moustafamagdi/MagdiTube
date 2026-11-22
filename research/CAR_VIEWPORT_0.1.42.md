# Car viewport correction — 0.1.42

The Samsung/DHU report showed a full-width native toolbar with YouTube confined to the top-left corner. The native WebView measured 800 x 346, but page telemetry reported 284 x 123. Reload did not correct this.

The car-only WebView now sets its initial physical scale from the car context's density. Android's `setInitialScale` deliberately does not account for screen density automatically: https://developer.android.com/reference/android/webkit/WebView#setInitialScale(int).

Testing the first scaling change exposed a second failure: reusing a disconnected car view could restore the incorrect viewport after reconnection. A genuine car-service disconnect now marks the view for replacement at the next attachment. Replacement removes and closes the obsolete page before constructing the next one. A temporary host cover/recreation in the same connection continues to retain the current document. A late disconnect from an obsolete service cannot invalidate the current connection. Reconnection opens Home and does not restore playback automatically.

## Verification on 2026-11-22

- 137 JVM/Robolectric tests passed, including three new regressions: host density differing from phone density at 160/240/320 dpi; no scale override on the phone; temporary cover retention versus true disconnect replacement and obsolete service callback rejection.
- Signed release build and release Lint passed: 0 errors, 36 existing warnings.
- Upgraded the connected Samsung SM-S948B in place, with the same signing certificate, to 0.1.42-car-viewport (versionCode 43). No uninstall or data clearing.
- Real Samsung Android 17 / Android Auto 18 / WebView 157 projected to Windows DHU 2.1 at 800 x 480: real YouTube Home filled the content area after phone-first initialization. Phone viewport remained 384 CSS pixels for its 1080-pixel view, while the car reported 800 x 346 for its 800 x 346 WebView.
- Closed the DHU connection and reconnected, opened Tube from the Android Auto taskbar: the diagnostic `Car display recreated after disconnection` was followed by an 800 x 346 page viewport. Visual inspection confirmed full-width YouTube Home again. No media autoplay occurred in this Home-page scenario.

This is physical-phone plus desktop-projection testing, not a physical vehicle head unit test. No real-video playback, fullscreen video transition, audible-output, active-media camera-cover or long-duration stability test was completed in this short phone session. Existing controller/fullscreen JVM tests passed; those are not substitutes for device playback checks.

Release APK: MM-Tube-0.1.42.apk

SHA-256: f847f9b2f534232ee7fee5e8467d0375196135e604647bc5134b76203101d4fe

Signer SHA-256: 6d9d30d8f427a6cfc0241831d1e324e684fe2aefda72925c89ef872bf3a5ab65
