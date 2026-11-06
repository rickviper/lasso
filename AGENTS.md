# AGENTS.md

# Lasso — OpenCode Development Rules

## 1. Project Objective

Build a personal Android application for the Xiaomi 11i running Android 13 that provides a Circle-to-Search-style experience:

1. User activates the service.
2. A selection interface appears over the current application.
3. User lassos an object on the screen.
4. The application extracts the selected visual region.
5. The selected image is sent to an appropriate visual-search mechanism.
6. Useful visual-search results are presented to the user.

The ultimate user experience should be:

ACTIVATE → LASSO → SEARCH

The application is primarily intended for personal use.

---

## 2. Development Methodology

This project MUST be developed incrementally.

Only ONE phase may be worked on at a time.

The current phase is recorded in PROJECT_STATUS.md.

DO NOT implement future phases unless explicitly instructed.

A phase is complete only after:

1. The implementation is complete.
2. The project builds successfully.
3. The APK installs successfully.
4. The feature is tested on the physical Xiaomi 11i.
5. All acceptance criteria for the phase pass.
6. The human tester explicitly confirms the phase passed.

Compilation alone does NOT constitute success.

---

## 3. Target Device

Primary device:

Xiaomi 11i
Android 13

The physical device is the source of truth for device-specific behavior.

Do not assume that an Android API documented as available is necessarily exposed or functional on the Xiaomi 11i/MIUI implementation.

---

## 4. Scope Discipline

For every phase:

* Implement only what is necessary for the current phase.
* Do not prematurely implement future functionality.
* Do not add unnecessary libraries.
* Do not introduce a backend unless required.
* Do not add authentication unless required.
* Do not add analytics or telemetry.
* Do not add cloud infrastructure unless explicitly required.
* Prefer native Android APIs where practical.
* Prefer the simplest working implementation.

If a future requirement is discovered, document it rather than implementing it early.

---

## 5. Device Security Restrictions

Do not attempt to bypass:

* Android permission systems
* Accessibility security restrictions
* MediaProjection consent
* overlay restrictions
* biometric security
* application sandboxing
* OS security mechanisms

Use officially supported Android mechanisms wherever possible.

---

## 6. Testing

Every phase must have explicit acceptance tests.

The developer agent must provide:

* build instructions
* installation instructions
* exact test procedure
* expected result
* diagnostic information to collect if the test fails

The human tester performs the physical-device tests.

The developer agent must not claim device-specific functionality works without physical-device verification.

---

## 7. Error Handling

When something fails:

1. Identify the exact failure.
2. Determine whether it is:

   * code-related
   * Android API-related
   * permission-related
   * Xiaomi/MIUI-specific
   * environment-related
   * external-service-related
3. Fix only the relevant problem.
4. Rebuild.
5. Retest.

Do not rewrite the entire project unless there is a clear technical reason.

---

## 8. Checkpoints

After each successful phase:

Create a Git commit using:

phase-X-complete

Also update PROJECT_STATUS.md.

Never remove a working checkpoint without a clear reason.

---

## 9. Documentation

Maintain:

PROJECT.md
PROJECT_STATUS.md
AGENTS.md

PROJECT.md contains the architecture and overall project description.

PROJECT_STATUS.md contains the current phase, completed phases, current blockers, and test results.

---

## 10. Before Coding

At the beginning of every phase:

1. Read AGENTS.md.
2. Read PROJECT.md.
3. Read PROJECT_STATUS.md.
4. Inspect the existing implementation.
5. State what you understand about the current phase.
6. State what files/components you expect to modify.
7. Identify any technical uncertainty.
8. Only then begin implementation.

---

## 11. Completion Rule

Never say:

"Phase complete"

unless the implementation has passed the specified acceptance criteria on the Xiaomi 11i.

If the test has not been performed, say:

"Implementation complete; device validation pending."

---

## 12. Current Development Philosophy

The goal is not to demonstrate sophisticated Android engineering.

The goal is to create a reliable personal utility.

Prefer:

SIMPLE + RELIABLE + TESTED

over:

COMPLEX + ELEGANT + UNTESTED

