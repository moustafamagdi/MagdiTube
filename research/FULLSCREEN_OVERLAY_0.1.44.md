# MM Tube 0.1.44 — Quiet fullscreen controls

The car's Fit/Fill text button is now a translucent circular image button, matching the Exit control. Outward arrows offer Fill screen; inward arrows restore the full picture. Arabic and English accessibility descriptions follow the observed mode. Phone labels remain unchanged.

Both app-owned controls disappear after five seconds without touch interaction in remote/CSS fullscreen and native WebView video fullscreen. One touch reveals them without forwarding that gesture to the player or activating a formerly hidden button. A second touch operates a visible button normally. A held gesture delays hiding until five seconds after release/cancellation. Keyboard-focused controls and touch exploration remain accessible. Status samples and changes sent from the phone do not reveal the overlays. YouTube's own controls retain their website/native behavior.

The visibility controller neither invokes media commands nor changes playback state. Exit, Stop, fullscreen replacement and view destruction remove its pending timer. Each mode uses the existing guarded Fit/Fill command, preserving player/document/request ownership and the silent phone-controller model.

Validation, 2026-11-22:

- Release build and lint passed; 143 JVM/Robolectric tests passed, including five new cases for the deadline, first-touch capture, hold/cancel, automatic versus keyboard focus, replacement and cleanup.
- Seven JavaScript regression suites passed.
- `tube/tests/fullscreen-overlay-native.mjs` passed nine scenarios on an API 36 emulator with real Android WebViews, synthetic video and Android `input tap` events. Both fullscreen modes were tested for image-button geometry, five-second hiding, first-tap reveal, icon state, frame progression, phone changes while hidden, two-step Exit, preserved video/source/playback, and Stop cleanup/no local phone playback. Screenshots of visible and hidden controls were inspected.
- Existing package and release signing certificate retained. No embedded account or debug harness in the release APK.

Repeat the native test with the debug ControllerHarnessActivity, CDP port 9226, `ADB`, `ADB_PORT` and `ADB_DEVICE` set for an isolated emulator. A large emulator display (2560×1400, density 160) fits both test panels. Optional `SCREENSHOT_DIR` saves Android screenshots.

This overlay update has not been tested on a physical Android Auto head unit or live YouTube in this release. The user's physical-car confirmation of the preceding Fit/Fill release does not substitute for testing this new timeout/input behavior.
