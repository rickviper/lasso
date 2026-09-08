# Circle Search — Project Status

## Current Phase

All work complete and device-validated. Daily driver: signed `1.0.1`
(v24) installed from release APK, E2E verified on Xiaomi 11i.

## Post-Phase-11 Fix Acceptance

- [x] `./gradlew assembleDebug` succeeds (`1.0.1`, versionCode 24).
- [x] APK installs on the Xiaomi 11i.
- [x] E2E ×3 (tile → circle → Confirm → Start → Lens results each time).
- [x] Gallery delta is 0 new images across all runs.
- [x] Lens receives the correct circled crop each time.
- [x] Cancel/deny/fail paths leave no files; repeated runs don't accumulate
  (private dir holds latest run only).
- [x] Human tester explicitly confirms.

## Phase 11 Acceptance Criteria

- [x] Audit: 3 necessary permissions, zero deps/secrets, 40 PII-free log
  lines (kept), error handling at all boundaries.
- [x] Release config: R8 minify+shrink (+SelectionView keep rule), local
  release keystore (outside repo, gitignored passwords), `1.0.0`.
- [x] `assembleRelease` succeeds; APK signed (apksigner-verified personal
  cert), 38KB, non-debuggable.
- [x] Uninstall debug APK, install release APK (fresh signature).
- [x] Grant notifications, place tile, full E2E incl. overlay draw
  (validates R8 kept the custom view) → Lens results.
- [x] Uninstall release APK cleanly (`adb uninstall`); gallery copies
  remain by OS design (private data removed).
- [x] Human tester explicitly confirms final acceptance.

## Phase 10 Acceptance Criteria

- [x] MIUI settings recipe + M1–M10 suite documented (no code changes).
- [x] Tester applies the recipe on the Xiaomi 11i.
- [x] Tester executes M1–M10; every failure fixed smallest-first, retested.
- [x] No hacks/bypasses; tile remains the sole trigger.
- [x] Human tester confirms reliability acceptance.

## Phase 10 MIUI Settings Recipe (Xiaomi 11i, exact paths to confirm)

1. QS tile: swipe down → edit tiles → **Circle Search** in active tiles.
   Persists across reboots (verified M1).
2. Notifications (REQUIRED for tap-to-search): long-press app icon → App
   info → Notifications → **Allow**. Reinstalls reset this: open the app
   once post-install and Allow at the prompt. Dashboard sharing works
   without it.
3. Battery saver: App info → Battery saver → **No restrictions**
   (recommended so MIUI never defers the capture service; M4 tests whether
   restrictive mode actually breaks anything).
4. Autostart: nothing to set — this build offers no autostart option and
   needs none (no background work; the tile is system-driven and
   self-revives, proven Phase 2/Round-2).
5. Overlay/"Display pop-up in background": NOT needed — the app holds no
   overlay permission (translucent activity only). Do not grant anything here.
6. "Allow restricted settings": NOT needed anymore (accessibility service
   removed in Phase 2). Dead service entry should have been disabled.
7. App lock: test captures on apps WITHOUT app lock (locked apps
   legitimately capture the lock screen — Round 3 finding, not a bug).

## Phase 10 Reliability Suite (report PASS/FAIL + observations per case)

- M1 Reboot cold: reboot → without opening the app, tile → full E2E.
- M2 Kill + tile: swipe-kill app → tile E2E (tile must self-revive).
- M3 Notifications OFF: disable per recipe step 2 → E2E → capture +
  auto-open Lens must still work; only the tap notice missing. Re-allow after.
- M4 Restrictive battery: set Battery saver restrictive → E2E → pass means
  No-restrictions is optional; fail means it is REQUIRED (recipe updated).
- M5 Do Not Disturb ON: full E2E → auto-open must be unaffected; note
  whether the tap notice appears silently.
- M6 Overnight Doze: leave phone idle overnight → morning tile E2E, no app
  pre-opened.
- M7 Tile remove/re-add: remove tile, re-add, E2E.
- M8 Reinstall: `adb install -r` → re-allow notifications → E2E; report
  what state survived (tile placement, grants).
