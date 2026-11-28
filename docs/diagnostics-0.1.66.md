# 0.1.66 — official diagnostics update

Official release of the latest car-player improvements: synchronized YouTube fullscreen exit, a larger native seek bar with time labels and ±10-second buttons, and the existing call-interruption resume handling. The native seek bar does not generate image thumbnails.

Removes the obsolete missing-Maps-card diagnostic marker. Adds content-free background-audio, interruption and resume-intent observations, safe playback counters and startup version to support reports. Report batches stay bounded; URLs, account data and watched titles are never included. Automatic-sharing preferences are preserved. Compatible Supabase ingestion and bilingual dashboard deployed.

Validation: Android build, unit tests and lint passed. Dashboard tests/build passed. Live ingestion rejects unsafe fields; persistence of a new synthetic identity could not be tested because the existing daily registration quota is full (HTTP 429). No physical phone/car was connected for this release.

إصدار رسمي: إزالة زر تسجيل مشكلة كارت الخرائط وإضافة تفاصيل آمنة للتشخيص، مع الاحتفاظ بإعدادات المستخدم.

Protocol additions (schema 1): playback-context uses closed states; media adds typed booleans (visible, muted, resumeWanted, fullscreen), counters (waiting, stalled, loads), and source/engine enums. These observations do not prove audible sound, Maps-card visibility or a call participant. Existing history remains readable. Startup events include the version active at capture; older events may lack it. Latest report events are capped at 24 KiB, within the ingestion body limit.
