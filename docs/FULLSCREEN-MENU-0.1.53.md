# Fullscreen settings menu correction

Direct inspection on the owner's Samsung SM-S948B (API 37, WebView
157.0.8103.50), projected through the existing minimized Desktop Head Unit,
reproduced the hidden gear menu on 2026-11-25. The desktop window was never
activated or restored. No private page URLs, titles or credentials were retained.

YouTube rendered its settings in `bottom-sheet-container` / `player-settings-menu`,
outside the promoted player. Our hide-everything-else selector hid this subtree.
The old popup visibility overrides also lost the CSS specificity comparison.
The click succeeded, but the resulting menu computed to `visibility:hidden`.

Exclude known popup roots and descendants from the hiding selector, including
the currently observed container. Preserve their normal display/closing logic
and raise them above the promoted player. Native fullscreen relocation now also
recognizes this container. Only legacy bottom-sheet renderers with dialog role
are portals; recommendation action buttons must not be promoted/reparented.

Live temporary CSS correction changed computed visibility to visible. Hit testing
confirmed that settings and quality options received input in Full and Fill.
The final source has separate headless Chromium CSS-cascade/click regression
coverage, plus native fullscreen portal insertion/restoration fixtures. This is
phone + DHU evidence, not physical vehicle validation or every YouTube layout.

Local WebView inspection is opt-in with `-PlocalWebViewInspection=true` and is
false in normal release builds. The inspection APK is not a release asset. Use
the same signing identity and install in place; never clear user data. Restore a
normal build and remove only the inspection ADB forward after the session.
