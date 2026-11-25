# User flow update 0.1.51

- A covered host gets one bounded SDK foreground retry if its existing-host request does not bring it back. Cancellation, disconnect and stale callbacks remain guarded. Pause/resume now updates foreground state.
- Open on car has a distinct blue background and accent border.
- Support advanced options use a bordered expandable group, show/hide text, chevron and saved expansion state. IPTV settings use the same disclosure wording.
- Removed executable self-reinstallation and King handoff helpers and request-install permission. Installation help opens MM Auto or its official releases. Existing package IDs, signatures, settings/accounts and diagnostic history are preserved.
- Tube: YouTube popup menus remain visible above inline fullscreen; native fullscreen portals are moved into the fullscreen subtree and restored on exit without moving the video. Fill keeps video below controls.

Validation: release unit tests and build; Tube JavaScript regression suites and a jsdom native popup/fill/restoration fixture. Browser visual check unavailable; real phone/car verification remains required. No claim of Android Auto visibility based only on installer metadata.
