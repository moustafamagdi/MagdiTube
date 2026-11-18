# MM Tube 0.1.30 controller layout and idle Stop

Baseline: canonical main eb86270, released 0.1.29/code 30. Version 0.1.30-controller-layout uses code 31 and the existing package/certificate.

## Changes

- Stop requires a selected/pending request or fresh observed car media. Idle Stop is disabled and dimmed; a rejected idle Stop cannot change the request or navigation.
- Explicit Stop cancels the document/selection and returns the connected car to YouTube Home. Disconnect continues to unload without loading Home or retaining queued work. Stop remains connection-wide within its epoch.
- Phone transport no longer scrolls horizontally: five primary and four secondary buttons on narrow widths, one row at sufficient width. All controls retain at least 48dp touch bounds at the tested widths. Play/Pause dimming and command/seek ownership remain unchanged.
- Phone account/subscriptions/reload/ad blocking/background audio/setup/log sessions are accessible through More. The reset action remains available as More > Reset browsing screen. Car toolbar retains Back/Home/Reload/Search, with Ads block: ON/OFF.
- One-line ellipsized title, compact status, no empty title/seek space while idle. Branding, document leases, native phone media blocking and diagnostics privacy unchanged.

## Evidence

- Before the fix: the two new idle-Stop and loading-Stop regressions failed on the old implementation. Disconnect regression passed.
- Final Java/Robolectric suite: 81 passed. Includes actual Stop enabled/alpha state, no idle navigation/request mutation, Home after loading Stop, cancellation of the delayed selection, disconnect/reconnect, actual menu entries, active player layout at 320/384/832dp with a long title, all transport bounds inside the panel and at least 48dp touch dimensions. Layout tests are measurement tests, not visual screenshots. Existing freshness, ownership, buffering/error/ended, fullscreen and late-resume regressions still pass.
- 7 Node.js helper suites passed.
- Android 16/API 36, WebView 133 emulator: 24 native integration scenarios passed using production phone/car views with synthetic media. Two added scenarios verify idle Stop rejection and Stop during loading returning Home without a late start. Existing routing, native phone blocking/mute, cookie transfer, transport, fullscreen progress, rapid selections, stale status, disconnect/reconnect and phone-closure scenarios pass. Buffering/error/ended cases are injected.
- Final typography was additionally covered by the complete JVM layout/unit suite. Release build/Lint passed: 0 errors, 27 warnings.
- No real YouTube playback, visual phone screenshots or physical Android Auto validation for this change. An additional emulator screenshot-preparation command was rejected by automatic approval review with no detailed reason; no screenshot result is claimed.
- Signed upgrade from released 0.1.29 to 0.1.30 installed without uninstalling, preserving the existing app-data sentinel on the emulator.

APK: MM-Tube-0.1.30.apk; package com.magdi.tube; versionCode=31.
SHA-256: c06931d2f854a79c22783aa3a84762b928e30d70d3a7e83f4482669eb69d2e44.
Certificate SHA-256: 6d9d30d8f427a6cfc0241831d1e324e684fe2aefda72925c89ef872bf3a5ab65.

## Device check

1. Open car Home before choosing video: Stop must be dimmed, Home remains visible.
2. Select video and press Stop during loading, then during playback: car returns Home and the cancelled video never starts again.
3. Check portrait/landscape controls and More > Log sessions/Account/Android Auto setup.
4. Confirm observed Play/Pause dimming, seeking/fullscreen, disconnect during loading and no autoplay after reconnect.
