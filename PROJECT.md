# Circle Search — Project Description

Personal Android utility for Xiaomi 11i (Android 13) providing a
Circle-to-Search-style experience.

Target UX: ACTIVATE → CIRCLE → SEARCH

## Architecture (as of Phase 1)

Single-module native Android app. No backend, no libraries, no capture,
no overlay, no search yet.

- `app/` — sole Android application module.
  - `src/main/AndroidManifest.xml` — manifest; LAUNCHER `MainActivity` +
    `GestureService` (bound with `BIND_ACCESSIBILITY_SERVICE`).
  - `src/main/java/com/ullas/circlesearch/GestureService.kt` — Phase 1 probe:
    `AccessibilityService` requesting fingerprint-gesture capability; reads
    `FingerprintGestureController.isGestureDetectionAvailable`, registers a
    gesture callback when available, records results in `ProbeState` and
    `Log.i` (`CircleSearchProbe` tag). Processes no accessibility events.
  - `src/main/java/com/ullas/circlesearch/MainActivity.kt` — probe screen:
    shows service-enabled state, gesture availability, last gesture and
    per-direction counts (refreshed in `onResume` / on demand); button opens
    Accessibility Settings.
  - `src/main/res/` — `layout/activity_main.xml`, `values/strings.xml`,
    `xml/accessibilityservice.xml` (`canRequestFingerprintGestures="true"`).
- Root Gradle files: `settings.gradle`, `build.gradle`, `gradle.properties`,
  Gradle wrapper (`gradlew`, `gradle/wrapper/`).

## Key Finding (Phase 1, device-verified 2026-09-07)

The Xiaomi 11i does NOT expose fingerprint swipe gestures to third-party
accessibility services (`FingerprintGestureController`
reports not-available; consistent with its side-mounted power-button sensor,
which supports no swipe gestures). Fingerprint-sensor swipe therefore cannot
be the ACTIVATE trigger — a later phase must specify an alternative (e.g.
accessibility shortcut/button, quick tile, floating trigger, or hardware-key
activation).

## Toolchain (Phase 0)

- AGP 8.5.2, Gradle 8.7 (wrapper), Kotlin 1.9.24
- compileSdk 34, minSdk 26, targetSdk 34
- applicationId `com.ullas.circlesearch`
- Zero external dependencies.

## Roadmap

- Phase 0: scaffold + debug APK installs/launches on Xiaomi 11i (done).
- Phase 1: fingerprint-gesture feasibility probe (current).
- Future phases (not implemented): activation trigger, overlay selection UI,
  region extraction, visual search, results presentation. Each phase is
  specified and device-validated separately per AGENTS.md.
