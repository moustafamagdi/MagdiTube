# Magdi Tube

Android app for viewing YouTube in a WebView, with experimental Android Auto projection. Package: `com.magdi.tube`.

## Install and use

Download the APK from [GitHub Releases](https://github.com/moustafamagdi/MagdiTube/releases). Install an update signed with the same certificate over the existing app to keep your settings and account session.

The phone app supports search, fullscreen and optional sign-in. Background audio and ad filtering are configurable. Playback, sign-in and ad filtering depend on the device's WebView and YouTube's current behavior.

## Android Auto

Projection uses a legacy integration and compatibility varies with the host and device. The car interface includes an Arabic/English search keyboard. The separate **Magdi Tube Audio** entry controls existing playback and shows video information and available playback actions.

The phone app can export diagnostics for playback and connectivity issues.

## Build and test

Use JDK 17 and the Android SDK with API 36 and Build Tools 36.0.0. Set `ANDROID_HOME` or configure `sdk.dir` in `local.properties`, then run:

```sh
./gradlew :tube:assembleDebug :tube:testDebugUnitTest
```

On Windows, use `gradlew.bat`. For an existing signed release, set `MAGDI_KEYSTORE` to the PKCS12 key file and `MAGDI_STORE_PASSWORD` to its password, then run `./gradlew :tube:assembleRelease :tube:lintRelease`. Keep signing files outside the repository.

The JavaScript suites in `tube/tests/` can be run individually with Node.js. See [third-party notices](THIRD_PARTY.md) for dependency sources.
