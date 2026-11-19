# MM Tube 0.1.35 keyboard letters

Baseline main a1f6e3f, version 0.1.34-unified-exit. VersionCode 36.

The built-in car search keyboard omitted Arabic dal (د) and dhal (ذ). Added dhal beside the number row and dal after jeem in the first letter row. The row count and maximum row width (12 keys) remain unchanged. English already contained all 26 letters.

Validation:
- 93 Java/Robolectric tests passed, 0 failures/errors. The existing car keyboard test now clicks every English letter, all 28 basic Arabic letters plus hamza forms, ta marbuta and alif maqsura, both digit sets, language switching, Space and Delete, then checks the exact encoded search URL produced by Go and toolbar restoration.
- Release APK build passed. Release Lint: 0 errors, 26 warnings.
- No new emulator, real YouTube or physical Android Auto test was performed for this keyboard-only change. Robolectric checks button creation and input behavior, not physical touch sizing.

APK: MM-Tube-0.1.35.apk, com.magdi.tube, versionCode=36.
SHA-256: e191318bac5e33a6ee52d731b488b83fe937554b50c94649e65c4855a247c074.
Certificate SHA-256: 6d9d30d8f427a6cfc0241831d1e324e684fe2aefda72925c89ef872bf3a5ab65.

Device check: update without uninstalling. Open Search on the car, switch to Arabic and type a query containing د and ذ. Switch to English and submit a query; confirm the letters and search results.

Publication: v0.1.35-keyboard-letters at dcacbab via successful [Actions run 44467596186](https://github.com/moustafamagdi/MagdiTube/actions/runs/44467596186). The temporary upload branch/worktree was removed after successful publication. No signing secrets were uploaded.

The downloaded public APK matched the local SHA-256 and existing certificate, package, versionName and versionCode. releases/latest returned this non-preview release with MM-Tube-0.1.35.apk labeled versionCode=36. The unchanged MM Auto Releases.java parser passed an additional JVM test against the public release JSON: expected URL/digest/code selected, update offered from code 35, no update over codes 36/37. This is a parser test, not a physical MM Auto UI test.
