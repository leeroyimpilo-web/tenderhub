# TenderBase Native Offline V1 — Build Plan

**Purpose:** Produce a real native Android APK that remains usable with mobile data and Wi-Fi disabled, before making UI or feature changes. This replaces the Capacitor remote-webview approach for the new native build, not the running Render site.

## Non-negotiable requirements

1. Kotlin + Jetpack Compose + Material 3. Android activities, navigation, controls and storage are native; **no WebView, no remote HTML shell**.
2. Offline from **the first launch**: bundle a snapshot of genuine official National Treasury eTenders records into the APK and import into the local database. Do not use synthetic/dummy tenders.
3. Android local database is the sole read source for all screens. Network can refresh local records only; network failure cannot clear or hide previously downloaded notices.
4. No account/login, payments, ads, trials or separate municipal tender crawler. Sources remain the existing first-party National Treasury eTenders importer and backend.
5. Preserve live Render service, Neon, production Android package and earlier APK; work exclusively on `development/native-offline-v1` until release gates pass.
6. Do not render nonfunctional buttons or imply features are implemented before they really work.

## Implementation architecture

- **Android:** Kotlin + Jetpack Compose + Material 3, MVVM and ViewModels, Room 2.8.5 with SQLite/FTS for tenders, WorkManager 2.12.0 for constrained online refresh, DataStore for settings. Pin tested versions during implementation.
- **Module:** `native-android/` in existing tenderhub repository; independent native Gradle project and GitHub workflow. Distinct package `za.co.tenderbase.native` for parallel testing with earlier APKs.
- **Local tables:** tenders (id, reference, title, buyer, description, province, category, dates, status, source URL, updated time), tender_documents (id, tender id, URL, title, MIME, local URI), bookmarks, bid_pipeline (stage, notes, updated time), dismissed_alerts (tender ID + closing date), sync_meta.
- **Data flow:** eTenders official portal → existing importer → Neon → existing Render read API / new versioned snapshot endpoint → compressed `bootstrap.json.gz` in APK at build time → Room → native UI.
- **Freshness:** App always reads Room. On launch / manual refresh, WorkManager or repository requests updates when connected, validates official records, then atomically saves a completed batch; bookmarks, pipeline notes and downloaded documents remain intact. A failed sync leaves the last valid dataset.
- **Server enhancement:** offer stable versioned cursor/snapshot or change cursor; current paginated `/api/tenders` max 50 can be used for a first bundle (41 pages for 2,020 rows), but should not be treated as guaranteed race-free incremental synchronization. Bundle/export must strip raw source payload to keep APK size controlled.
- **Documents:** metadata and links always visible offline. User-selected PDFs are downloaded and stored in app private storage, with offline reopening through PdfRenderer/compatible viewer. An undownloaded external URL cannot be opened without internet, so label this explicitly; never claim all 6,902 document links are offline files.
- **Local state:** bookmarks, pipeline stage/notes, search/filter settings and dismissals are saved locally immediately. No credentials in the APK.
- **Alerts/calendar:** calculate deadline alerts from local dates; native ICS/calendar intent from local data; notification permission and background reminders implemented only if genuinely scheduled and tested.
- **Existing WebView app user data:** web localStorage cannot be read directly from a differently signed/native package. A later opt-in JSON export/import can migrate saved tenders and pipeline; never assume old data magically appears in native app.

## Sprint sequence and hard acceptance gates

### Sprint 0 — Native project and CI
- Create `native-android/` with separate Kotlin/Compose Gradle build, Android manifest, native launcher activity and native navigation skeleton.
- Separate `applicationId` from existing web-based APK; Internet permission only for updates; add download and storage strategies with modern scoped storage.
- GitHub Actions native-only build task producing `TenderBase-Native-Offline-debug.apk`.
- **Pass:** APK builds, installs and launches without a WebView or live website; blank-state UI must be honest (no fake data).

### Sprint 1 — Real data preloaded for first offline launch (critical)
- Build a CI export script to fetch **all** available records from Render with consistent snapshot semantics, retain title, description, reference, buyer, province, category, dates, document metadata, source URL.
- Validate every record's identifier, count, timestamps, source, no duplicates, acceptable snapshot size and freshness; package gzip asset. Fail build on missing/empty/invalid official snapshot.
- Seed Room from asset on first launch, idempotently; show source date and count in Settings.
- **Pass:** uninstall/clean-install APK with airplane mode ON, launch directly, and browse real notices without ever enabling data.

### Sprint 2 — Offline tender discovery
- Implement native Today, Find, tender details, local search (title, description, buyer, reference), province/category/buyer filters, closing status, sorting and pagination.
- Correctly show open/closed based on Android's current clock, not only imported label. Full details and documents list come from Room.
- **Pass:** all search/detail/navigation operations work in airplane mode and survive process death.

### Sprint 3 — Offline personal features
- Native Save/Unsave, saved search, pipeline stage + notes with immediate persistence, Research aggregations, closing Calendar, ICS export, in-app alerts and dismissals, DataStore preferences.
- Add backup/export/import for user-owned state if needed to migrate old installations.
- **Pass:** airplane-mode edits and reload survive force-stop/reboot; no network dependencies for these features.

### Sprint 4 — Sync and offline document library
- Manual refresh + constrained periodic WorkManager; transactional upserts, reconciliation without deleting user data, retry/backoff, last successful sync and source failure messages.
- User-chosen document download, file verification, local reopen; if not cached and offline show clear `Download when online` state.
- **Pass:** airplane-mode functional, return online to update, go offline again without losing existing data; cached PDF opens offline.

### Sprint 5 — Verification and APK acceptance
- Automated unit tests (parsers, sorting, deadline/clock, Room migrations), emulator tests, forced offline/reconnect tests, DB upgrade tests, versioned build artifacts.
- Manual phone checks on Android 15/16: install, airplane-mode cold start, find, details, saved and pipeline persistence, documents, date/time zone, background/foreground, screen rotations.
- **Pass:** commit SHA and APK digest recorded, successful CI, no crash, repeatable offline test on device. This is the baseline to freeze.

### Sprint 6 — UI and feature tuning (only after the native offline gates)
- Reproduce/improve approved TenderBase design using native Compose, then apply requested screen-by-screen improvements and additional features without breaking offline storage.
- Release changes from an isolated branch only after green native tests and explicit review.

## Explicit data and capability boundaries

- The on-device snapshot represents the **last published** official dataset, not live changes while offline.
- New tenders, amendments and real-time document URLs require connectivity to refresh.
- Government source availability/coverage may be incomplete. Always show snapshot/sync timestamp.
- No database connection string, GitHub secret or user login required inside the APK.
- Current `main` Render/Neon backend and periodic GitHub eTenders importer remain operational, unchanged by native work.

## Initial implementation checklist
- [ ] Native Gradle project compiles without Capacitor/WebView
- [ ] Verified official build-time snapshot bundled with APK
- [ ] Room seeded on first offline launch
- [ ] Native searchable real tender list + details
- [ ] Local saves and pipeline persist
- [ ] Offline research/calendar/alerts
- [ ] Online sync and document caching
- [ ] APK instrumented + physical-device airplane-mode tests
- [ ] Freeze native offline baseline before redesign
