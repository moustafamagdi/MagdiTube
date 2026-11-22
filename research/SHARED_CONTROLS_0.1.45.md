# MM Tube 0.1.45 — One touch for player and app controls

The five-second overlay in 0.1.44 consumed a hidden-controls gesture before the YouTube player could receive it. This meant revealing the app buttons and the player's own controls required separate touches.

TubeView now dispatches the original Android gesture to its child normally. Hidden app buttons remain invisible and outside hit testing throughout DOWN/MOVE/UP; only after UP finishes dispatching are Exit and Fit/Fill revealed. This avoids activating an app button under the original touch. There is no synthetic replay, injected click, YouTube-private API, forced pause/play or seek. The website/native player retains its normal response to the touch, including deliberate interaction with controls that are already visible. CANCEL does not reveal hidden buttons. The five-second timeout, circular icons and accessibility behavior remain.

Validation on 2026-11-22:

- Signed release assembly and lint passed; 144 JVM/Robolectric tests passed. A new TubeView integration test dispatches the original DOWN/UP at a hidden Fit button location, verifies delivery to the child while the overlay remains hidden, then verifies both buttons appear without activating Fit.
- Seven JavaScript regression suites passed.
- The API 36 Android emulator test `tube/tests/fullscreen-overlay-native.mjs` uses real Android input taps and synthetic media. It verifies original pointer delivery, simultaneous app/player-control reveal with a YouTube-style sibling-controls fixture, and Pause/Play on the next tap without another reveal. Both remote and native fullscreen retain timeout, icon switching, playback frame progression, phone controls while hidden, Exit and Stop cleanup. The phone stays silent.
- The sibling-controls fixture is synthetic, not the live YouTube website. The native fixture can interpret a center tap as Pause; the test verifies that observed state and resumes explicitly, rather than undoing the player action in production. Native fullscreen checks exercise Android input and playback/overlay behavior; they do not prove YouTube-specific native chrome behavior. Live YouTube and physical Android Auto need device verification.
- Same package, signing certificate and update identity. No fixture/account assets or debug harness in the release APK.

Device check: enter fullscreen, wait five seconds, then touch the picture once. Check that the app buttons and YouTube controls appear together, then press Pause. Also check Fit/Fill, Exit and a second fullscreen session.
