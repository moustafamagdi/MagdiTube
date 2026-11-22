# MM Tube 0.1.48: ad assistance default and car audio state

Ad assistance now defaults to enabled when no preference exists, including upgrades where the old disabled default was never explicitly saved. An explicit saved off/on choice remains unchanged. The existing toggle and filtering scope are preserved; complete ad blocking is not guaranteed.

The reported Unmute problem was reproduced on real mobile YouTube in an API 36 emulator: the HTML video had `muted=false`, `volume=1`, while `movie_player.isMuted()` was true and `button.ytp-unmute` remained visible. Calling the player's own `unMute()` corrected both the internal state and prompt.

The new car-only helper uses YouTube's unmute API first, with a scoped explicit unmute-button fallback and a native media fallback. It never hides the prompt with CSS. Repairs run at playback/metadata events and periodic checks, with a maximum of four attempts until a healthy state or a new playback event, and one fallback click per prompt per playback record. Stop/document revocation cancels the helper; obsolete video elements are rejected. Paused media is not automatically played. Explicit Play prepares sound; currently playing media is kept unmuted. Positive media volume is preserved; zero volume recovers the last positive level. Android/car system volume is untouched.

The phone controller never loads this helper and retains native muting and media blocking.

## Validation

- 147 JVM/Robolectric tests pass, including default-on and persisted-off preference coverage. Release lint: 0 errors and 37 existing warnings.
- Eight JavaScript suites pass. The audio suite reproduces separate HTML/player mute states, checks prompt synchronization, positive-volume preservation, pause/no-autoplay, media replacement, cancellation, absent phone helper and bounded fallback attempts.
- `car-audio-native.mjs` passes against a synthetic YouTube UI around a real decoded video in the Android debug harness. Pause/Play, fullscreen, Stop and no phone playback are checked.
- The same script with `LIVE_YOUTUBE=1` passes against the real mobile YouTube page for the public Big Buck Bunny test video. The real player's mute API, HTML media state and prompt visibility are checked at startup, after Pause/Play and during fullscreen. The old mismatch was separately reproduced before the fix.
- Emulator observations do not prove physically audible car-speaker output. No physical Android Auto test was performed for this release. No claim that all YouTube UI variants or all ads are covered.

## Device check

Install over the existing application. With the car player open, select a video from the phone and confirm audible sound and no lingering Unmute prompt. Check Pause/Play, the next video and fullscreen. Car/system volume can still silence output. If ad assistance was explicitly disabled before, enable it in More > Advanced options on the phone (or the car's Ads block control).
