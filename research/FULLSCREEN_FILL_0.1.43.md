# Fullscreen picture fit — 0.1.43

Enter Car fullscreen, then choose **Fill screen / بدون حواف**. Choose **Full picture / الصورة كاملة** to restore the full frame. Fill uses centered aspect-preserving cropping, not stretching. The normal player remains unchanged. Android Auto's system navigation area is outside the app; black bars encoded into the video are not detected or removed automatically.

The phone button requires fresh observed fullscreen playback state. Commands retain connection, request, player and document ownership checks. The car has a corresponding button. Switching modes only changes the current video element's object-fit/object-position styles; exit restores its previous inline styles and priorities. No source changes, seeking, playback commands or audio-focus changes are part of fitting. The phone remains a silent controller.

Validation on 2026-11-22:

- Release assembly, Android lint and 138 Robolectric/JVM tests passed. Native phone-button regression covers observed state, disabling outside fullscreen, stale requests and disconnection. Layout checks include the new button at narrow and wide sizes.
- Seven JavaScript regression suites passed. The existing Android WebView controller regression also passed, covering native fullscreen status, play/pause, rapid selections, stale reports, Stop, disconnect/reconnect, phone closure and no phone playback.
- `tube/tests/fullscreen-fill-native.mjs` passed in API 36 emulator Android WebViews with synthetic video: actual phone button clicks, cover/contain at 960×540, 1280×480 and 480×800, unchanged media element/source/mute, pause position, advancing decoded frames, car-local toggle, native WebView fullscreen, exit, Stop and no phone playback.
- Set `TEST_APP=tube`, launch the debug `ControllerHarnessActivity`, and forward its WebView debugger to 9226 to repeat that test. The harness is excluded from the release.
- Existing package ID and signing certificate are retained; no account or fixture assets are packaged in the release.

The new Fit/Fill option has not been tested with live YouTube or a physical Android Auto head unit in this release. Synthetic emulator success does not establish those results. Prior density/viewport fixes remain included.
