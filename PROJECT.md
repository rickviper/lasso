# Circle Search — Project Description

Personal Android utility for Xiaomi 11i (Android 13) providing a
Circle-to-Search-style experience.

Target UX: ACTIVATE → CIRCLE → SEARCH

## Architecture (as of Phase 2 completion spec)

Single-module native Android app. Both triggers call one centralized
`Activation.trigger(context, source)` (log + per-source count + Toast; no
overlay, capture, cropping, or search).

- `Activation.kt` — the single activation function both triggers call.
- `GestureService.kt` — primary trigger: accessibility-shortcut taps via
  `AccessibilityButtonController` → `Activation`; keeps the Phase 1
  fingerprint-availability readout. No event processing, no key observation.
- `ProbeTile.kt` — secondary trigger: `TileService.onClick` → `Activation`,
  then collapses QS into the dashboard.
- `MainActivity.kt` — dashboard: service state, per-source activation counts,
  last activation.
- Dropped and removed: volume-key observer, overlay dot service + permissions.

- `app/` — sole Android application module.
  - `src/main/AndroidManifest.xml` — LAUNCHER `MainActivity`, `GestureService`
    (bound with `BIND_ACCESSIBILITY_SERVICE`), `ProbeTile` (bound with
    `BIND_QUICK_SETTINGS_TILE`). No special permissions.
  - `src/main/res/` — `layout/activity_main.xml`, `values/strings.xml`,
    `xml/accessibilityservice.xml` (`flagRequestAccessibilityButton` +
    `canRequestFingerprintGestures`), `drawable/ic_trigger_dot.xml` (tile icon).
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

## Key Finding (Phase 2 Round 1, device-verified 2026-09-07)

- Volume keys: NOT delivered to third-party accessibility services on this
  MIUI build — ruled out.
- Power button: not interceptable by third parties (analysis) — ruled out.
- Accessibility shortcut/button: WORKS (49 probe clicks via the floating
  shortcut button). Leading candidate.
- Quick Settings tile: WORKS (2 probe taps). Solid fallback.
- Overlay dot: never appeared despite permission granted — unproven on MIUI,
  deprioritized.

## Chosen Activation (tester decision 2026-09-07, option c)

- Primary: accessibility shortcut/button (proven: 49 probe clicks).
- Fallback: Quick Settings tile (proven: 2 probe taps).
- Dropped: volume keys, power button, fingerprint gestures, overlay dot.

## Toolchain (Phase 0)

- AGP 8.5.2, Gradle 8.7 (wrapper), Kotlin 1.9.24
- compileSdk 34, minSdk 26, targetSdk 34
- applicationId `com.ullas.circlesearch`
- Zero external dependencies.

## Roadmap

- Phase 0: scaffold + debug APK installs/launches on Xiaomi 11i (done).
- Phase 1: fingerprint-gesture feasibility probe (closed: NOT AVAILABLE).
- Phase 2: GLOBAL TRIGGER (current) — accessibility shortcut (primary) +
  QS tile (secondary) wired to centralized `Activation`; tester validates
  on-device before closure.
- Future phases (not implemented): activation trigger, overlay selection UI,
  region extraction, visual search, results presentation. Each phase is
  specified and device-validated separately per AGENTS.md.
