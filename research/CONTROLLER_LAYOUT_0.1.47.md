# MM Tube 0.1.47: phone controller alignment

The horizontal Android LinearLayouts used default text-baseline alignment with fixed-height children. Labels wrapping to two lines could push buttons below the row boundary. Six controls also shared a narrow portrait row.

Rows now align button edges, center text, and use a shared measured height that accommodates wrapped labels. Portrait groups playback, navigation and display actions separately; wider layouts use two rows. Font scaling and available width determine further reflow. Rounded outlined buttons have consistent spacing, a 48dp minimum touch target and native ripple feedback. Observed playback enablement, dimming and click/request ownership are unchanged.

## Validation

- 145 JVM/Robolectric tests pass. The new layout regression uses native text rendering and covers English/Arabic, 320/384/832dp, and 100/130/200% font scale. It checks aligned top/bottom edges, row containment, touch targets and complete text height. It failed against the old layout (`Previous inside row`). Legacy shadow text layout alone did not reproduce the problem, so this test class now uses native graphics.
- All seven JavaScript suites pass; Android release lint: 0 errors, 37 existing warnings.
- `tube/tests/controller-layout-native.mjs` checks actual buttons in the phone MainActivity on an API 36 emulator: English and Arabic, 360dp portrait, 320dp portrait at 130% font size, and 832dp landscape. It checks bounds, overlap and disabled state when no car is available and saves screenshots for visual review.
- Same package ID and release signing certificate; no account is embedded. Upgrade installation is checked without uninstalling the app.
- No new physical Android Auto test or real playback test is claimed for this phone layout change. Controller playback state/request regression tests remain in the existing suite. Fullscreen Fill logic is unchanged from 0.1.46.

## Device check

Install the update over the existing app. Check the phone controls in portrait and landscape, including Arabic and enlarged system text. Connect/open MM Tube on the car: Play/Pause should follow the observed car state; Fill and Full picture still operate on the car only.