- M9 Dark mode ON: overlay buttons/readability + dashboard legible.
- M10 Low-power mode (Battery saver system-wide): E2E, note latency.

## Phase 10 Test Rounds

- Round 1 (PASS): recipe applied, full M1–M10 suite green, zero failures —
  no code changes required. Phase 10 complete.

## Phase 9 Acceptance Criteria

- [x] Destructive suite defined (`0.11.3-phase8`, versionCode 22, no changes).
- [x] Tester executes the full suite on the Xiaomi 11i.
- [x] Every reported failure fixed with the smallest change, rebuilt, retested.
- [x] No unrelated rewrites; tile remains the sole trigger.
- [x] Human tester confirms the robustness suite passes.

## Phase 9 Destructive Suite (report PASS/FAIL + what you saw per case)

- R1 Rapid double-tap tile: tap tile twice fast → expect ONE overlay, no
  crash, drawing works.
- R2 Back at each stage: back on overlay (exits), back on consent dialog
  (returns to overlay, can redraw), back during invisible wait (clean exit,
  capture still completes to notification).
- R3 Home mid-flow: Home on overlay, Home on consent dialog → expect no
  crash; report where you land when returning.
- R4 Rotate during draw: rotate with overlay open → expect usable overlay
  (drawing may reset), no crash.
- R5 Rotate during consent dialog and during capture wait → expect capture
  completes or fails gracefully with toast/log, no crash.
- R6 Deny → retry: deny consent, draw again, Confirm, grant → expect normal
  capture (button re-enabled, no stuck state).
- R7 Swipe-kill app mid-flow: kill during overlay and during capture wait →
  expect no crash loop; next tile tap starts clean.
- R8 Back-to-back runs: complete E2E twice in quick succession → expect two
  independent captures, second never silently swallowed (check logcat for
  `already in progress` + 12s-timeout behavior).
- R9 Notifications revoked: App info → Notifications OFF → full E2E →
  expect capture + Lens auto-open still work (only the tap-to-search notice
  missing); then re-allow.
- R10 Reboot: reboot with tile placed → tile still present and E2E works.
- R11 Heavy foreground app: run E2E over a game/heavy app (no applock) →
  expect capture + crop correct, note latency.
- R12 Secure/DRM app (e.g. video app): expect graceful behavior — capture
  may be black per OS design; report whether the app stays stable and what
  Lens receives (known limitation, must not crash).

## Phase 9 Test Rounds

- Round 1 (PASS): full R1–R12 suite green on device, zero failures reported —
  no code changes required. Phase 9 complete.

## Phase 8 Acceptance Criteria

- [x] `./gradlew assembleDebug` succeeds (`0.11.3-phase8`, versionCode 22).
- [x] APK installs on the Xiaomi 11i.
- [x] Confirm → consent dialog appears OVER the dimmed overlay+circle
  (no dead gap); Deny → instant continue, no flash.
- [x] Grant → capture proceeds; shade shows "Circle Search capture" with a
  working Cancel action (cancel → no files, no search notice, clean exit).
- [x] After Start now with NO further taps: Lens opens on the crop
  automatically (direct Google-app target; chooser only as fallback).
- [x] Normal E2E unchanged: tap-to-search → chooser → Lens.
- [x] Tile remains the only trigger; no unrelated features.
- [x] Human tester explicitly confirms pass.

## Phase 8 Test Rounds

- Round 1 (FAIL): toast "Capture failed (no frame)" after Start now.
  Zero usable images in the 4s window. Suspects ordered: (1) 0.9.1's
  physical-display sizing never device-tested (geometry); (2) 0.11.1 keeps
  the overlay alive during capture (frame-production stall?); (3) stride
  conversion rejects. Current logs already discriminate (see below) —
  NO code change until logcat identifies which. Needed from one failing
  run: the `display rotation=… real=… requested=…` line, the
  `frames seen=…` line, any `failed` line, plus app + orientation held.
  - `real≠requested` → geometry fix. `seen=0` → production stall.
    `seen>0` → conversion fix.
