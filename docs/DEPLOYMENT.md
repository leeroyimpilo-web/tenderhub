# TenderBase V2 — Finish live deployment and Android QA

This project is an **open-access, no-login, no-payment** MVP. It uses official National Treasury eTenders only.

## Completed and verified
- Dedicated Neon project: `tenderhub-v2`; database: `tenderhub`.
- 2,020 unique official opportunities imported on 4 November 2026.
- 6,902 official document links present; official PDF download probe returned HTTP 200.
- Next.js TypeScript/build and official-source parser tests passed.
- Playwright end-to-end test passed with a temporary database seeded from real official opportunities: [run 41892830838](https://github.com/leeroyimpilo-web/tenderhub/actions/runs/41892830838).

## Deploy the web backend (required for APK)
1. Open Render Blueprint: https://dashboard.render.com/blueprint/new?repo=https://github.com/leeroyimpilo-web/tenderhub
2. Sign in to Render, select your intended workspace, and authorize GitHub repository access if requested. **Do not reuse or modify existing services.**
3. Review the single new web service named `tenderhub-v2` with the `free` plan and Frankfurt region. No Render Postgres service is required because Neon already exists.
4. For the mandatory `DATABASE_URL` secret, open Neon Console, project `tenderhub-v2`, database `tenderhub`, and copy the **pooled** connection URL into Render's secret field. Never commit/paste this URL into GitHub source or chat.
5. Apply the Blueprint. Check the deployed app and `/api/health`. Expected: `ok=true`, `count` greater than 0 and a non-null lastSync. Render's health path is `/api/health` so deployment will fail rather than claiming success if it cannot access the database.
6. In the app, manually test navigation, Find, detail PDF download, Saved, Pipeline, Research, Calendar, settings and alerts.

## Configure automatic eTenders refresh (required before release)
1. Copy a Neon pooled connection string **directly** from Neon to the GitHub UI under `Settings > Secrets and variables > Actions > New repository secret`.
2. Create the name `TENDERHUB_DATABASE_URL`, then paste the pooled Neon URL as the value (do not put it in a repository file).
3. GitHub Actions: open `Sync official National Treasury opportunities` and use `Run workflow` to test. Check `sync_runs` after the run and verify fresh data appears.
4. The scheduled workflow then runs every 6 hours. It only uses the official National Treasury eTenders first-party portal; no municipal scraping or third-party sources.
5. The OCDS releases API was returning HTTP 500 on 4 November 2026; this should not block the working first-party opportunities import. The offline bulk OCDS import remains a separate fallback.

## Produce an installable Android test APK
1. After the Render site is live and its `/api/health` returns real records, open GitHub Actions > `Build TenderBase Android APK` > Run workflow.
2. Supply your **real Render HTTPS URL** as `app_url`.
3. The workflow rejects unconfigured, unencrypted, unavailable or empty demo hosts; then generates the Capacitor Android project, builds a debug APK, and uploads `TenderBase-v2-Android-debug` as an Actions artifact.
4. Download the artifact from the completed workflow and install the `app-debug.apk` on an Android device. This is a **debug test APK**, not a signed Google Play release.
5. Check back navigation, PDF downloads, notifications, settings and local storage persistence on the actual device. Do not distribute as production until those tests pass.

## Current limitations
- The app requires internet access for tender discovery and research. Saved bid metadata and pipeline notes are local.
- Alerts are checked in-app. Background Android notifications are **not yet implemented** and the UI must not imply otherwise.
- No automatic payment, trial, login or account code is present.
- APK generation requires a working hosted Next.js backend because server-side database queries cannot run from a static APK.
