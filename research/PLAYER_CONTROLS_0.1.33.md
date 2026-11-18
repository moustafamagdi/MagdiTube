# MM Tube 0.1.33: retain player controls in remote fullscreen

Baseline: main fd13aa1, version 0.1.32-controller-fullscreen. New versionCode 34, unchanged com.magdi.tube identity/certificate.

## Change and evidence

0.1.32 deliberately promoted only the video and hid its sibling controls. A new native WebView regression reproduced the problem before the change: a revealed player button still had computed visibility hidden. The fix promotes a known container owning the observed single video, preferring the mobile outer player because controls can be siblings of the inner HTML5 player. Only ancestors of that specific player are normalized. Video wrappers expand inside it; controls keep their own event handlers, visibility and stacking behavior. Unrelated player containers are not promoted, preserving the black-overlay fix.

The video is not moved/replaced/reloaded. Inline styles are not overwritten. Marker/style cleanup restores the original layout. Unknown player layouts get the video element's standard browser controls, restoring the prior controls attribute on exit. Existing execution-time document ownership, acknowledgement timeout, cancellation and native WebView surface handling remain unchanged.

A direct requestFullscreen with no user gesture was rejected in the car WebView. This agrees with the [Fullscreen API's transient activation requirement](https://developer.mozilla.org/en-US/docs/Web/API/Element/requestFullscreen#security_considerations). We do not fabricate car-page gestures in production or claim that this CSS mode becomes the native fullscreen API mode.

## Validation

- 93 Java/Robolectric tests passed; 7 JavaScript suites passed.
- 47 synthetic WebView checks passed on Android 16/API 36 with WebView 133.0.6943.137: 27 general controller, 1 native fullscreen-cover, 9 prior remote-fullscreen and 10 new player-controls checks. New coverage includes no-gesture rejection, visible/hittable mobile controls outside the inner video wrapper, video geometry/no black overlay, car-side Play/Pause/seek feeding phone state, controls' own show/hide behavior, cover/return, exit restoration, desktop layout, reversible unknown-layout controls and disconnect/reconnect.
- The chained general test initially saw the prior scenario's native Stop button before the 500 ms phone UI poll completed. The remote scenario now awaits the observed disabled Stop button before handing off; no production state change was needed. The general suite then passed.
- Actual live YouTube playback was tested through the debug harness's opt-in liveCar mode (production car WebView client and real network/media; synthetic phone browsing selects the public Blender Big Buck Bunny demo). Observed mobile DOM includes player-container-id -> player -> movie_player -> html5-video-container -> video; YouTube's custom controls are in a sibling player-control-container within the outer player. They are lazily populated on touch.
- In live YouTube remote fullscreen, adb taps revealed YouTube's own controls, paused and resumed the actual video, and native phone Play/Pause state followed. Cover/return preserved the document and advancing media; remote exit restored the page without restarting playback. adb screenshots visually checked the video and YouTube Pause/Play controls. No claim of audible sound: emulator audio output is disabled.
- This is not a physical Android Auto test and does not establish Samsung/WebView 157 behavior. Remote fullscreen remains a page-layout mode; it is not guaranteed pixel-identical to YouTube's native fullscreen.
- Release/debug builds and release Lint passed: 0 errors, 27 warnings.
- Signed 0.1.32 -> 0.1.33 install-r upgrade passed without uninstalling; the existing private app-data sentinel survived.

For optional live smoke testing, install the debug harness and launch ControllerHarnessActivity with --ez liveCar true in a fresh task. This opts only the car view out of fixture interception. Phone browsing stays synthetic and native phone playback blocking remains in effect. All normal automated suites use the default intercepted fixture mode.

APK: MM-Tube-0.1.33.apk, versionCode=34.
SHA-256: 7a65212c14fcda818a0d896bc45242b380e3f8d4e64101d00e5870dbf0a3be95.
Certificate SHA-256: 6d9d30d8f427a6cfc0241831d1e324e684fe2aefda72925c89ef872bf3a5ab65.

## Physical check

Update over the installed app. Select a video, use Car fullscreen on the phone and touch the car picture to reveal controls. Try YouTube Pause/Play and seeking, exit/re-enter, then cover/return. Check picture/audio continuity on your actual car. Disconnect/reconnect must not autoplay. Copy the new Car log if controls or the picture do not return.

## Publication verification

Published v0.1.33-player-controls at 193f0ab via [GitHub Actions run 44330369534](https://github.com/moustafamagdi/MagdiTube/actions/runs/44330369534). The isolated upload branch verified the signed APK, source tag, package/code/certificate and downloaded hash with GitHub's built-in token. No local signing/authentication secrets were uploaded; the temporary remote branch/worktree was removed after success. No upload workflow or APK was added to main.

Public releases/latest returned the new non-preview release with MM-Tube-0.1.33.apk labeled versionCode=34. Downloaded APK hash matched the local APK and GitHub digest above. apksigner and aapt verified the existing certificate and package/code/version. The unchanged MM Auto Releases.java parser passed against the published feed, selecting the exact filename/URL/digest/code, offering the update from code 33 and refusing updates over code 34/35. This is a JVM parser check, not a physical MM Auto UI test.
