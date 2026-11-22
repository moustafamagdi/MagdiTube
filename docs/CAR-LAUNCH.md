# Phone-requested car UI launch

Both apps use the bundled legacy com.google.android.apps.auto.sdk projection
service, not an AndroidX CarAppService. The modern CarContext.startCarApp API is
therefore not a drop-in solution (https://developer.android.com/reference/androidx/car/app/CarContext).

The implementation uses the bundled SDK's CarActivity.startCarActivity when this
app already owns a connected host. Otherwise it creates a short-lived
android.support.car.Car connection and, only after the SDK confirms a projected
car connection, requests CarFirstPartyManager.startCarActivity. This is a
best-effort legacy host API: the class name does not grant first-party privileges,
and Android Auto may deny or not support it. No host patch, root, shell launch,
permission escalation, background activity intent or additional permission is used.

The only intent target is this app's projection service, with MAIN/projection
category and package name. It carries no media URL, title, credentials or selection.
An eight-second deadline bounds each attempt. Repeated taps while pending do not
send duplicate requests. Missing/denied/disconnected responses end the attempt;
late callbacks cannot launch or complete another request. Leaving the phone or
Stop cancels pending work. There is no retry on reconnection.

A request is only reported as opened after the app's current car host reaches
onStart; sending an intent is not success. Coverage/disconnection/replacement
invalidates visible status, and old host callbacks cannot mark a successor visible.
This observes Android lifecycle, not physical head-unit pixels.

The phone has an Open on car button (Arabic/English). Selecting media requests
foreground presentation too. If the car player is unavailable, the media selection
is rejected immediately; the UI launch can still be attempted, but the user must
select the item again once ready. No delayed playback is retained, including after
Stop, phone closure, expiry or reconnection. An available player continues to use
the existing latest-request-wins controller. Audio-card binding is unchanged.

## Validation and limitations

Automated tests cover successful mocked SDK/host launch, missing callbacks,
permission denial, timeout, cancellation, duplicate taps, old callbacks, warm
host routing and actual app onStart/onStop lifecycle integration with the external
SDK host stubbed. These do not establish first-party-manager access on a real host.
Release builds retain the package/signing certificate and contain no test account.

A physical Android Auto connection is required to validate both cold launch and
bringing an existing interface forward. Test while parked: connect without opening
the app on the car, use Open on car on the phone, and observe the result within
eight seconds. If it opens, select media afterward. Then switch to Maps and select
a different item on the phone. Verify sound/progress and transport controls.
Finally disconnect during a request and reconnect: no media may autoplay. A denial
or timeout should leave the manual-open fallback and all normal playback intact.

Validation on 2026-11-22: 134 JVM/Robolectric tests, release build and release lint passed (existing warnings remain). The existing API 36 synthetic controller suite passed 27 scenarios. Native launch fallback is tested on a fixture emulator without Android Auto installed; this can establish rejection/retryability and no delayed media, not successful car launch.

The 3 added native fallback checks passed, including the actual launch button, an unavailable-selection/reconnect flow and no local media. IPTV also checks a UI language change. The Android Auto host is absent in these emulator fallback checks.

The standalone APK uses Gradle dex compression to reduce download size. All 105
non-signature ZIP payload entries match the uncompressed build byte-for-byte,
and the signing certificate is unchanged. The compressed debug APK installed as
an update and passed six additional synthetic media-card/lifecycle scenarios on
API 36, including decoding, fullscreen coverage, phone closure and no autoplay.
