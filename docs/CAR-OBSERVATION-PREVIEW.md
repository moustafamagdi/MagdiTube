# Car-window observation preview (2026-11-28)

Purpose: diagnose Open on car during camera coverage and missing Maps guidance without changing launch, focus ownership, or media-card activation behavior.

The app cannot observe the head unit camera or Google Maps UI. A lifecycle-visible window is not proof of physical screen visibility. No navigation content, watched titles, URLs, or location is collected by these events.

New safe events:
- car-ui: launch-tap, launch-pending, already-visible, start/resume/pause/stop, focus-gained/lost, window-visible/hidden, fullscreen-on/off, fill-on/off, user-marker.
- car-ui code bits: 1 bound car connection, 2 lifecycle visibility, 4 own window focus, 8 own window visibility, 16 own view attached. Missing bits mean the property was false at sampling, not that a camera was detected.
- media-card: session-active/inactive, none, playing, paused, stopped, buffering, error. These describe the control session; they do not prove a card is rendered.
- user-marker is user-reported Maps card absence, never automatic diagnosis.

Test journey on a stationary vehicle: compare normal Android Auto, camera open, camera closed; normal/fullscreen/fill playback; guidance with and without the media card visible. Tap Support & diagnostics > Mark missing Maps guidance card to timestamp the observation, then Send report now or allow enabled automatic reporting to send later. No device ID is hardcoded.

Automatic reporting remains optional and Android-scheduled (about every two hours); no live monitoring is implied. Safe local retention remains 24 hours / 256 events, uploads up to 128. Reports share with app name and UTC export timestamp and preserve prior snapshots.

Validation: complete existing Java unit suites, new safe-observation tests, release lint and signed release builds passed. Actual phone/car reproduction remains pending. Backend acceptance/rejection and dashboard labels are tested separately in MM-App-Diagnostics.