- Round 2 (exact cause from logcat): `real=requested`, but
  `frames seen=1 skipped=1` — one settled frame arrived and our skip logic
  recycled it, then the static screen produced nothing more. Root cause
  (code-related): skip-first-frames assumed continuous flow (true when the
  overlay finished and UI animated); the 0.11.1 always-alive invisible
  overlay yields ~one latched frame. Fix (`0.11.2-phase8`, v21): skipped
  frames held as fallback, saved at deadline when nothing better arrives;
  blank-preference and transition settle unchanged.
- Round 3 (latency): auto-open works but slow (~5s). Cause (code-related):
  after holding the fallback frame the loop pointlessly polled a silent
  screen to the full 4s deadline. Fix (`0.11.3-phase8`, v22): break after
  5 consecutive empty polls when a non-blank fallback is held (~0.5s).
  Typical flow now ~1–2s total; blank-fallback path still waits it out.
- Round 4 (PASS): fast auto-open to Lens, cancel/deny/chooser paths verified.
  Phase 8 complete.

## Phase 7 Acceptance Criteria

- [x] `./gradlew assembleDebug` succeeds (`0.10.1-phase7`, versionCode 18).
- [x] APK installs on the Xiaomi 11i.
- [x] E2E (no-applock app, portrait): tile → dimmed overlay over the app →
  circle → Confirm → consent → "Circle Search ready" notification appears.
- [x] Tap notification → chooser → Lens → visual results for the circled
  region. Dashboard never needed mid-flow.
- [x] Only trigger is the QS tile; only necessary permissions (FGS pair for
  capture, POST_NOTIFICATIONS for tap-to-search); no unrelated features.
- [x] Human tester explicitly confirms the end-to-end workflow passes.

## Phase 7 Test Rounds

- Round 1 (FAIL, my wrong assumption): no "Circle Search ready" notification
  ever appears — App info shows notifications greyed-out OFF. Cause:
  code-related gap — I relied on an FGS exemption that covers only the
  service's own notice, not content notifications; on Android 13+ these are
  off-by-default until POST_NOTIFICATIONS is declared + runtime-granted.
  Fix (`0.10.1-phase7`, v18): manifest permission + request on dashboard
  open (grant logged). Dashboard sharing works with or without the grant.
- Round 2 (PASS): release `1.0.0` installed (debug uninstalled first),
  notifications granted, tile placed, E2E incl. overlay draw → Lens;
  uninstall clean. Phase 11 complete — daily driver released.
- Round 2 (PASS): notification granted, E2E tile→circle→tap→Lens passes.
  Phase 7 complete — first MVP done.

## Phase 6 Acceptance Criteria

- [x] `./gradlew assembleDebug` succeeds (`0.9.1-phase6`, versionCode 16).
- [x] APK installs on the Xiaomi 11i.
- [x] Feasibility analysis recorded (Lens/Bing/Cloud Vision/ML Kit/TinEye/
  share, 9 criteria each) — in PROJECT.md.
