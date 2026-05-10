# Core Testing Brief

## Goal

LaundryHub uses `origin/master` as the product-behavior baseline while the KMP/API branch is being recovered. The default test suite should protect the app shape without mutating live business data.

## Baseline Strategy

- Keep the active KMP worktree unchanged while reading `origin/master` from a separate `git worktree`.
- Run the master baseline first when the expected Android behavior is unclear.
- Port only stable, user-visible contracts into KMP instrumentation tests.
- Keep mutating scenarios behind an explicit decision because the current macrobenchmark flow can create real orders.

## Current Instrumentation Coverage

- Order bottom sheet WhatsApp option visibility stays covered in `OrderBottomSheetWhatsAppOptionTest`.
- Outcome bottom sheet now checks the add/update form contract, required fields, payment chips, and enabled update action.
- Profile content now checks the baseline section hierarchy, Inventory and Reminder navigation callbacks, WhatsApp option visibility, clear-cache confirmation, and sign-out surface.
- Inventory content now checks package master rows, add-package entry, unregistered package suggestions, and package editor save gating.
- `LaundryHubStartupE2eTest` launches the real app and verifies it reaches a known entry point: onboarding, spreadsheet setup, or the signed-in app shell.
- The signed-in shell smoke test navigates Home, History, Order sheet, Outcome, and Profile without submitting data. It skips when the device is not signed in.
- `LaundryHubGuardedMutatingE2eTest` contains the first guarded mutating flow for add-order submission. It is skipped unless sandbox mutation arguments are explicitly passed.
- The guarded add-order flow now validates the real app path far enough to catch backend duplicate-ID failures: package selection, required field entry, submit, backend `201`, home refresh, and database row visibility.
- The app robot launch path no longer force-stops the target package during instrumentation because that can kill the process hosting `TestRunner`.

## Run Modes

Default safe run:

```bash
./gradlew connectedDebugAndroidTest --no-daemon
```

Guarded sandbox mutation run:

```bash
./gradlew connectedDebugAndroidTest --no-daemon \
  -Pandroid.testInstrumentationRunnerArguments.laundryhub.e2e.mutating=true \
  -Pandroid.testInstrumentationRunnerArguments.laundryhub.e2e.target=sandbox
```

Only use the guarded mutation run after confirming the installed app points to a sandbox backend/database. The guard intentionally requires both arguments so production data is not mutated by accident.

## Device Notes

- The connected device was `SM-S931B - 16`.
- The first KMP run failed after the device screen became inactive, which destroyed the Compose hierarchy during tests.
- Before running connected tests on this device, wake/unlock it and keep the display awake with `adb shell svc power stayon true`.
- On Samsung devices, dismiss notification shade/keyguard before manual instrumentation. A system overlay can make the startup detector return `UNKNOWN` even when the app session is valid.
- Prefer a stable USB connection for long connected runs. The latest Wi-Fi ADB attempt dropped during Gradle device service/property queries and produced `Unknown API Level` / `No compatible devices` before a trustworthy app assertion result.
- Compose semantics are the primary selector path. The app robot also has a hierarchy-bounds fallback for package option cards because UIAutomator can intermittently miss clickable Compose nodes while the hierarchy dump still contains the semantic marker.

## Verification

- Master baseline worktree: `./gradlew testDebugUnitTest` passed.
- Master baseline worktree: `./gradlew connectedDebugAndroidTest` passed.
- KMP branch: `./gradlew :app:testDebugUnitTest :backend:test --no-daemon` passed.
- KMP branch: `./gradlew :shared:jvmTest --no-daemon` passed.
- KMP branch: `./gradlew assembleDebug --no-daemon` passed.
- KMP branch: `./gradlew assembleRelease --no-daemon` passed.
- KMP branch: `./gradlew :app:assembleDebugAndroidTest --no-daemon` passed.
- Earlier KMP branch run: `./gradlew connectedDebugAndroidTest --no-daemon` passed with the safe default instrumentation suite.
- Latest KMP branch run: `./gradlew connectedDebugAndroidTest --no-daemon` is not green yet. The run was blocked by unstable Wi-Fi ADB/device transport after the robot force-stop issue was fixed.
- Earlier manual guarded mutation run passed with `laundryhub.e2e.mutating=true` and `laundryhub.e2e.target=sandbox`. Treat that as historical evidence only; rerun after deploying the latest outcome/package API changes before relying on it for this branch state.

## Remaining E2E Risk

- Inventory mutating flows still need sandbox-backed submit/update/delete coverage before we can call the full E2E matrix complete.
- The full add/delete macrobenchmark was not executed in this pass. Run it only after confirming the target backend/database is safe for mutating E2E validation.

## E2E Definition Of Done

- Default `connectedDebugAndroidTest` stays green without mutating live data.
- The signed-in shell smoke passes on a logged-in device.
- Mutating add/update/delete tests run only against a confirmed sandbox backend/database.
- Order, outcome, inventory, history, home refresh, and profile/settings all have either contract coverage or a real app-flow smoke test.
- Macrobenchmark results are recorded in `docs/core/performance/README.md` only after the scenario runs against a stable, safe target.
