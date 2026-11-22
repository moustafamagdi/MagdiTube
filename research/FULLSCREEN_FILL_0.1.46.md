# MM Tube 0.1.46 — Fullscreen container Fill

The native-fullscreen path could report Fill while retaining a fitted video box. Changing only object-fit to cover does not enlarge a 640 x 360 box inside a 1280 x 480 player. The previous native test fullscreened the video itself and therefore missed the container case.

The helper now sizes the native fullscreen root, the selected video's intervening media wrappers and the video to the available viewport. Controls are not scaled or moved. It preserves the selected media element, source and playback position. Original style declarations are restored on Full picture, exit, reset or player/root replacement. While Fill owns the layout, a scoped style observer reapplies it after website style rewrites; it disconnects before restoration. The existing controller-fullscreen path retains its own layout handling.

A concise local diagnostic records the requested picture mode and applied/rejected callback result, without content identity or URLs. Phone playback, connection/request ownership, fullscreen button visibility and touch routing are unchanged.

## Validation

- The new Android WebView regression failed against 0.1.45: Fill reported true and cover, but measured x=320, y=60, width=640, height=360 in a 1280 x 480 viewport.
- It passes with the fix: native container geometry covers the viewport, controls remain hit-testable, website style rewrites and viewport resizing retain Fill, and Fit/exit restore original styles. The same media source remains selected.
- Existing synthetic tests cover wide/portrait layouts, phone and car buttons, paused position, advancing decoded frames, Stop, controller-only phone behavior, the 5-second overlay timeout and one original touch reaching player controls.
- 144 JVM/Robolectric tests and seven JavaScript suites pass. Release build and lint pass with no lint errors (37 advisory warnings).
- Real public YouTube video streamed in an API 36 emulator: controller-requested fullscreen switched visibly from letterbox to crop/fill at 1280 x 480 while retaining the same paused frame/source. Native container behavior was verified with synthetic Chromium playback; attempts to exercise YouTube's own native fullscreen in the emulator were not stable enough for a compatibility claim.
- No physical Android Auto/head-unit test was performed for this release. The recording supplied by the user is a symptom report, not proof of the fixed build on that device.

## Device check

Install over the current version. Play a video, enter fullscreen from the phone and from YouTube's player, then tap the visible circular Fill icon on the car. Confirm that surrounding bars disappear by cropping the picture, Full picture restores the complete image, and the controls still hide/reappear normally. Android Auto's own system/navigation bar and black bars encoded into the source video are outside this layout change.
