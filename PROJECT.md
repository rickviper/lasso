# Circle Search — Project Description

Personal Android utility for Xiaomi 11i (Android 13) providing a
Circle-to-Search-style experience.

Target UX: ACTIVATE → CIRCLE → SEARCH

## Architecture (as of Phase 0)

Single-module native Android app. No backend, no libraries, no services yet.

- `app/` — sole Android application module.
  - `src/main/AndroidManifest.xml` — manifest; single LAUNCHER `MainActivity`.
  - `src/main/java/com/ullas/circlesearch/MainActivity.kt` — plain
    `android.app.Activity`, sets the Phase 0 layout. No AppCompat/Material
    dependency (uses the platform Material theme).
  - `src/main/res/` — `layout/activity_main.xml`, `values/strings.xml`.
- Root Gradle files: `settings.gradle`, `build.gradle`, `gradle.properties`,
  Gradle wrapper (`gradlew`, `gradle/wrapper/`).

## Toolchain (Phase 0)

- AGP 8.5.2, Gradle 8.7 (wrapper), Kotlin 1.9.24
- compileSdk 34, minSdk 26, targetSdk 34
- applicationId `com.ullas.circlesearch`
- Zero external dependencies.

## Roadmap

- Phase 0: scaffold + debug APK installs/launches on Xiaomi 11i (current).
- Future phases (not implemented): activation trigger, overlay selection UI,
  region extraction, visual search, results presentation. Each phase is
  specified and device-validated separately per AGENTS.md.
