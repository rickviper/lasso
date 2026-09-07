# Circle Search — Project Status

## Current Phase

Phase 3 — SELECTION OVERLAY. Implementation complete (`0.6.0-phase3`);
device validation pending. NOT complete until tester confirms.

## Phase 3 Acceptance Criteria

- [x] `./gradlew assembleDebug` succeeds (`0.6.0-phase3`, versionCode 6).
- [ ] APK installs on the Xiaomi 11i.
- [ ] Tile tap over a foreground app opens the dimmed fullscreen overlay.
- [ ] Finger draws a smooth white freehand path that follows the touch.
- [ ] Path stays visible after finger release; new touch starts a new selection.
- [ ] Cancel removes the overlay immediately, returns to the foreground app.
- [ ] Confirm with a drawn circle logs bounds, shows them, closes overlay;
  dashboard shows last selection. Confirm with no drawing stays open.
- [ ] No capture, cropping, search, Lens, networking, OCR, or AI code.
- [ ] Human tester explicitly confirms pass.

## Phase 3 Test Rounds

- Round 1 (FAIL): overlay, buttons, and drawing all perfect, BUT the tile
  brought our app task forward — the overlay sat on the dashboard instead of
  the foreground app. Cause: code-related — `OverlayActivity` used the default
  task affinity, so `FLAG_ACTIVITY_NEW_TASK` joined the existing app task.
  Fix: `android:taskAffinity=""` (separate task layered above the foreground
  app). Rebuilt, same version, retest pending.

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
