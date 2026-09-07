# Circle Search — Project Description

Personal Android utility for Xiaomi 11i (Android 13) providing a
Circle-to-Search-style experience.

Target UX: ACTIVATE → CIRCLE → SEARCH

## Architecture (as of Phase 4)

Single-module native Android app. Tile tap → `Activation` → translucent
`OverlayActivity` (separate task) → Confirm hides the UI, takes the system
MediaProjection consent, captures one frame to app-private storage.
Zero permissions, zero dependencies.

- `OverlayActivity.kt` — overlay + consent (`createScreenCaptureIntent`) +
  hands the grant to `CaptureService`; deny restores the overlay.
- `CaptureService.kt` — `mediaProjection`-type foreground service (required
  for `getMediaProjection` at targetSdk 29+): one frame via `ImageReader` +
  `VirtualDisplay` on a bg thread → PNG in `filesDir/captures/`, then stops.
  Projection/display/reader always released.
- Rest unchanged: `Activation`/`ProbeState` (+ `lastCapture`), `ProbeTile`,
  `SelectionView`, dashboard (+ last capture).

- `Activation.kt` — centralized trigger + `ProbeState` (counts, last selection).
- `ProbeTile.kt` — sole trigger: `TileService.onClick` → `Activation`, then
  collapses QS into the overlay.
- `OverlayActivity.kt` + `activity_overlay.xml` — dimmed surface, Cancel /
  Confirm buttons; Confirm logs selection bounds (no capture/search yet).
- `SelectionView.kt` — single-finger freehand path with quadratic smoothing;
  stroke persists after release; new touch replaces the selection.
- `MainActivity.kt` — dashboard: tile count, last activation, last selection.

- `Activation.kt` — centralized trigger + `ProbeState` counts.
- `ProbeTile.kt` — `TileService.onClick` → `Activation`, then collapses QS
  into the overlay.
- `MainActivity.kt` — dashboard: tile activation count, last activation,
  last selection bounds.
- No permissions. No accessibility service (dropped with the shortcut).
- Dropped and removed: fingerprint/volume/power/overlay-dot/shortcut paths.

- `app/` — sole Android application module.
  - `src/main/AndroidManifest.xml` — LAUNCHER `MainActivity` + `ProbeTile`
    (bound with `BIND_QUICK_SETTINGS_TILE`). No permissions requested.
  - `src/main/res/` — `layout/activity_main.xml`, `values/strings.xml`,
    `drawable/ic_trigger_dot.xml` (tile icon).
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

## Chosen Activation (tester decision 2026-09-07, revised same day)

- Sole trigger: Quick Settings tile (proven across installs and reboots).
- Accessibility shortcut DITCHED on tester instruction: MIUI does not rebind
  the service after the app process is killed (`enabled: true` but bound
  `false` despite No-restrictions battery, locked app, no autostart option).
  Service and all a11y code removed.
- Dropped: fingerprint gestures, volume keys, power button, overlay dot,
  accessibility shortcut.

## Toolchain (Phase 0)

- AGP 8.5.2, Gradle 8.7 (wrapper), Kotlin 1.9.24
- compileSdk 34, minSdk 26, targetSdk 34
- applicationId `com.ullas.circlesearch`
- Zero external dependencies.

## Roadmap

- Phase 0: scaffold + debug APK installs/launches on Xiaomi 11i (done).
- Phase 1: fingerprint-gesture feasibility probe (closed: NOT AVAILABLE).
- Phase 2: GLOBAL TRIGGER (done) — QS tile wired to centralized `Activation`.
- Phase 3: SELECTION OVERLAY (done) — tile opens dimmed fullscreen freehand
  circle UI (separate task) with Cancel/Confirm; bounds logged on confirm.
- Phase 4: SCREEN CAPTURE (current) — MediaProjection consent + one frame to
  app-private PNG; tester validates on-device before closure.
- Future phases (not implemented): region cropping, visual search, results
  presentation. Each phase is specified and device-validated separately
  per AGENTS.md.