- [x] PoC: capture a crop → dashboard "Visual search (share crop)" →
  system chooser appears with Lens (tester confirmed "Search image
  (Google) works splendidly").
- [x] PoC: picking Lens shows visual results for the crop; no auth/CAPTCHA/
  restriction bypass anywhere.
- [x] No permanent search integration added.
- [x] Human reviews result and chooses the integration (chose: share-handoff).

## Phase 6 Test Rounds

- Round 1 (FAIL, likely not-code): on some foreground app (looks like a video
  app), full + crop capture mostly BLACK with only a thin readable left
  strip; Home screen captures perfectly on the same build, and Chrome was
  perfect in Phase 4. Signature matches Android FLAG_SECURE content
  (DRM/secure video surfaces are excluded from MediaProjection by OS design
  — cannot and must not be bypassed, AGENTS.md §5). Crop inherits the black
  from the full frame, so no separate crop bug. Pending tester confirmation:
  (1) which app shows this, (2) Chrome/Settings retest on current build
  still perfect. If confirmed, Phase 7 must handle protected content
  gracefully (we already have isBlank(); warn instead of searching black).
- Round 2 (FAIL, theory revised): black/slivered captures now in Gallery,
  Chrome, YouTube, Instagram — Home perfect, same build. Secure-content
  theory DEAD (Chrome/Gallery aren't secure). Remaining suspect:
  buffer/display size mismatch — mirror sized from overlay-window metrics
  disagreeing with the physical display (rotation/decor), yielding partial
  frames. Fix (`0.9.1-phase6`, v16): capture sized from the physical
  display's real metrics with rotation logged
  (`display rotation=R real=WxH requested=WxH`); overlay metrics only a
  fallback. Crop alignment may be off when sizes disagree — expected this
  round; the size log is the diagnostic.
- Round 3 (ROOT CAUSE FOUND by tester, not-code): MIUI App Lock. When our
  overlay/consent flow foregrounds, App Lock blanks the screen with the
  fingerprint/pattern prompt, and the capture faithfully records that black
  lock screen (sliver = transition). Apps WITHOUT app lock capture
  perfectly. Nothing to fix — bypassing a lock screen is forbidden
  (AGENTS.md §5); capturing the displayed lock screen is correct behavior.
  Known limitation: captures taken while any lock/auth screen is up will
  show that screen. PoC result stands: share → Lens works splendidly on
  good captures. Awaiting integration choice (a/b/c/d).
- Round 4 (CLOSE): tester chose (a) — productize the share handoff.
  Consent dialog confirmed non-suppressible by OS design (recorded, no
  bypass attempted). Phase 6 complete.

## Phase 5 Acceptance Criteria

- [x] `./gradlew assembleDebug` succeeds (`0.8.1-phase5`, versionCode 14).
- [x] APK installs on the Xiaomi 11i.
- [x] Portrait: circle an object → crop matches the circled area
  (+ small margin) in gallery `crop_<ts>.png`; dashboard `Last crop` line.
- [x] Landscape (rotate phone, repeat): crop matches, no crash.
- [x] Edge circle (touching screen edges): clamped, no crash.
- [x] Dot/tiny tap + Confirm: gracefully skipped (log `too small`), no crash.
- [x] No visual search, networking, or new permissions.
- [x] Human tester explicitly confirms pass.

## Phase 5 Test Rounds

- Round 1 (FAIL): file counts vary run to run (0–3 files: missing full,
  duplicated fulls, missing crop). Causes: (1) concurrent duplicate service
  starts (fast re-taps / double-tap Confirm) each capturing; (2) dashboard
  showed STALE success lines after failures, hiding what actually happened;
  (3) gallery accumulates all runs, so eyeball-counting mixes runs. Fix
  (`0.8.1-phase5`, v14): re-entrancy guard (duplicate start ignored +
  logged); Confirm disabled after first tap (re-enabled on deny); failure
  paths now overwrite dashboard lines (FAILED/skipped markers, never stale).
  Retest requires a clean slate (empty the CircleSearch album first) and
  exact per-run filenames.
- Round 2 (PASS): clean-slate protocol — exact expected files per run
  (portrait/landscape/edge crops match, dot gracefully skipped, no crashes).
  Phase 5 complete.

## Phase 4 Acceptance Criteria

- [x] `./gradlew assembleDebug` succeeds (`0.7.5-phase4`, versionCode 12).
- [x] APK installs on the Xiaomi 11i.
- [x] Phase 3 overlay still works (draw, persistent stroke, Cancel, back).
- [x] Confirm with a circle → overlay hides → system screen-cast consent
  dialog appears (first run: allow it).
- [x] After consent: capture saved, dashboard shows
  `Last capture: capture_<ts>.png <W>x<H> <N>KB`; overlay closes.
- [x] Denying consent restores the overlay (can Cancel/draw again).
- [x] Pulled PNG shows the underlying app with NO overlay/dim artifacts.
- [x] No cropping, search, Lens, networking, OCR, or AI code.
- [x] Human tester explicitly confirms pass.

## Phase 4 Test Rounds

- Round 1 (FAIL): consent dialog appears, but tapping "Start now" kills the
  flow — overlay vanishes, no capture, dashboard counters reset to zero.
  Cause (code-related): `android:noHistory="true"` finished OverlayActivity
  the moment the system consent activity came forward, so the result had no
  recipient; later process death reset the in-memory counters (hence the
  zeros — not a separate bug).   Fix 2026-09-07 (`0.7.1-phase4`, v8): removed
  `noHistory`; capture setup wrapped in try/catch (failure restores overlay
  instead of dying); dashboard shows on-disk captures (survives process
  death). If "Start now" still fails, logcat will now show the exact
  exception via the `capture setup failed` / `capture failed` lines.
- Round 2 (exact cause found): `Capture failed: java.lang.SecurityException`
  — my error, not the device's. Per official docs, targetSdk 29+ apps MUST
  call `getMediaProjection` inside a foreground service of type
  `mediaProjection`; I called it with none. Fix (`0.7.2-phase4`, v9):
  new `CaptureService` (FGS type `mediaProjection` + both FGS permissions,
  both auto-granted normal permissions); overlay hands the consent result
  to it and finishes; capture runs there, then the service stops. Consent
  flow itself is unchanged and legitimate.
- Round 3 (FAIL): capture itself works and the PNG generates fine, BUT the
  phone lands on the Circle Search dashboard instead of staying on the
  foreground app. Cause under investigation — need timing + PNG content to
  distinguish task-reorder-before-capture from task-switch-after-capture.
- Round 3 refined (tester answers): dashboard appears immediately at tile
  tap, and the pulled PNG SHOWS THE DASHBOARD. So the overlay joins/is
  layered over our own task and captures it — the tile launch foregrounds
  our main task around the consent interlude on MIUI. Fix (`0.7.3-phase4`,
  v10): `MainActivity noHistory` (dashboard never lingers as a competing
  task) + tile launches overlay with `NEW_TASK | CLEAR_TASK` (always a fresh
  overlay task, never joins/resurrects another). Nothing else changed.
- Round 4 (FAIL, expectation gap): capture works — dashboard shows
  `capture_….png 1080x2400 1944KB` and `Files on disk` confirms it — but the
  tester expected the screenshot visible on the device. Cause: file lived in
  app-private storage, invisible to gallery/file managers. Fix
  (`0.7.4-phase4`, v11): also write a MediaStore copy to
  Pictures/CircleSearch (no permission needed for own files on API 29+);
  app-private PNG stays the next phase's source. Dashboard appends
  "+ gallery" on success.
- Round 5 (FAIL): captures alternate good / completely-black across taps.
  Cause class: capture-timing fragility — we grabbed the very first queued
  frame, which during consent-exit/task transitions (or as a
  placeholder/fence-not-ready buffer) can be blank. Fix (`0.7.5-phase4`,
  v12): 600ms settle delay after display creation, skip first 2 frames,
  keep polling up to 4s preferring the first non-blank frame (64-pixel grid
  check), save last blank only if nothing better arrives; frame stats logged
  (`frames seen=X skipped=Y usedBlank=Z`). If black persists,   those stats +
  whether the consent dialog appeared each time narrow it to a stateful
  cause (secure content / MIUI mirror quirk).
- Round 6 (PASS): 4 consecutive captures all good, gallery copies verified.
  Phase 4 complete.

## Phase 3 Acceptance Criteria

- [x] `./gradlew assembleDebug` succeeds (`0.6.0-phase3`, versionCode 6).
- [x] APK installs on the Xiaomi 11i.
- [x] Tile tap over a foreground app opens the dimmed fullscreen overlay.
- [x] Finger draws a smooth white freehand path that follows the touch.
- [x] Path stays visible after finger release; new touch starts a new selection.
- [x] Cancel removes the overlay immediately, returns to the foreground app.
- [x] Confirm with a drawn circle logs bounds, shows them, closes overlay;
  dashboard shows last selection. Confirm with no drawing stays open.
- [x] No capture, cropping, search, Lens, networking, OCR, or AI code.
- [x] Human tester explicitly confirms pass.

## Phase 3 Test Rounds

- Round 1 (FAIL): overlay, buttons, and drawing all perfect, BUT the tile
  brought our app task forward — the overlay sat on the dashboard instead of
  the foreground app. Cause: code-related — `OverlayActivity` used the default
  task affinity, so `FLAG_ACTIVITY_NEW_TASK` joined the existing app task.
  Fix: `android:taskAffinity=""` (separate task layered above the foreground
  app). Rebuilt, same version, retest pending.
- Round 2 (PASS): overlay appears over the foreground app; draw/cancel/
  confirm all behave. Phase 3 complete.

## Phase 2 Acceptance Criteria (tile-only, revised 2026-09-07)

- [x] `./gradlew assembleDebug` succeeds (`0.5.0-phase2`, versionCode 5).
- [x] APK installs on the Xiaomi 11i.
- [x] QS tile tap from any foreground app → tile count +1, dashboard opens.
- [x] Counts + log (`activated via quick-settings-tile`) are ground truth;
  toast is best-effort only (MIUI suppresses background toasts).
- [x] No accessibility service, overlay, capture, cropping, search, or Lens code.
- [x] Human tester explicitly confirms pass.

## Phase 2 Test Rounds (0.4.0+guard)

## Phase 2 Test Rounds (0.4.0)

- Round 1: tile tap opens app + tile count +1, but NO toast unless the app is
  already open. Shortcut worked right after install, then died; Settings shows
  the service as malfunctioning.
- Diagnosis (§7): code-related prime suspect — unguarded `Toast` as the last
  step of `Activation.trigger`. Counts increment (code before the toast runs),
  then the toast attempt from a background process throws on this MIUI build,
  killing the shared app process and taking the accessibility service down
  (0.3.0 had no toast and survived 49 clicks). Possible contributor:
  environment-related — reinstall may have reset MIUI battery/autostart
  restrictions. Fix 2026-09-07: toast wrapped in try/catch (feedback can never
  kill the service); counts + log remain ground truth. Rebuilt, retest pending
  with service toggle + battery steps below.
- Round 2 (guard verified): fresh install, both toasts + counts work with app
  open. Over a foreground app: no toast but BOTH counts increment — guard
  holds, service survives. New finding: after swiping the app away and
  reopening, tile works but shortcut doesn't. Diagnosis: environment-related
  (MIUI force-kill; tile self-revives on demand, the a11y service doesn't
  rebind by itself). Dashboard now shows "Service bound this session" to
  distinguish settings-flag-true from actually-bound. Fix direction is MIUI
  user settings (Autostart, Battery No restrictions, lock app in recents),
  not code.
- Round 3 (tester decision 2026-09-07): shortcut DITCHED — after swipe-kill +
  reopen, `enabled: true` but bound `false` despite No-restrictions battery
  and locked app (no autostart option on this build). Tile-only from here:
  `0.5.0-phase2` removes GestureService, a11y config, and overlay/FGS
  remnants. Tester should also disable the dead service entry in Settings →
  Accessibility.
- Phase 2 Round 4 (PASS): tester confirmed tile tap from foreground app
  increments count and opens dashboard. Phase 2 complete.

## Completed Phases

- Phase 0 — minimum viable Android project: scaffold builds, installs, and
  launches on the Xiaomi 11i showing the full "Circle Search / Phase 0 —
  project scaffold OK" UI (incl. large-font clipping fix, commit a86db9c).
- Phase 1 — CLOSED with negative verdict (tester-confirmed 2026-09-07):
  Xiaomi 11i does not expose fingerprint gestures (`NOT AVAILABLE`).
  Fingerprint swipe is ruled out as the ACTIVATE trigger. No checkpoint tag
  (phase did not pass); result recorded as a finding, not a feature.
- Phase 2 — GLOBAL TRIGGER, complete 2026-09-07 (tester-confirmed PASS):
  Quick Settings tile is the sole trigger wired to centralized `Activation`
  (`0.5.0-phase2`); tile tap from any foreground app increments the count
  and opens the dashboard. Accessibility shortcut tried and dropped (MIUI
  does not rebind the service after process kill).

## Phase 1 Acceptance Criteria

- [x] `./gradlew assembleDebug` succeeds (`0.2.0-phase1`, versionCode 2).
- [x] APK installs on the Xiaomi 11i.
- [x] Accessibility service can be enabled via system Settings (required the
  Android 13 "Allow restricted settings" step for sideloaded apps; then enabled OK).
- [x] Probe reports gesture availability (AVAILABLE or NOT AVAILABLE).
- [x] If AVAILABLE: each supported swipe direction is detected and shown/logged.
  (N/A — device reports NOT AVAILABLE, so no swipe test applies.)
- [ ] Human tester explicitly confirms the result.

## Phase 0 Acceptance Criteria

- [x] `./gradlew assembleDebug` succeeds on the dev machine.
- [x] `app/build/outputs/apk/debug/app-debug.apk` installs on the Xiaomi 11i.
- [x] App launches and shows "Circle Search / Phase 0 — project scaffold OK".
- [x] Human tester explicitly confirms pass.

## Current Blockers

- None (previous "Restricted setting" blocker cleared via "Allow restricted
  settings").

## Test Results

- Round 1 (FAIL): install + launch OK, but both TextViews clipped on the
  right ("Circle Searc" / "Phase 0 — project scaffol…"). Cause: code-related —
  `wrap_content` TextViews in a centered layout overflow instead of wrapping
  under the device's large system font. Fix: `match_parent` width +
  `center_horizontal` gravity so text wraps. Rebuilt 2026-09-07, retest pending.
- Round 2 (PASS): human tester confirmed full text visible on Xiaomi 11i.
  Phase 0 complete.
- Phase 1: implementation complete 2026-09-07 (`0.2.0-phase1`); device
  validation pending.
- Phase 1 Round 1 (BLOCKED, not a code failure): "Restricted setting" dialog
  when enabling the service. Awaiting tester to grant "Allow restricted
  settings" and re-run the enable + gesture test.
- Phase 1 Round 2 (DEVICE VERDICT): service enabled (`Service enabled: true`)
  after granting "Allow restricted settings"; probe reports `Gestures: NOT
  AVAILABLE on this device`, all swipe counters 0 (screenshot evidence).
  FingerprintGestureController is not exposed on the Xiaomi 11i — consistent
  with its side-mounted (power-button) sensor, which does not support swipe
  gestures. No swipe testing applies. Awaiting explicit tester confirmation
  to close Phase 1.
- Phase 1 closed 2026-09-07 on tester verdict ("Phase 1 has failed"):
  fingerprint gestures unavailable, no bypass attempted. Finding recorded in
  PROJECT.md.
- Phase 2 — activation investigation, IN PROGRESS: all four probes
  device-tested with verdicts (volume FAIL, a11y shortcut PASS, QS tile PASS,
  overlay UNPROVEN). Tester chose option (c): accessibility shortcut primary
  + QS tile fallback. Closure criteria still to be confirmed by tester.
- Phase 2: activation probes built 2026-09-07 (`0.3.0-phase2`, versionCode 3):
  volume-key observation, a11y-button callback, QS tile, overlay dot. Device
  validation pending; tester chooses mechanism before further development.
- Phase 2 Round 1 (DEVICE VERDICTS, screenshot evidence):
  - Volume keys: FAIL — no key events reach `onKeyEvent` (`(none yet)`, count
    0); keys just adjust volume. Ruled out on this MIUI build.
  - A11y shortcut/button: PASS — 49 clicks registered via the floating
    "Circle Search shortcut" button (per-service shortcut toggle exists in
    Settings → Accessibility → Circle Search). Note: API availability flag
    reads NOT AVAILABLE while clicks work — flag is cosmetic here, clicks are
    the ground truth.
  - QS tile: PASS — 2 taps, opens app.
  - Overlay dot: FAIL/UNPROVEN — `permitted=true` but `shown=false`, no dot
    seen. Suspected MIUI FGS/background kill (or API-33 `specialUse`-type
    issue); needs logcat to diagnose. Lowest priority given two proven options.
  - Awaiting tester choice of mechanism.
- Phase 2 completion spec (tester instruction 2026-09-07): GLOBAL TRIGGER —
  a11y shortcut primary + QS tile secondary, both calling centralized
  `Activation.trigger`; no overlay/capture/search.
- Phase 2 implementation complete 2026-09-07 (`0.4.0-phase2`, versionCode 4):
  `Activation.kt` added; shortcut + tile wired to it; volume observer and
  overlay service/permissions removed. Device validation pending.
