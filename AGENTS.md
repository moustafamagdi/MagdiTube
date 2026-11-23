# MM Tube project rules

- The permanent user-facing product name is **MM Tube**; the separate audio entry is **MM Tube Audio**. Do not revert to the historical Magdi Tube name.
- Use `@string/app_name` and `@string/audio_app_name` for Android labels. Keep UI, notifications, diagnostics and release titles consistent with MM Tube.
- Preserve the approved red play-button logo in `tube/src/main/res/drawable/ic_tube.xml` and the matching red headphones/play audio icon in `ic_tube_audio.xml`. Do not substitute the historical teal M logo or redesign these assets without a user request.
- The legacy package ID `com.magdi.tube`, GitHub repository `moustafamagdi/MagdiTube`, and existing signing certificate are stable update identities, not display branding. Keep them unchanged.
- Release assets use `MM-Tube.apk` for MM Auto compatibility. Increment versionCode for each published update.
- Preserve the phone-controller/car-player separation and diagnostic copy/export when making branding changes.
