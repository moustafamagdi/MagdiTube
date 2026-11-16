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

## 0.1.11-picture-fix
User confirmed continuous audio during camera coverage in 0.1.10, but reported
that the visible video frame freezes after return until a touch. This suggests
render/focus restoration rather than a playback pause; not independently traced.

On resume, window-focus gain and attachment, schedule a bounded picture refresh
(immediate, 250 ms, 900 ms). Only runs while attached, shown and active, outside
account/search pages. Resume WebView rendering, request view focus/layout/redraw,
notify page resize, and request a WebView visual-state callback followed by redraw.
Fullscreen container is refreshed too. No simulated taps, pause/play, seek, reload
or audio-focus requests are used by this refresh path. Callbacks cancel on cover
or destruction. Visual-state readiness does not prove video frames were presented.

New component regression checks refresh sends no pause/play/seek/reload and does
nothing while covered. Hardware-compositor recovery on Honda is UNVERIFIED.
Test while parked: open camera and return without touching the player, in normal
and fullscreen modes. If still frozen, export log before touching the player.

## 0.1.12-fullscreen-return
User confirmed normal-view camera return works in 0.1.11 but fullscreen returns
paused. The custom fullscreen video subtree did not have the WebView's temporary
window-visibility preservation. Apply it to the custom subtree, leaving actual
host-window visibility under Android Auto control. Capture cover intent before
focus/visibility callbacks and on page blur. On fullscreen return, bounded checks
at 300/1100 ms refresh the picture and attempt existing guarded resume. These are
not a recurring play loop; manual pause/Stop cancellation remains in place.
No extra audio focus, reload, seek or simulated touch is introduced.

Tests cover custom subtree visibility/cover notification and page-blur pause intent,
plus previous regressions. Actual Honda fullscreen audio continuity remains
unverified. Test normal and fullscreen camera return while parked, and manual Pause
before leaving (must remain paused). Export log immediately if playback still stops.

## 0.1.13-larger-buttons
User reports 0.1.12 works well in all tested modes on their Honda. Increase search
Go and Close buttons to minimum widths of 112/96 dp and fullscreen Exit to 128 dp.
All three use 18 sp text and 56 dp height; Exit has an 8 dp edge margin. Widths
can expand for larger text. Playback behavior is unchanged. Build, lint and existing
component tests are checked; the new sizing has not been viewed on the Honda.

## 0.1.14-install-setup-preview
Phone-only Android Auto setup opens Android's system installer for a user-confirmed
self-reinstall of the currently installed, same-signed APK. Requests install permission
through system settings. The APK is copied only from this app's sourceDir to a narrowly
scoped cache FileProvider path; no download, root, Shizuku or silent installation.
Split installs are rejected. App data is preserved by an in-place installation.

Experimental classic installer metadata hints follow the approach documented by
https://github.com/fcaronte/KingInstaller (independent implementation; no code copied).
Android/OEM versions may ignore these hints. No guarantee of Android Auto acceptance.

Diagnostics include current installer/initiator and lastUpdateTime, a persisted attempt
baseline and before/after result, checked at process start, package replacement, setup
open and export. A matching pre-existing source is explicitly distinguished from a
new match; a dialog result alone never counts as success. Update timestamp evidence
means replacement was observed, not proof of which installer performed it if another
installation occurred in the meantime. A narrow known-initiator list is diagnostic,
not Android Auto's official acceptance criteria. No claim of successful self-reinstall
until tested on the user's Samsung. Playback logic remains unchanged.

Validation: signed release build and lint passed; 14 component tests passed (5 setup
checks and 9 playback/UI regressions). No connected phone was available; actual
Samsung self-reinstallation and Android Auto acceptance remain unverified.

## 0.1.15-media-card-preview
Adds a MediaBrowserServiceCompat / MediaSessionCompat control-only source labelled
Magdi Tube Audio, alongside the legacy video entry. Choose a video in Magdi Tube,
then select its Audio source in Android Auto and return to the navigation dashboard.
The host controls card placement and naming. Play/Pause target the existing WebView;
no new decoder, media URL extraction, audio-focus request or playback on browser
connection. The muted phone preview cannot replace an established car target.

Pause cancels automatic resume intent and keeps the selected target. A detached car
session is exempt from idle expiry while its media-card service exists. Play requests
WebView playback and uses the existing foreground keep-alive, respecting background
Audio OFF and audio-mode interruptions. The app cannot recover a document after
process death; choose a video again. Existing full-screen video behavior is retained.

Generic current-video metadata is used; no titles/URLs are logged. Diagnostics record
browser connections, control commands, target and playback-state reports. Reports
on Pause initially reflect the requested state; periodic WebView samples provide
subsequent observations. There is no searchable media catalog/voice-search support;
only the two specific media-search lint checks are suppressed for this control-only
preview. No next/previous/seek or artwork feature is claimed.

