# TenderBase V2 — TenderHub

A new open-access South African tender intelligence MVP, using **only official National Treasury eTenders OCDS releases**. No account, authentication, payments, subscriptions, or separate municipal crawling.

## Implemented source modules
- Original TenderBase navy/Poppins/Tailwind visual tokens and responsive navigation.
- Server-backed Today, Find (real SQL filters + pagination), Research, tender details and documents.
- Local device watchlist, bid pipeline notes/stages, calendar file export, in-app deadline alerts and preference storage.
- PostgreSQL SQL schema, paginated official OCDS ingestion, source health endpoint, and normalization unit tests.

**Important: these modules are committed but have not yet been verified in a deployed environment. No live tender feed or installable Android APK is claimed.** An actual DATABASE_URL, database initialization, successful official OCDS sync, CI checks, and live deployment are required before release.

## Requirements
Node 20+, a PostgreSQL database (Neon is supported), and network access to the official eTenders API.

## Setup
1. `npm install`
2. Copy `.env.example` to `.env.local`, then set `DATABASE_URL`. Do not commit credentials.
3. `npm run db:init` (loads the SQL schema)
4. `npm run sync:etenders` (7-day release window); use `npm run sync:30d` for a larger initial backfill.
5. If the live official API fails, download official monthly OCDS JSON from https://data.etenders.gov.za/Home/ReleasesFiles, extract the JSON or JSONL file and run `npm run import:official -- /path/to/download.json` (also supports `.gz`). This imports genuine official records only.
6. `npm run dev` to run the public web interface.
7. `npm run typecheck`, `npm run test:ocds`, and `npm run build` to validate.

For deployment, use Render (Node web app) and Neon (PostgreSQL). Run the importer as an isolated scheduled job and monitor `sync_runs` failures. The UI never substitutes mock tenders if the official feed is unavailable.

## Key URLs
- `/` — Today
- `/search` — Find
- `/research` — Research
- `/tenders/[ocid]` — Full notice
- `/saved` — Local watchlist
- `/pipeline` — Local bid pipeline
- `/calendar` — Closing deadlines, ICS downloads
- `/alerts` — On-launch in-app alerts for saved deadlines
- `/settings` — Preferences and source status
- `/api/health`, `/api/tenders` — Server APIs

## Sources and limitations
Official publication: https://data.etenders.gov.za/Home/LearnMore

Official first-party opportunities endpoint (confirmed HTTP 200 on 4 November 2026): `https://www.etenders.gov.za/Home/PaginatedTenderOpportunities`. This is the same National Treasury eTenders portal, not separate municipal discovery or a commercial listing. Only currently advertised tenders are returned, so historical data still relies on OCDS/bulk files.

Original OCDS importer endpoint: `https://ocds-api.etenders.gov.za/api/OCDSReleases`; the official Swagger page returned HTTP 404, and a live API probe on 4 November 2026 returned HTTP 500 for multiple valid date windows (HTTP 400 without mandatory dates). The official bulk JSON import is the fallback; neither source has been successfully imported into a configured database yet. The dataset does not include every government procurement process. Fields absent in source data are labeled as not supplied. Documents link to URLs in official releases; missing URLs cannot be conjured.

In-app alert reminders appear upon opening the app; **background Android notifications are not implemented**. This is intentional rather than a fake notification toggle. APK packaging and device testing remain future work.

## Next release gates
- [ ] GitHub CI passes on the committed tree.
- [ ] Database provisioned and schema initialized.
- [ ] Official eTenders importer verified against live API and initial 30-day backfill completed.
- [ ] End-to-end browser tests on real imported records.
- [ ] Hosting deployment and data sync schedule enabled.
- [ ] Capacitor native Android packaging, notification support and APK smoke tests.

No dummy data, demo buttons, paywalls, local municipal scrapers, or authentication.
