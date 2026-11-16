# Magdi Tube
Independent YouTube WebView app with experimental Android Auto integration.
Private personal project extracted from moustafamagdi/MagdiMirror at e14b807.
Package: com.magdi.tube. Current version: 0.1.20-media-details.

## Build
Use JDK 17+, Android SDK 36 / Build Tools 36.0.0 and the Gradle wrapper.
Set sdk.dir in local.properties or ANDROID_HOME. For signed releases set
MAGDI_KEYSTORE to the existing private PKCS12 file and MAGDI_STORE_PASSWORD to
its password (alias magdi-mirror). Signing secrets are never included here.
Run ./gradlew :tube:assembleRelease :tube:lintRelease.
Run node tube/tests/player-filter.cjs and node tube/tests/ad-assist.cjs.

## Versions and testing
See [module notes](tube/README.md) for the architecture, experimental ad filter,
authentication behavior and actual DHU test limits. 0.1.7 permits the exact
accounts.google.com.sa HTTPS phone sign-in callback reported by the user.
The new account flow still requires a user retry; no login success is claimed.

Install updates over the existing application to preserve data and the account
session. The package name and release signing certificate remain unchanged.
Ad blocking can be toggled on the phone or with Block: ON/OFF on the car.
Complete ad blocking is not guaranteed. No Honda validation of this version.

0.1.8 adds player fullscreen handling, in-car Arabic/English Search keyboard, and experimental background audio. See module notes for test evidence and limitations.
