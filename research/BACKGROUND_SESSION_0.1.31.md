# MM Tube 0.1.31 background session recovery

Baseline: canonical 0.1.30-controller-layout, main ade3d34. VersionCode 32, same package and release certificate.

## Cause and change

TubeCarActivity.onStop called TubeView.pause, which detached the controller, cancelled the document and navigated about:blank. Android host coverage therefore discarded playback. A new cover path keeps the active player, immutable document-start lease, polling, request, media consumers and fullscreen subtree. It stops visual refresh work while hidden. Resume after cover does not navigate Home, including during loading; bounded existing guarded resume callbacks respect explicit Pause/Stop/replacement/disconnection.

CarPlaybackSession retains a covered/recreated host while TubeService remains bound. Binding ownership prevents a stale old service callback cancelling a newer connection. Unbind/destroy still stops and clears all pending work. No phone playback/fallback, no reconnect autoplay. Background audio OFF continues to request pause rather than background playback.

## Tests

- The initial old-code regression failed its connection-preservation assertion. The legacy SDK superclass lifecycle was shadowed to avoid requiring the actual Android Auto host, while the production TubeCarActivity.onStop executes normally.
- 88 Java/Robolectric tests passed (7 new lifecycle regressions): onStop request/document retention, loading cover, fullscreen return, same-service host recreation, disconnect while covered, Stop while covered, old-service callback isolation. Existing resume-generation, stale-state, layout, privacy and controller regressions remain passing.
- 7 Node.js JavaScript suites passed.
- Android 16/API 36, WebView 133.0.6943.137: 27 native synthetic integration scenarios passed, plus 1 separate native fullscreen-cover check (28 total). Cover tests dispatch window visibility changes and verify actual WebView video time keeps advancing, the document/request is unchanged on return, manual Pause stays paused, and a slow selection survives cover/return. Existing native phone blocking, cookie attributes/deletion, transport/freshness, rapid selection, fullscreen, Stop, disconnect/reconnect and phone closure pass.
- Native emulator tests use synthetic media and injected faults, not live YouTube or a real reverse camera. They do not prove audible sound, physical Android Auto lifecycle/binding behavior or compatibility with Samsung WebView 157. Host recreation is a JVM lifecycle test.
- Release/debug build and release Lint passed: 0 errors, 27 warnings.
- Signed 0.1.30 -> 0.1.31 emulator install-r upgrade passed without uninstalling and preserved the existing app-data sentinel.

APK: MM-Tube-0.1.31.apk; package com.magdi.tube; versionCode=32.
SHA-256: 8c3e7c8b2f58943d278795b1b01ec05a1e2012676c3edde192c4c74a74c74c06.
Certificate SHA-256: 6d9d30d8f427a6cfc0241831d1e324e684fe2aefda72925c89ef872bf3a5ab65.

## Physical device check

With Background audio ON, play a video/fullscreen, open reverse camera or another car view, then return. Confirm continuing audio, retained video/time and no Home reload. Repeat after explicit Pause (must remain paused) and Stop (must remain stopped). Disconnect while loading; reconnect must not autoplay. If a particular host unbinds the car service when opening its camera, playback is intentionally cancelled at that boundary; export the new Car log so that lifecycle can be identified rather than treating it as a mere cover.

## Publication verification

Published v0.1.31-background-session at 0a19a03 using [GitHub Actions run 44324481286](https://github.com/moustafamagdi/MagdiTube/actions/runs/44324481286). The browser was unavailable, so an isolated temporary upload branch used GitHub's built-in workflow token to verify and publish the already-signed APK; no local authentication tokens or signing secrets were uploaded. Its checks restricted source changes to the upload bundle/workflow and verified exact SHA-256, existing certificate, package and code before publishing. The temporary remote branch/worktree was removed after success; no upload workflow or APK was added to main.

Downloaded public APK matched the local SHA-256 and existing signing certificate; aapt confirmed versionCode 32 and versionName 0.1.31-background-session. Asset label is versionCode=32. The unchanged MM Auto Releases.java parser passed its published-feed regression, selecting the exact asset/URL/digest/code, offering an update from code 31 and rejecting an update over codes 32 or 33. This is a JVM parser check, not a physical MM Auto UI test.
