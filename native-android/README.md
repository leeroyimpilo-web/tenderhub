# TenderBase Native Offline APK

Kotlin, Jetpack Compose and Room database. No WebView. No internet permission is needed to launch, browse, filter, read or bookmark tender data.

CI downloads the full official National Treasury eTenders snapshot from the existing Render API, validates it, compresses it into the APK, and builds native code. On a fresh install, Room imports the bundle as an atomic transaction. All screens read local Room data only.

Native functionality currently: Today, searchable Find, province/category/status filters, sorting, tender details with official document metadata, offline bookmarks, snapshot freshness.

The first APK does NOT support online data refresh, PDF offline caching, bid pipeline or research yet. Those are future sprints; no fake buttons.

Build using JDK 17 + Gradle 8.9 + SDK 35. Run `node scripts/export-native-snapshot.mjs` from repo root and `gradle :app:assembleDebug` from this folder. The build fails if real official data isn't present.
