# Magdi Tube 0.1.1 preview
Independent YouTube WebView application with experimental legacy Android Auto integration. No MediaProjection, accessibility or Shizuku; INTERNET is the only declared permission.
0.1.1 replaces the debug URL field/test button with Home, Back and Reload, a compact dark toolbar, loading indicator, localized Arabic/English controls and original vector logo.
Architecture reference: AndreyPavlenko/Fermata modules/web/FermataWebView.java and yt/YoutubeWebView.java. Newly written implementation; no Fermata source copied. Normal YouTube playback and ads preserved.
0.1.0 DHU results: feed videos rendered with speed 13.9/restrict all; user confirmed audible continuous sound. Not Honda validation. Precise A/V synchronization and pause/resume not measured. Controlled Morphe comparison incomplete due concurrent interaction.
Limitations: Google sign-in integration, fullscreen custom views, media session/background audio and seamless phone/car transfer are not implemented. Some external links are unsupported. YouTube/web/network changes can affect playback.
Build: :tube:assembleRelease :tube:lintRelease with existing project toolchain and local signing environment. Never distribute signing files.

0.1.2: phone-only Export log button shares bounded persistent app diagnostics via a read-only FileProvider grant. Includes device/AA/WebView versions, lifecycle, load error codes and 15-second video state samples. No URLs/titles/cookies recorded. Does not capture system/AA logcat. Google sign-in remains unsupported and is explained when requested. Build/lint passed; update installed and export button observed on device. End-to-end recipient attachment not yet verified.

0.1.3-login-preview: experimental phone Account and Subscriptions controls; allow HTTPS accounts.google.com/myaccount.google.com navigation only on phone. Standard WebView identity, no OAuth client impersonation. Shared default WebView cookie storage explicitly enabled and flushed on page completion/pause. No cookie extraction or credential logging. Video telemetry skipped on non-YouTube account pages. Successful Google authorization, persistence and personalized car feed require user testing; loading an account page does not establish login success.

0.1.4: expands phone-only HTTPS account navigation from accounts/myaccount.google.com to google.com and its exact subdomains to accommodate Google web return/consent hops. Does not permit arbitrary external sites or non-HTTPS links. Blocked navigation shows/logs only scheme and host, never path/query/token. Flushes cookies on blocked return so Subscriptions can check an already-created session. Actual reported blocked destination was unavailable in 0.1.3 logs; fix remains provisional pending retest.

0.1.5: optional ad assistance (OFF by default), independently written after reviewing TizenTube architecture; no TizenTube source copied. Hides known sponsored-card elements and clicks visible enabled known skip-ad buttons once per element. Does not intercept account requests, modify cookies, extract streams, seek videos or claim full ad blocking. Enable on phone and reload car page. No SponsorBlock. Live ad effectiveness is unverified; selectors can change.

