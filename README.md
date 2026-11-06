# Lasso

**A personal, open Android utility that brings Circle-to-Search to a phone that doesn't have it.**

My Xiaomi 11i (Android 13 / MIUI) never got Google's native Circle to Search feature. Rather than wait, I built my own — a minimal, zero-dependency implementation that does one thing well:

> **ACTIVATE → LASSO → SEARCH**
>
> Pull down Quick Settings, tap the **Lasso** tile, draw a loop around anything on screen, and Google Lens opens with visual results for exactly that region.

No bloat, no accounts, no analytics, no network code of its own — **zero third-party dependencies**. The entire app is ~1,000 lines of Kotlin on top of native Android APIs, and the release APK is ~38 KB.

---

## Table of Contents

- [What It Does](#what-it-does)
- [How It Works](#how-it-works)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Permissions](#permissions)
- [Privacy & Security Model](#privacy--security-model)
- [Building](#building)
- [Installation](#installation)
- [First-Run Setup (Xiaomi / MIUI)](#first-run-setup-xiaomi--miui)
- [Using It](#using-it)
- [Known Limitations](#known-limitations)
- [Development Process](#development-process)
- [License](#license)

---

## What It Does

This exists because **Google's Circle to Search never shipped on the Xiaomi 11i** (and many other devices). Instead of a long-press on the home button/gesture bar, I wanted a trigger that actually works on this hardware — the Quick Settings tile is reliable, always available, and system-managed.

The flow is deliberately three steps:

1. **ACTIVATE** — Tap the **Lasso** Quick Settings tile from any app.
2. **LASSO** — A dimmed fullscreen overlay appears over the current app. Draw a freehand loop around anything you're curious about (smoothing keeps the stroke clean).
3. **SEARCH** — Tap **Confirm**, grant the one-time-per-session screen-capture consent, and the app:
   - grabs a single settled frame of the screen,
   - crops the lassoed region (+ a 32 dp margin, clamped to the display),
   - and hands the crop to **Google Lens** via the system share sheet — opening it **automatically** (~1–2 s, no extra taps).

That's the whole app. It is intentionally small: a reliable personal tool, not a platform.

### Highlights
- **Accurate crops** — selection bounds are padded, clamped to screen edges, and tiny accidental taps are skipped gracefully.
- **Robust frame grabbing** — settle delay + skip/blank-frame detection so you never search a black transition frame; a held fallback frame covers static screens.
- **Gallery stays clean** — captures and crops live in app-private storage and are shared via a read-only `ContentProvider`. Nothing is ever published to MediaStore.
- **Auto-cleanup** — each new capture prunes the previous run's private files; cancel/fail paths write nothing.
- **Single-flight capture** — double-taps and re-entry can't spawn duplicate captures.

---

## How It Works

The end-to-end flow, step by step:

```
┌────────────┐   tap    ┌─────────────────┐  draw +  ┌──────────────────┐
│ QS Tile    │ ───────► │ OverlayActivity │  Confirm │ MediaProjection  │
│ (ProbeTile)│          │ (dimmed overlay,│ ───────► │ consent dialog   │
└────────────┘          │  SelectionView) │          │ (system, per-    │
                        └─────────────────┘          │  session by OS)  │
                                                     └────────┬─────────┘
                                                              │ grant
                                                              ▼
                        ┌─────────────────┐  crop URI ┌────────────────────┐
                        │ ShareHelper /   │ ◄──────── │ CaptureService     │
                        │ ShareActivity   │           │ (FGS mediaProjection:
                        └────────┬────────┘           │  1 settled frame → │
                                 │                    │  PNG + crop)       │
                                 ▼                    └────────────────────┘
                        ┌─────────────────┐
                        │ Google Lens     │
                        │ (share handoff) │
                        └─────────────────┘
```

1. **Tile tap** → `ProbeTile` counts the activation and launches `OverlayActivity` as a fresh task (`NEW_TASK | CLEAR_TASK`) so it always layers over the foreground app and never resurrects a stale task (a real MIUI quirk).
2. **Lasso → Confirm** → the overlay asks the system for screen-capture consent. The overlay **stays visible behind the consent dialog** (no dead gap); denying simply returns you to the overlay to redraw.
3. **Grant** → the consent result is handed to `CaptureService`, a foreground service of type `mediaProjection` (mandatory on targetSdk 29+). The overlay hides itself *first*, so the capture contains only the underlying app.
4. **One frame** → the service mirrors the **physical display** (its real metrics and rotation — not the overlay window's) into an `ImageReader`, waits ~600 ms for transitions to settle, skips the first frames, rejects all-black placeholder buffers, and saves the first good frame as PNG. It then crops the lassoed bounds and releases every resource (display, projection, reader).
5. **Search** → the crop is shared read-only through `CropProvider`. `ShareHelper` tries the Google app's Lens share target **directly** (no chooser friction) and falls back to the system chooser. A "Lasso ready" notification also appears as a tap-to-search fallback, and the dashboard button can re-share the last crop anytime.

---

## Architecture

Single Gradle module (`app`), no other modules, no content-graph complexity:

| File | Role |
|---|---|
| `ProbeTile.kt` | The sole trigger: `TileService.onClick` → `Activation`, then collapses QS into the overlay. Survives reboots and swipe-kills (system-managed). |
| `Activation.kt` | Centralized trigger + `ProbeState` (activation counts, last selection/capture/crop) + `CropHolder` (single-slot in-memory crop). |
| `OverlayActivity.kt` | Dimmed fullscreen selection surface. Cancel / Confirm buttons, MediaProjection consent hand-off, invisible-wait for the capture result, 12 s timeout guard. |
| `SelectionView.kt` | Custom `View`: single-finger freehand path with quadratic midpoint smoothing; stroke persists after release; a new touch replaces the selection. |
| `CaptureService.kt` | The capture worker: FGS type `mediaProjection`, one settled frame via `ImageReader` + `VirtualDisplay`, blank-frame rejection, padded/clamped crop, temp-file pruning, tap-to-search notification. Always releases projection/display/reader. |
| `ShareActivity.kt` | Single entry point for visual search (notification tap or dashboard button). Cold-start safe — falls back to the newest private crop file. |
| `ShareHelper.kt` | Opens the search: direct Google-app Lens target when resolvable, system chooser otherwise. |
| `CropProvider.kt` | Read-only `ContentProvider` serving app-private captures/crops to the share sheet. Path-traversal guarded, never publishes to MediaStore. |
| `MainActivity.kt` | Diagnostics dashboard: activation counts, last selection/capture/crop, files on disk; requests `POST_NOTIFICATIONS` on Android 13+. |

Key design constraints honored throughout:

- **No accessibility service** (tried and dropped — see below), **no overlay permission** (translucent activity instead), **no storage permission** (own private files on API 29+ need none).
- Every fallible boundary is wrapped in try/catch; feedback (toasts/notifications) can never crash the flow.
- Consent is per-session **by OS design** and is never suppressed, pre-granted, or auto-clicked.

---

## Tech Stack

| | |
|---|---|
| Language | Kotlin 1.9.24 (JVM 17) |
| UI | Classic `Activity` + XML layouts (no Compose — zero dependencies) |
| Capture | `MediaProjection` + `ImageReader` + `VirtualDisplay` |
| Build | AGP 8.5.2, Gradle 8.7 (wrapper) |
| SDK | compileSdk 34, targetSdk 34, minSdk 26 |
| Dependencies | **None** (no AndroidX, no libraries — `Activity` and platform APIs only) |
| Minify | R8 + resource shrinking in release |

---

## Permissions

Exactly three, all justified:

| Permission | Why |
|---|---|
| `FOREGROUND_SERVICE` | Required to run the capture as a foreground service. |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Required since targetSdk 29+: `getMediaProjection` must be called inside a foreground service of type `mediaProjection`. |
| `POST_NOTIFICATIONS` | Android 13+ runtime permission so the "tap to search" notice can appear. The FGS exemption covers only the service's own notice, not content notifications. |

The screen-capture consent dialog is the system's own and appears **every capture session** — that's Android working as designed, and this app deliberately doesn't fight it.

---

## Privacy & Security Model

- **Your images stay yours.** Captures and crops are written only to app-private storage (`filesDir/captures/`). Nothing is uploaded by this app — it has no networking code at all. When you tap search, the crop is handed to the Google app and processed by Google Lens under Google's terms, exactly as if you had shared a screenshot yourself.
- **No gallery pollution.** A `ContentProvider` serves the crop read-only to the share sheet; nothing touches MediaStore. Old runs are pruned automatically.
- **No tracking.** No analytics, no telemetry, no identifiers, no crash reporting. ~40 log lines exist purely for personal diagnostics and contain no PII.
- **No security bypasses.** No permission circumvention, no consent automation, no DRM/secure-content workarounds. Secure surfaces (e.g. DRM video) legitimately capture black; locked apps capture their lock screen — by design, and the app treats both gracefully.

---

## Building

Requirements: JDK 17, Android SDK 34 (set via `ANDROID_HOME`/`local.properties`).

```bash
# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release APK (R8 minified + shrunk)
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

Release signing expects four properties in `local.properties` (gitignored):

```properties
releaseKeystore=<path to .keystore>
releaseKeystorePassword=<store password>
releaseKeyAlias=<key alias>
releaseKeyPassword=<key password>
```

> Keep the keystore backed up — losing it means uninstall + fresh install for every update.

---

## Installation

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the app once after install and **allow notifications** when prompted (Android 13 resets this on reinstall).

---

## First-Run Setup (Xiaomi / MIUI)

Tested recipe for the Xiaomi 11i (Android 13); other devices need at most the first two steps:

1. **Add the tile** — swipe down → edit tiles → move **Lasso** into the active area. It persists across reboots and survives swipe-kills (it's system-driven and self-revives).
2. **Allow notifications** — App info → Notifications → **Allow** (required for tap-to-search; everything else works without it).
3. **Battery saver: No restrictions** (App info → Battery saver) — recommended so MIUI never defers the capture service.
4. **Nothing else.** No autostart option is needed (no background work), no overlay/"display pop-up in background" permission (the app holds none), no "Allow restricted settings" (no accessibility service).

---

## Using It

1. Open any app and find something you want to search.
2. Pull down Quick Settings → tap **Lasso**.
3. The screen dims — draw a loop around the thing (a new touch replaces the old stroke; **Cancel** or Back exits).
4. Tap **Confirm** → grant screen capture (**Start now**).
5. Lens opens on your crop automatically in ~1–2 s. Alternatively tap the **"Lasso ready"** notification, or use the dashboard's **Visual search (share crop)** button for the last crop.

---

## Known Limitations

- **Consent dialog every session** — per-session by Android design; not suppressible by any legitimate means.
- **Apps with MIUI App Lock** — App Lock blanks the screen when the flow foregrounds, so the capture (correctly) shows the lock screen. Test on apps without app lock.
- **DRM / FLAG_SECURE content** (protected video, etc.) — captures black by OS design. The app detects blank frames and fails gracefully rather than searching black.
- **Requires the Google app** for Lens results (share handoff); without it the system chooser offers whatever image-search targets exist.
- **Landscape works**, but the drawing UI is portrait-first.

---

## Development Process

This started as a **personal project out of frustration** — my Xiaomi 11i didn't have Circle to Search, and I wanted it. I built it incrementally, one phase at a time, under a strict methodology recorded in [`AGENTS.md`](AGENTS.md): every phase required a successful build, an install on the physical Xiaomi 11i, explicit acceptance tests executed by a human tester (me), and a `phase-X-complete` git checkpoint. Compilation alone never counted as done. The goal was never to demonstrate sophisticated Android engineering — the goal was a **reliable personal utility** that works on *this* device.

| Phase | Scope | Result |
|---|---|---|
| 0 | Scaffold | Built, installed, launched ✓ |
| 1 | Fingerprint-gesture feasibility | **Closed: NOT AVAILABLE** on the Xiaomi 11i (`FingerprintGestureController` not exposed; side-mounted sensor) |
| 2 | Global trigger | Probes device-tested: volume keys ✗, power ✗, overlay dot unproven, **QS tile ✓**. Accessibility shortcut worked but MIUI refuses to rebind it after process kill — dropped. Tile became the sole trigger. |
| 3 | Selection overlay | Dimmed overlay layered over the foreground app (`taskAffinity=""`), freehand circle ✓ |
| 4 | Screen capture | Consent → FGS capture; fixed `noHistory` kill, SecurityException (missing FGS type), task-hijack, invisible private files, black frames (settle + skip + blank-check) |
| 5 | Circle → image | Padded/clamped crop, single-flight capture, honest failure states |
| 6 | Visual-search feasibility | No public Lens API exists; Bing Visual Search retired (Aug 2025); Cloud Vision / ML Kit / TinEye evaluated → **share handoff to Lens chosen** (free, full results, user-controlled) |
| 7 | First complete MVP | Tile → circle → tap-to-search → Lens end-to-end on device |
| 8 | UX refinement | Visible-through-consent overlay, cancellable capture, auto-open Lens (~1–2 s), fallback frame grab |
| 9 | Destructive testing | R1–R12 suite (rapid taps, rotations, kills, reboots, DRM apps…) — all green |
| 10 | MIUI hardening | Settings recipe + M1–M10 suite (reboot cold, doze, DND, reinstall…) — all green |
| 11 | Daily-driver release | Audited, signed, R8-minified `1.0.0`; post-release fix to keep captures out of the Gallery → **`1.0.1`** |

Two honest dead ends are preserved in the docs because they're genuinely useful findings for anyone building similar tools on MIUI: **fingerprint swipe gestures are not exposed to third-party apps on this device**, and **MIUI does not rebind accessibility services after the app process is killed**, which killed the accessibility-shortcut trigger despite it working initially.

---

## License

Personal project, shared as-is for educational/reference use. No warranty. If you fork it, mind your local privacy laws and Google's terms when handing images to Lens.