The 0.1.14 user log confirmed self-reinstall changed installer from packageinstaller
to com.android.vending while keeping the system installer as initiator on SM-S948B.
This confirms metadata change, not Android Auto acceptance on other devices.

Validation: release build, scoped lint and 19 component tests passed. Media-card
tests capture the state submitted to Android's session API (Robolectric does not
round-trip it through a controller), routing, paused-target retention, no competing
audio-focus request and no playback on connection. No phone was connected, so real
Android Auto discovery, dashboard placement and Play/Pause delivery remain untested.

## 0.1.16-exit-position
Moves the fullscreen Exit button from the top-right corner to the middle of the
physical left edge, including Arabic layouts, to avoid the reported overlap with
YouTube quality/subtitle controls. Keeps the 128 dp minimum width, 56 dp height,
18 sp label and 8 dp edge inset. No playback or media-card behavior change.
The button still overlays video; actual player layouts can vary.

Validation: release APK build, lint and all 19 existing unit tests passed. Actual car/player layout verification remains with the device test.

## 0.1.17-compact-exit
Replaces the fullscreen Exit label with a white vector close icon on a translucent
36 dp circular background. A 48 dp touch target keeps it usable without the previous
wide visual footprint. The circle remains midway down the physical left edge with
an 8 dp visible inset, including RTL layouts. Adds bounded press feedback and a
focus outline, with localized accessibility descriptions.

Validation: signed release build, lint and all 19 existing component tests passed.
Fullscreen tests exercise construction of the new drawable/button. Appearance on
the actual car display remains a user device check. This release changes only the
Exit control; the discussed performance and expanded media-card features are not
included.

## 0.1.18-youtube-thumbnail
The audio card now publishes the current YouTube video's existing thumbnail via
local content URIs in session metadata and the playable browser item's icon.
Source order: matching current player-response thumbnail (closest to 480 px),
matching video poster, matching Open Graph image, then YouTube's hqdefault thumbnail
for the watch/shorts/live video ID. No screenshots, canvas, frame sampling, media
stream extraction, frame decoding or separate player are used. Downloaded image
bytes are decoded/resized as an ordinary static image, preserving aspect ratio.

Only HTTPS i.ytimg.com/img.youtube.com image paths containing the exact video ID
are accepted in JavaScript and Java. A stale player response is rejected during SPA
navigation; metadata from another video is ignored. URLs/IDs are stripped before
playback diagnostics are serialized. Logs report requested/ready/unavailable only;
ready means a local thumbnail is available, not that the host has displayed it.

Downloads and image processing run on one background worker with a one-item pending
queue; stale completions cannot replace newer artwork. Redirects are disabled;
connect/read timeouts are five seconds, response cap 1 MiB, source dimensions at
most 4096 per side, and output at most 480 px on the longest side. Disk cache retains
up to eight images / approximately 2 MiB with hashed filenames. Only the individual
cached image receives read permission for Android Auto; report/installer files are
not granted. Android Auto receives local URIs rather than large bitmap IPC payloads.

Image requests happen when the selected target's artwork changes, not on every
playback sample. Pause preserves its artwork; muted phone previews cannot replace
an established car target. No artwork, failed download or a new video awaiting its
image uses the app icon rather than an old image or a captured frame. A failed URL
is not repeatedly retried by the two-second playback poll; changing the video or
reconnecting/recreating the audio service allows a new request. Feed previews with
no watch/shorts/live video identity use the app icon.

Validation: release build/lint and 27 component tests passed, including metadata
publication, stale-completion isolation, pause/cache reuse, download-failure
fallback, URL/ID validation, image bounds/resizing and cache eviction. Twelve Node
thumbnail identity/source checks and all existing JS suites passed. No phone was
connected, so actual YouTube DOM variations, Android Auto artwork reading/layout
and car rendering remain device checks. This release does not add next/previous,
seek, thumbnail-to-video navigation, or claim to fix audio/video synchronization.
Reference: https://developer.android.com/training/cars/media/create-media-browser/media-artwork

## 0.1.19-performance-preview
Reduces app-side work during playback without changing video quality, playback
speed, audio focus ownership or the original YouTube thumbnail source.

- Hidden phone playback yields to an observed playing, unmuted car video. Its
  media is paused, future autoplay is guarded and its sampling stops. Returning
  to the phone removes the guard without automatically restarting that video.
  A visible phone and account sign-in pages are left available.
- Cookie disk flushes are coalesced on a background worker. Diagnostic formatting
  and logcat writes also run in the background, with at most 128 pending events;
  overflow is counted rather than allowed to grow without bound.
