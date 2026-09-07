# Circle Search — Project Status

## Current Phase

Phase 1 — fingerprint-gesture feasibility probe on Xiaomi 11i / Android 13.
Implementation complete; device validation pending.

## Completed Phases

- Phase 0 — minimum viable Android project: scaffold builds, installs, and
  launches on the Xiaomi 11i showing the full "Circle Search / Phase 0 —
  project scaffold OK" UI (incl. large-font clipping fix, commit a86db9c).

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
