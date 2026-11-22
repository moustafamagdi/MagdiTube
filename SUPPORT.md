# Support & diagnostics

On the phone, open **Settings → Support & diagnostics** (IPTV) or **More → Support & diagnostics** (Tube).

- **Send report now** asks consent and sends a small technical report without choosing sessions or parts.
- **Copy support ID** gives the developer a random installation ID to look up. Send the approximate problem time too. Each application has its own ID.
- **Automatically send diagnostics** is off until you explicitly enable it. Network/battery restrictions can delay delivery.
- **Privacy & retention** explains the fields, Supabase recipient/location, 30-day server retention and limits.
- **Delete sent reports & reset ID** stops sharing, deletes the server reports for this installation, then rotates its ID on success. Requires internet; failed deletion preserves the old ID for retry.
- **Share a report manually / Copy report** provides a simple fallback containing the last 24 hours / up to 256 safe events.
- **Advanced: local log sessions** preserves complete historical local copy/export and the existing 40-part/7-day rules during rollout. These raw log files are never uploaded automatically.

No user account is needed. Reports do not contain watched titles, search queries, URLs, provider credentials, passwords, cookies, account details, location or hardware identifiers. Supabase receives IP addresses to serve requests; diagnostic tables do not store them. This is pseudonymous technical reporting, not a claim of perfect anonymity. Automatic batches contain at most 128 events and are attempted approximately every two hours; some older local events may be omitted under volume/retention limits.

Developer inbox: https://supabase.com/dashboard/project/hncaagdpxhbysdcdwuoa/explorer/notebook/fea19f9e-0f9a-4975-b690-ea13452e8e3c

Backend implementation, quotas and owner instructions: [shared service](https://github.com/moustafamagdi/MagdiIPTV/tree/main/support-backend). This release does not change playback routing, package identity, signing key or existing accounts.
