## What changed

The phone now has Support & diagnostics: copy a random installation Support ID, send a technical report with consent, or opt into periodic automatic reports. No session or file-part selection is needed for normal reporting. Privacy details, deletion/reset, simple manual sharing and advanced local sessions remain available.

The shared developer-owned Supabase service is live on the free plan. Reports use a closed data vocabulary: software/device environment, playback counters and error categories. No raw logs, content titles/IDs, searches, stream/provider URLs, credentials, cookies or account data are uploaded. Automatic sending is off by default. Each app has a separate installation ID, not a person/hardware ID. IP addresses are necessarily handled by the hosting provider but are not stored in diagnostic reports.

Delivery is approximately every two hours, subject to Android scheduling/network restrictions. Reports are retained for approximately 30 days or less under storage limits. Free-plan quotas and conservative server budgets apply. Sending can fail or be delayed; the UI reports whether a manual upload actually arrived. Existing local diagnostic sessions keep their previous retention and are never automatically uploaded.

## Validation

- Java/Robolectric regression suites and release Android Lint passed, including actual support-screen buttons, consent cancel/accept, default-off/manual/automatic behavior, opt-out, deletion success/failure, closed privacy vocabulary, and bounded history.
- Backend validator tests passed. Live HTTPS tests covered ingestion, idempotency, credentials, forbidden fields, body size, owned deletion and admin lookup. SQL checks covered retention, daily budget and denied client read/RPC access. Security advisors report only the expected RLS-with-no-public-policy informational notices.
- Android API 36 / WebView 133 emulator: both apps sent reports via their real phone UI to Supabase, with matching support IDs and delivery confirmation. No IPTV account was provisioned.
- No physical Xiaomi/Samsung or Android Auto validation was performed for this release. Long-term OEM background delivery remains a device test. Playback implementation and phone/controller separation are unchanged.

Install over your existing app to preserve its data. Open Support & diagnostics from Settings (IPTV) or More (Tube); try Send report now, then give the developer your copied Support ID and the approximate problem time. Automatic sharing requires a separate opt-in.

Full regression counts: IPTV 96 Java tests; Tube 101 Java tests; 4 backend validator tests. Targeted support-flow tests were rerun after strengthening automatic-send assertions. Device metadata is captured at report time; it can differ from the environment of older retained events.
