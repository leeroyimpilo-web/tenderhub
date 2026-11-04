# TenderBase V2 · Open eTenders MVP

TenderBase is a South African, login-free tender intelligence app. This is a **new implementation**, not a production release yet.

## Locked scope
- Preserve the previous TenderBase visual language: navy gradient Today page, Poppins typography, clean white cards, mobile bottom navigation and desktop sidebar.
- Only National Treasury eTenders **OCDS** data; no separate municipal or commercial discovery.
- Open without registration or payment. No auth, billing, subscriptions, plans, trial or paywalls.
- All shipped controls must be functional. Never ship mock tenders, demo actions or inactive buttons.
- Features: Today, Find, Research, tender details and documents, Saved, Pipeline, deadline calendar, alerts/reminders, settings and source-health display.
- Save personal app state locally to the device. Don't publish personal user data to a server.

## Architecture
Next.js + TypeScript + Tailwind CSS front end, Node ingestion job, PostgreSQL (Neon) for shared official tender data, Capacitor Android packaging later. Front-end should tolerate server unavailability with honest error states, never fabricated records.

## Source of truth
National Treasury: https://data.etenders.gov.za/ (OCDS API & bulk downloads).
The API endpoint `https://ocds-api.etenders.gov.za/api/OCDSReleases` must be verified with live tests before declaring live synchronization complete.

## Status
- [x] Fresh GitHub repository initialized
- [ ] UI ported and typechecked
- [ ] Database migrations and OCDS sync tested against official releases
- [ ] Search, detail, research, local state and reminders tested
- [ ] Web deployment verified
- [ ] Android APK built and smoke-tested

**No functional or production readiness claims until each step has been verified.**