- Repeated focus/fullscreen-return notifications share a three-refresh burst.
  Only one visual-state callback and one playback sample per generation may be
  outstanding. Navigation/resume invalidates stale sample results.
- Playback tracking avoids scanning the DOM on unrelated mutations. Bundled
  scripts are cached in memory; artwork lookup is requested only for media-card
  candidates while the service exists. Steady media-session progress is updated
  every ten seconds; observed state changes and large seeks publish immediately.

Detailed playback diagnostics remain approximately every sixteen seconds and now
identify phone/car and a process-local view instance. They include video size,
total/dropped frame counters when supported, waiting/stalled/load events, timer
lateness, JavaScript callback delay, refresh count and app Java heap usage. No
video frames are captured. These counters do not measure car-display latency or
audio/video offset; the Java heap figure excludes native, GPU and WebView renderer
memory. The normal playback/control observation interval remains two seconds.

Validation: signed release build, lint and all 34 component tests passed. All five
Node suites passed, including sample instrumentation, suppression/autoplay guards,
unrelated DOM mutations, original thumbnail selection and ad-filter regressions.
Tests verify coalesced cookie writes, bounded refresh bursts, slow/stale sample
handling and media-session update throttling. No phone was connected: actual car
smoothness, audio/video synchronization and camera/fullscreen return still require
device testing. No measured FPS or synchronization improvement is claimed.

References: https://developer.android.com/reference/android/webkit/CookieManager#flush()
and https://w3c.github.io/media-playback-quality/

## 0.1.20-media-details
Publishes the current video title and channel in media-session title/display-title
and artist/display-subtitle, and in the playable browser item. Reads only a player
response or player video data matching the current YouTube video ID. Missing or
stale details use a generic current-audio label; document titles from search/feed
pages are never used. Text is bounded to 300 characters and control characters are
removed. Title changes publish even when artwork is unchanged; pause preserves
details and navigation clears them. Titles remain inside the artwork observation
removed before diagnostic serialization, and are not logged or written to disk.

Adds standard next/previous media-session callbacks and advertises each action
only when available. The page bridge checks a matching current watch-page player,
then uses its enabled next/previous button or a verified playlist neighbour and
corresponding player method. It checks identity and availability again at command
time, so a stale command cannot skip a newly selected video. No browser-history
navigation, arbitrary recommendation selection or rewind-as-previous fallback is
used. Unsupported player layouts and playlist boundaries leave that action absent.
Calls and suppressed phone pages cannot start playback through skip commands.

Original YouTube thumbnail retrieval, aspect ratio and performance optimizations
are preserved. This release does not alter artwork cropping/layout by the host,
add seek controls or open the video when artwork is tapped.

Validation: signed release build, lint, 37 component tests and all six Node suites
passed. Added title replacement/pause/clearing/size tests, immediate action updates,
car target routing, call protection, playlist boundaries, disabled buttons and
stale-command rejection. No connected phone: actual mobile YouTube player methods,
button availability and Android Auto card presentation still need device testing.
Reference: https://developer.android.com/training/cars/media/enable-playback

## 0.1.21-full-thumbnail
Fits the complete original YouTube thumbnail into a 480 x 480 dark artwork tile,
preserving its aspect ratio with a 24 px minimum inset. This compensates for square
center-cropping by media-card hosts instead of cropping the source image. Host
badges, rounded masks and unusual layouts remain outside the application's control.
Static image composition runs on the existing bounded background image worker;
there is no video frame capture or per-frame work. A versioned cache key ensures
previous rectangular cached artwork is replaced without clearing app data.

Validation: signed release build, lint and all 39 component tests passed. New
checks verify full-image bounds/aspect ratio for landscape, portrait, square and
4:3 images, square output and source bitmap ownership. This APK was not installed
on the connected phone; actual Android Auto card appearance requires device testing.

Reviewed both Tube and IPTV manifests: each registers a media-browser service and
a separate legacy projection service. This explains the two launcher entries per
app in the supplied screenshot; installed package listing showed one package for
each app in the queried user. No launcher services were removed. Thumbnail tap
handling is owned by Android Auto's media UI; direct navigation from that surface
to the legacy video CarActivity has not been implemented or verified. A normal
phone activity session intent is not treated as an equivalent car-video shortcut.
Host UI reference: https://developers.google.com/cars/design/create-apps/media-apps/overview

## 0.1.22-audio-icon
The MediaCardService has its own gold headphones vector icon with a white Tube
mark. The video/projection and phone application keep the existing mint Tube icon.
Both services remain enabled. Playback, artwork and navigation are unchanged.
Release build and lint passed; compiled manifest icon references were checked.
Android Auto may cache launcher artwork; actual host appearance remains a device check.
