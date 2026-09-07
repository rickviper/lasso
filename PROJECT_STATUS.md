# Circle Search — Project Status

## Current Phase

Phase 0 — minimum viable Android project (scaffold only).

## Completed Phases

- (none yet — Phase 0 implementation in progress, device validation pending)

## Phase 0 Acceptance Criteria

- [ ] `./gradlew assembleDebug` succeeds on the dev machine.
- [ ] `app/build/outputs/apk/debug/app-debug.apk` installs on the Xiaomi 11i.
- [ ] App launches and shows "Circle Search / Phase 0 — project scaffold OK".
- [ ] Human tester explicitly confirms pass.

## Current Blockers

- None.

## Test Results

- Round 1 (FAIL): install + launch OK, but both TextViews clipped on the
  right ("Circle Searc" / "Phase 0 — project scaffol…"). Cause: code-related —
  `wrap_content` TextViews in a centered layout overflow instead of wrapping
  under the device's large system font. Fix: `match_parent` width +
  `center_horizontal` gravity so text wraps. Rebuilt 2026-09-07, retest pending.
- Round 2: pending device validation by human tester (fixed APK).
