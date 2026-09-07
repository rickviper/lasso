# Circle Search — Project Status

## Current Phase

Phase 2 — activation mechanism investigation (no bypasses; tester chooses the
mechanism before any further development).

## Completed Phases

- Phase 0 — minimum viable Android project: scaffold builds, installs, and
  launches on the Xiaomi 11i showing the full "Circle Search / Phase 0 —
  project scaffold OK" UI (incl. large-font clipping fix, commit a86db9c).
- Phase 1 — CLOSED with negative verdict (tester-confirmed 2026-09-07):
  Xiaomi 11i does not expose fingerprint gestures (`NOT AVAILABLE`).
  Fingerprint swipe is ruled out as the ACTIVATE trigger. No checkpoint tag
  (phase did not pass); result recorded as a finding, not a feature.

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
