# Core Testing Brief

## Goal

LaundryHub uses `origin/master` as the product-behavior baseline while the KMP/API branch is being recovered. The default test suite should protect the app shape without mutating live business data.

## Baseline Strategy

- Keep the active KMP worktree unchanged while reading `origin/master` from a separate `git worktree`.
- Run the master baseline first when the expected Android behavior is unclear.
- Port only stable, user-visible contracts into KMP instrumentation tests.
- Keep mutating scenarios behind an explicit decision because the current macrobenchmark flow can create real orders.

## Coverage Reporting

- CI generates JaCoCo XML with `./gradlew clean testDebugUnitTest jacocoTestReport`.
- SonarCloud analyzes from the root project with explicit source, test, binary, and JaCoCo XML paths for all production modules:
  - `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`
  - `backend/build/reports/jacoco/test/jacocoTestReport.xml`
  - `shared/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`
- Coveralls receives the same three JaCoCo XML files through the GitHub Action `files` input.
- Gradle subprojects are marked `skipProject` for Sonar; JaCoCo XML is imported once by the root project. The root source set intentionally lists `app/src/main/java`, `backend/src/main/kotlin`, `shared/src/commonMain/kotlin`, and `shared/src/javaMain/kotlin`; previous CI logs showed `File '<name>.kt' not found in project sources` when coverage XML was imported without those explicit root source paths.
- The app report carries Android app coverage only. Shared coverage is published by the shared module report so `shared/src/commonMain/kotlin` is mapped through the root Sonar source set instead of leaking through the app report.
- `PlatformDate.kt` is excluded from coverage because the shared expect/actual files share a filename and JaCoCo JVM line mappings can point to the actual implementation while Sonar resolves the expect file first.
- Sonar tokens are expected to live in GitHub Actions secrets. Local verification should stop at generating and inspecting JaCoCo XML unless a developer intentionally provides a local token.
- The canonical repository for coverage badges and CI secrets is `ufebri/LaundryHub`.

## Current Instrumentation Coverage

- Order bottom sheet WhatsApp option visibility stays covered in `OrderBottomSheetWhatsAppOptionTest`.
- Outcome bottom sheet now checks the add/update form contract, required fields, payment chips, and enabled update action.
- Profile content now checks the baseline section hierarchy, Inventory and Reminder navigation callbacks, WhatsApp option visibility, clear-cache confirmation, and sign-out surface.
- Inventory content now checks package master rows, add-package entry, unregistered package suggestions, and package editor save gating.
- Inventory unit coverage now verifies backend packages without a valid Sheets row index still open in edit mode, so the update path is not tied to `sheetRowIndex`.
- `LaundryHubStartupE2eTest` launches the real app and verifies it reaches a known entry point: onboarding, spreadsheet setup, or the signed-in app shell.
- The signed-in shell smoke test navigates Home, History, Order sheet, Outcome, and Profile without submitting data. It skips when the device is not signed in.
- `LaundryHubGrossE2eTest` is a read-only signed-in E2E check for Home Gross Income. It fetches `/api/gross`, selects the current/latest month with the shared gross helper, then checks the Home card and Gross Detail first row. Pass `laundryhub.e2e.apiBaseUrl` when the app should be validated against a specific backend URL.
- `LaundryHubGuardedMutatingE2eTest` contains guarded mutating flows for order submit/update/delete, outcome submit/update/delete, inventory package submit/update/delete, and the Home pending-order visibility transition. It is skipped unless sandbox mutation arguments are explicitly passed.
- The guarded order flow validates the real app path far enough to catch backend duplicate-ID failures: package selection, required field entry, submit, backend-created id feedback, history edit, and history delete.
- The guarded outcome flow validates backend-created outcome ids, required payment method selection, edit, and delete through the deployed API.
- The guarded inventory flow exercises add/update/delete from the Profile inventory surface. It uses package name as the current stable identifier and avoids flaky rename input in UIAutomator.
- The app robot launch path no longer force-stops the target package during instrumentation because that can kill the process hosting `TestRunner`.

