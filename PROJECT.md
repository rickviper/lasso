# Circle Search — Project Description

Personal Android utility for Xiaomi 11i (Android 13) providing a
Circle-to-Search-style experience.

Target UX: ACTIVATE → CIRCLE → SEARCH

## Architecture (as of Phase 5)

Single-module native Android app. Tile → overlay → Confirm stores numeric
`RectF` → consent → `CaptureService` crops the settled frame (bounds +
32dp padding, clamped, min 16px) and retains it in `CropHolder` memory +
`crop_<ts>.png` (private + gallery). Zero permissions beyond the two
auto-granted FGS ones; zero dependencies; no search of any kind.

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

## Key Finding (Phase 6, investigated 2026-09-07)

No public Google Lens API exists (scraper vendors excluded: bypass rule).
Bing Visual Search was retired Aug 2025 with no Azure replacement. Remaining
legitimate options:

| Candidate | Availability | API/SDK | Auth | Cost | Submit | Android | Results | Privacy | Latency | Limits |
|---|---|---|---|---|---|---|---|---|---|---|
| Cloud Vision API | GA, REST `images:annotate` | HTTPS + API key | Key + billing project | ~$1.50/1K after 1K free/mo | base64 ≤20MB | any HTTPS | labels/objects/landmarks/logos + WEB_DETECTION (pages + similar images) | image to Google Cloud | ~1–3s | needs human key+billing setup |
| ML Kit labeling (bundled) | GA `image-labeling:17.0.9` | on-device | none | free | Bitmap | minSdk 23 ✓ | 400+ labels only, NO web | fully private | instant | +5.7MB APK; Play Svcs variant instead: +200KB, model downloads on first run |
| TinEye API | GA, REST | key (bundles) | prepaid $200/5K ($0.04/s) | expensive | URL/upload | any HTTPS | exact/near-duplicate matches only | private per ToS | ~1–2s | no semantic recognition |
| Share handoff → Lens | OS share sheet | ACTION_SEND, no SDK | none | free | content URI | all | FULL Lens results | user-controlled share | instant handoff | leaves app; needs Google app installed |

PoC (this phase): share handoff — dashboard "Visual search (share crop)"
sends the crop's MediaStore URI to the system chooser (no FileProvider, no
new dependency, no permission). Cloud Vision documented as the in-app
alternative for the Phase 7 choice (requires tester-provisioned key).

Known limitation (tester-isolated 2026-09-07): MIUI App Lock blanks the
screen when our flow foregrounds; captures then (correctly) show the lock
screen. Test on apps without app lock.

Known platform rule: MediaProjection consent is per-session by OS design —
the system dialog appears on every capture and cannot be suppressed or
pre-granted by a third-party app. All suppression paths (device-owner
provisioning, signature permissions, auto-clicking the consent UI) are
off-limits (AGENTS.md §5).

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
- Phase 4: SCREEN CAPTURE (done) — MediaProjection consent + one settled
  frame to app-private PNG + gallery copy; noHistory/no-task bugs fixed.
- Phase 5: CIRCLE → IMAGE (done) — padded clamped crop retained in memory +
  debug files; single-flight capture, no stale dashboard.
- Phase 6: VISUAL SEARCH FEASIBILITY (done) — analysis + share-handoff
  PoC (Lens works); tester chose (a) productized share handoff.
- Phase 7: FIRST COMPLETE MVP (done) — tile→circle→tap-to-search→Lens
  end-to-end on device; only necessary permissions.
- Phase 8: UX REFINEMENT (done) — visible-through-consent overlay,
  cancellable capture, auto-open Lens (~1–2s), single-flight + fallback
  hardened frame grab.
  Each phase is specified and device-validated separately per AGENTS.md.