## 0.1.6-adblock-preview
Experimental player-response filtering, independently implemented after studying
[TizenTube's adblock module](https://github.com/reisxd/TizenTube/blob/main/mods/features/adblock.js).
This removes adPlacements, adSlots and playerAds from recognized player responses,
including initial player assignment, JSON.parse and Response.json. It does not
rely on a skip button, so non-skippable client-side ads are also targeted.
It cannot guarantee removal of every ad, including server-inserted ads or future
YouTube delivery changes. No SponsorBlock or extraction of video streams.

AndroidX WebKit installs the filter before page scripts, for three exact HTTPS
YouTube origins only. Google account pages are excluded. Existing cookies, stream
formats and account payloads are preserved; updating does not clear application
data. On unsupported WebViews the diagnostic log reports that filtering is
unavailable and the existing skip-button/card helper remains the fallback.

Enable **Ad blocker** on the phone; this reloads the phone page. Press **Reload**
on the car to apply the same setting. Switch it off and reload to restore original
playback. Default remains OFF. Diagnostic video samples now report the number
of player ad fields removed, not a count of ads prevented. Sanitized app events
are also written to the MagdiTube logcat tag; exported reports still contain only
app diagnostics. No cookies, credentials or full URLs are logged.

Validation: release build and lint passed; synthetic JavaScript tests cover
player field removal, stream/account preservation, exact-origin isolation,
initial assignment, Response.json, JSON.parse revivers/errors and disabling.
These tests alone do not establish real-world ad removal.
Car toolbar also includes Block: ON/OFF for immediate toggle and reload without
opening the phone. The nonEmpty diagnostic counter distinguishes removed
nonempty fields from empty placeholders; neither counter counts actual ads.

Live DHU 2.1 test (2026-11-15, SM-S948B, parked: speed 0/restrict none):
- Updated existing signed installation without clearing data; profile avatar and personalized feed remained visible.
- Feed video played with blocker ON; telemetry progressed from 9 to 24 seconds, readyState 4, muted false. Three nonempty player ad fields were removed.
- OFF reloaded and played the same content. ON again removed two nonempty fields and playback progressed from 3 to 18 seconds. Car tap unmuted the player.
- Counts are fields, not ads. This is not a deterministic before/after ad-serving comparison: the OFF reload also played content without an observed ad. Non-skippable ad removal and universal blocking are NOT independently proven. Audible output was not independently verified in this test; no Honda hardware test.
- Earlier transient connection error recovered after reload.

## 0.1.7-login-fix
The supplied phone screenshot showed a blocked HTTPS redirect to
accounts.google.com.sa after Google's two-step verification. Add that exact
hostname to phone-only account navigation. No wildcard country domains, no
change to the YouTube-only ad filter or cookie storage. Car account navigation
remains blocked and HTTP remains blocked. Build/lint validation does not prove
successful completion of the user's authentication; retry on phone is required.

## 0.1.8-playback-preview
- Implement WebChromeClient custom-view fullscreen with an Exit overlay and Back
  handling. Hides the app toolbar; the Android Auto system bar remains host-owned.
  Phone system bars can be hidden during fullscreen and normal insets restored.
- Add an explicit Search control. In the car this uses an in-app Arabic/English
  keyboard and URL-encoded YouTube results; use this instead of the site's search
  icon when the host IME is unavailable. Queries are never logged. Phone uses IME.
- Background audio defaults ON, configurable on phone. Temporary window coverage
  no longer explicitly pauses WebView. A mediaPlayback foreground service with
  Stop notification and media session is maintained while playback is observed.
  Temporary audio-focus loss pauses and gain requests resume; permanent loss stops.
  Notifications permission is requested on phone for the Stop control.
- No background survival guarantee: WebView/site/host may still pause media, and
  activity destruction, disconnection or process termination releases playback.
  The WebView remains active, so background audio can consume more battery.

Validation: signed release build and lint passed (0 errors). Five Robolectric
Android API 32 tests cover fullscreen restoration, search encoding and dismissal,
covered-view behavior and audio-focus callbacks. Existing ad-filter JS tests pass.
These are simulated component tests, not an actual YouTube/car streaming test.
No phone was connected for DHU testing. A local Android emulator startup attempt
exited before device registration. Honda reverse-camera/audio behavior, actual
YouTube fullscreen and keyboard rendering remain user-test items.

Device checklist: update without uninstalling; allow notifications; play a video,
use its fullscreen button, then Exit/Back; use app Search to type Arabic/English;
while parked switch to the car camera and return, checking playback/time; switch
away and check audio/Stop notification. Export the log if playback pauses.

References: https://developer.android.com/reference/android/webkit/WebChromeClient
and https://developer.android.com/about/versions/14/changes/fgs-types-required

## 0.1.9-playback-fix
User reported fullscreen works but playback pauses after approximately two seconds.
That matches the new playback polling interval. The 0.1.8 keep-alive service was
requesting a second audio-focus owner and creating a second media session while
WebView was already playing. This is the leading code-level explanation, not a
confirmed device trace.

Remove that extra audio-focus request/session and the automatic focus callbacks.
WebView remains responsible for actual media focus and system interruptions.
The service only maintains foreground priority and the explicit Stop notification.
Multiple phone/car views are tracked without automatically pausing one another.
Fullscreen, search keyboard, cookies, login and ad filtering remain unchanged.
Background continuation is still experimental and depends on WebView/host behavior.

Regression tests check repeated service starts do not request focus or issue pause,
second-view registration does not pause the first, and explicit Stop still pauses.
No live phone/car test was possible for this hotfix; confirm on Honda while parked.

## 0.1.10-resume-preview
Experimental car-session retention: reuse one TubeView across replacement car
activities, detach the old parent and rebind a mutable context. A detached paused
session expires after five minutes; playing sessions remain until stopped/process
exit. Notification Stop also releases a detached retained car session.

Track page playback intent on play/pause/end, visibility and native cover events.
On host return or an observed audio-mode interruption ending, request resume once
only when the page reports prior playback intent, audio mode is NORMAL and no
music is currently active. Manual visible pauses, recent pointer-associated pauses,
ended videos and notification Stop cancel the intent. No competing audio focus
or additional media session is introduced. Native WebView still handles ducking.
Phone-call recovery is best effort: audio mode/visibility timing may vary; no
telephony permission or guaranteed phone-call detection is claimed. Background
WebView playback itself may still be suspended by YouTube/Android/host behavior.

Tests: component tests cover session reuse plus existing fullscreen/search/no-focus
regressions. JS tests cover cover/resume, single attempt, manual pause, Stop and end.
Live camera/call/notification handling on Honda remains unverified. Test while parked:
play, open camera and return; open another screen and return; manually pause then
leave/return (must stay paused); test a notification and call. Export diagnostics
immediately after an unexpected pause or unwanted resume.