## Run Modes

Default safe run:

```bash
./gradlew connectedDebugAndroidTest --no-daemon
```

Read-only gross check against a specific backend:

```bash
./gradlew connectedDebugAndroidTest --no-daemon \
  -Pandroid.testInstrumentationRunnerArguments.laundryhub.e2e.apiBaseUrl=https://<backend-host>/api
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
- Prefer a stable USB connection for long connected runs. Wi-Fi ADB previously dropped during Gradle device service/property queries and produced `Unknown API Level` / `No compatible devices`, but the latest connected run completed after reconnecting the device.
- Compose semantics are the primary selector path. The app robot also has a hierarchy-bounds fallback for package option cards because UIAutomator can intermittently miss clickable Compose nodes while the hierarchy dump still contains the semantic marker.

## Verification

- Master baseline worktree: `./gradlew testDebugUnitTest` passed.
- Master baseline worktree: `./gradlew connectedDebugAndroidTest` passed.
- KMP branch: `./gradlew :app:testDebugUnitTest :backend:test --no-daemon` passed.
- KMP branch: `./gradlew :shared:jvmTest --no-daemon` passed.
- KMP branch: `./gradlew assembleDebug --no-daemon` passed.
- KMP branch: `./gradlew assembleRelease --no-daemon` passed.
- KMP branch: `./gradlew :app:assembleDebugAndroidTest --no-daemon` passed.
- KMP branch after inventory edit-mode fix: `./gradlew :app:testDebugUnitTest :backend:test --no-daemon` passed.
- KMP branch after inventory edit-mode fix: `./gradlew assembleRelease --no-daemon` passed with R8/minification.
- KMP branch after inventory edit-mode fix: `./gradlew :app:connectedDebugAndroidTest --no-daemon` passed on `SM-S931B - 16`: Gradle reported 21 finished, 0 failed, 4 skipped.
- The skipped connected tests were the four guarded mutating flows because the safe run intentionally did not pass sandbox mutation arguments. The signed-in shell smoke passed in the latest safe connected run.
- KMP branch after deployed backend: guarded mutating order, outcome, and flicker/Home visibility flows passed with `laundryhub.e2e.mutating=true` and `laundryhub.e2e.target=sandbox`: 3 tests completed, 0 failed, 0 skipped.
- KMP branch after deployed backend: `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest --no-daemon` passed on `SM-S931B - 16`: 2 tests completed, 0 failed.
- Deployed backend package API was verified with create, update-by-name, delete-by-name, and post-delete list checks. The package delete response reported `sheetSynced=true`.
- Focused inventory UI E2E attempts compiled and launched. One signed-in attempt exposed the edit-mode bug fixed in this pass; later focused reruns skipped because the target app started at onboarding after reinstall. That is a device auth precondition, not a package API failure.
- Coverage reporting fix: `./gradlew clean testDebugUnitTest jacocoTestReport --no-daemon` passed after the CI coverage path update.
- Coverage reporting fix: `./gradlew :app:testDebugUnitTest --no-daemon --stacktrace` passed.
- Coverage reporting fix: `./gradlew jacocoTestReport --no-daemon` passed, and the app plus backend JaCoCo XML files were present with nonzero counters.

## Remaining E2E Risk

- The inventory UI mutating test still needs one signed-in-device rerun before the full UI E2E matrix is complete. Backend package create/update/delete, Sheets delete sync, and the Android edit-mode regression contract are already verified.
- The guarded inventory UI flow requires the target app to be logged in after Gradle installs the debug APK for the focused run.

## E2E Definition Of Done

- Default `connectedDebugAndroidTest` stays green without mutating live data.
- The signed-in shell smoke passes on a logged-in device.
- Mutating add/update/delete tests run only against a confirmed sandbox backend/database.
- Order, outcome, inventory, history, home refresh, and profile/settings all have either contract coverage or a real app-flow smoke test.
- Macrobenchmark results are recorded in `docs/core/performance/README.md` only after the scenario runs against a stable, safe target.
