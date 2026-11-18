# MM Tube 0.1.34 unified fullscreen exit

Baseline main e27b004, version 0.1.33-player-controls. VersionCode 35.

Remote fullscreen previously created a rectangular text Exit button at top-left. Native YouTube/WebView fullscreen created the existing translucent circular close icon at left-center. Both now call one addFullscreenExit helper using the unchanged ic_fullscreen_close and fullscreen_close_background resources, 36 dp circular artwork inside a 48 dp square touch target and 2 dp physical-left margin. Each retains its own correct exit callback. No YouTube JavaScript, media handling, ownership or session behavior changed.

Validation:
- 93 existing Java/Robolectric tests passed. The remote fullscreen restoration test now presses the actual exit view instead of invoking Back.
- Release and debug builds passed. Release Lint: 0 errors, 26 warnings.
- Three focused Android 16/API 36 emulator checks passed using actual native taps and synthetic video: remote exit preserves the playing document; both modes expose one ImageButton with identical square screen bounds [363,639,435,711]; native exit preserves the playing document. Before/after screenshots for both modes were visually inspected and show the same circular close icon at the left-center edge.
- uiautomator returned status 137 after successfully writing its hierarchy. The verification accepted only its explicit dump-success message plus a freshly created readable XML file, then validated the button and tapped its measured bounds.
- Signed 0.1.33 -> 0.1.34 install-r upgrade passed without uninstalling, preserving the existing private app-data sentinel.
- This release's focused UI checks used synthetic emulator playback, not a new live YouTube or physical Android Auto test. The previous 0.1.33 live YouTube validation is documented separately; it is not counted as a test of this build.

APK: MM-Tube-0.1.34.apk, com.magdi.tube, versionCode=35.
SHA-256: 9f1bc1864c37cd14610a9f0ca1aae3b1394b99823d503d2dd5a25eaeb9c0e6b2.
Certificate SHA-256: 6d9d30d8f427a6cfc0241831d1e324e684fe2aefda72925c89ef872bf3a5ab65.

Device check: update without uninstalling. Enter fullscreen from the phone and then from YouTube on the car; confirm the same circular exit at left-center and that pressing it returns without restarting the video.
