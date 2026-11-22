# Automatic media-card binding — 0.1.40-auto-audio

The existing MediaBrowserService binds automatically when accepted car playback
observations select a ready, unmuted player. Opening MM Tube Audio manually is no
longer required to initialize its MediaSession. There is no second player or new
audio-focus request. The phone remains a controller only.

Pause retains the binding. Stop, replacement selection, expired observations and
disconnection clear old state and release it. Binding uses the application context;
failed or missing callbacks allow at most three attempts per selection, separated
by ten seconds. Stale connection callbacks cannot clear a newer binding.

Validation on 2026-11-22: 118 JVM/Robolectric tests passed (no failures/errors/skips),
clean release build and release lint passed. Existing lint warnings remain. The
API 36 emulator passed the existing 27-scenario native controller suite and six
new automatic-audio scenarios using synthetic video, actual Chromium decoding and
MediaController transport callbacks. These check automatic session creation,
observed Play/Pause and native phone buttons, covered fullscreen/progress, Stop,
reconnection without autoplay, and phone closure without local playback.

This is not an audible-output, real YouTube or physical Android Auto test. On a
parked car, start MM Tube playback without opening MM Tube Audio, switch to Maps,
check sound and card/phone Pause and Play, then return and verify the same video.
Test Stop and disconnect/reconnect: neither may restart playback automatically.
With background audio disabled, leaving the car interface must still pause.

Both launcher entries remain. Launching the car interface from the phone before
the car player is available is a separate, unimplemented next step. Upgrade in
place with the unchanged package/signing certificate; do not uninstall. Debug
harnesses and synthetic media are not part of the release APK.
