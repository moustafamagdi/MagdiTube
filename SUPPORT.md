# Shared Support ID and simpler settings

Update MM Auto, MM Tube and MM IPTV over the installed apps; do not uninstall or clear data. Open Support & diagnostics in each updated app. With MM Auto available, all three display one random Support ID within the same Android profile. App updates and process restarts retain it. Profiles/work profiles and cloned apps are separate; it is not a permanent hardware or person identifier. Clearing MM Auto data can create a new shared ID. Other apps reuse their cached shared ID while MM Auto is unavailable; without a cache they display their existing installation ID until MM Auto is available.

The original per-installation ID and upload credential are kept internally. A successful disclosed upload links that installation, including its retained older reports, to the common ID. Old IDs remain searchable. Opening the support screen does not upload or retroactively enable diagnostics.

The main support screen offers Copy support ID, Send report now, optional automatic sending and Privacy. Advanced options contains manual Copy/Share, detailed local sessions (Tube/IPTV) or installation details (Auto), and Delete this app’s reports. Deleting stops this app's automatic delivery, removes only this installation’s reports after server confirmation, and rotates its internal installation credential; the common device ID and other apps’ reports remain. Offline deletion retains credentials for retry. Saved exports are unaffected.

## Consent and privacy

Fresh installations default to automatic sharing on, separately for each app. A short first-use phone notice explains the improvement purpose, device details/support ID, excluded sensitive content and the option to stop sharing. Destination, hosting provider, transport, retention and delivery frequency remain available in Support & diagnostics → Privacy & retention. Continue keeps sharing on; Turn off sharing stops it. No automatic job is scheduled and no automatic request is sent before that notice is acknowledged. Closing the app without choosing keeps uploads blocked. The notice is in Arabic or English and is not shown over the car player.

Any saved off setting remains off on upgrade, including an earlier default: old builds did not reliably distinguish it from an explicit opt-out. Existing enabled settings remain enabled but wait for this disclosure once. Choices survive restarts and updates. Turning sharing off cancels the job and active connection; deleting reports also persists the off choice even when deletion fails. No hardware identifier, credential, app data or backend retention/quota is changed. Manual sending still explains linking. No hardware ID, phone number, watched titles, searches, stream/provider URLs, account credentials or cookies are used for identity linking. The provider exposes only a random UUID and linking proof to pinned official MM packages with the existing signing certificate; unknown callers and other signers are rejected. The proof is never copied, exported, logged or displayed. It is sent over HTTPS to the diagnostics service, which stores only its hash. Installation upload credentials remain distinct.

Technical categories, local 24-hour/256-event safe history, at most 128 events per upload, approximately two-hour optional automatic schedule and server retention up to 30 days stay unchanged. Supabase hosts reports in Frankfurt and necessarily receives IP addresses for transport; diagnostic tables do not store them. The previous full local logs remain manual-only under Advanced, with their existing retention limits.

## Settings

IPTV keeps account fields and Support directly accessible; secondary playback/install and account-removal options are collapsed under Advanced. Tube keeps Subscriptions, Account and Support in More, with troubleshooting/playback options in Advanced. Auto keeps appearance, language, update alerts and Support in Settings; version/help/installation-method tools are grouped in Advanced. Player routing, subscription data, package IDs and approved branding remain unchanged.

Owner dashboard: https://mm-app-diagnostics.moustafa-magdi.workers.dev (owner only, Arabic/English).
Backend: https://github.com/moustafamagdi/MagdiIPTV/tree/main/support-backend

## Validation

All three release builds and Android lint were run, plus JVM/Robolectric regression tests. Backend validation and transactional tests cover linking, old-ID lookup, wrong proofs, app-isolated deletion and private grants. Synthetic calls to the live Edge service verify accepted/rejected payloads and clean up their test reports. Dashboard browser fixtures cover both languages, common-ID copy/search and installation-specific report detail/export.

The signed APKs were tested on an API 36 emulator, including upgrading old installs, preserving internal IDs and credential hashes, displaying the same common ID, missing-broker fallback, cached identity, unauthorized IPC rejection, returning to an already-open support screen and replacing APKs again. This is not physical-device or Android Auto validation. Xiaomi/Samsung background delivery, OEM installation behavior and real car playback still need device testing.

## Default-sharing release validation

112 JVM/Robolectric tests passed for this app, including seven new default/disclosure regressions and the existing manual-send, privacy allowlist, retention, identity and deletion tests. Release build and lint passed with no errors. Tests check native English/Arabic notice buttons, a saved off setting, default on gated by notice acknowledgment, persistence, failed deletion and the two-hour attempt limit. No backend schema, endpoint, quota, paid service, player behavior or notification consent was changed.

API 36 emulator validation used signed in-place upgrades, then simulated fresh support preferences while retaining installation identities. It verified disclosure on the actual phone home screen, English Turn off sharing, Arabic Continue, restart persistence and the native settings switch. Shared-ID file hashes and internal installation ID/token remained unchanged. Tests ran offline; original preferences and connectivity were restored. The complete English and Arabic dialogs were visually inspected. No live diagnostic upload or physical Android Auto/real stream test was performed for this release.
