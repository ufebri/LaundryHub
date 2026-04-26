# Core Performance

Status: active living brief
Last updated: 2026-04-26 09:34 WIB
Primary area: `core/performance`, `ui/order`

## Goal

Keep LaundryHub performance discussions grounded in a repeatable baseline instead of subjective feel.

This brief is the source of truth for:

- what we measure
- which user journey we treat as the first baseline
- how we record results
- how we classify the app as `fast`, `acceptable`, or `slow`

## Current Scope

The first tracked baseline should start from the full `Tambah Order` flow, not only from opening the entry sheet.

Initial journey to measure:

1. Launch app in `release` build.
2. Wait until Home is interactive.
3. Open the `Order` / `Tambah Order` entry flow.
4. Fill the minimum valid order data.
5. Submit the order.
6. Wait until the success state is visible.
7. Confirm the new order is reflected in the `Pending Order` section on Home.

This first baseline is intentionally practical.

Reason:

- it is a high-frequency flow
- it covers both UI entry speed and action-completion speed
- it also covers the follow-up refresh path that users actually care about after submit
- it is easier to discuss with product and QA because the result is user-facing end to end
- it gives us a stronger first benchmark before adding wider app journeys

## Baseline Rules

- Always measure from a `release` build.
- Prefer one repeatable mid-range Android device as the main local baseline device.
- Compare like-for-like:
  - same device
  - same build type
  - same scenario
- Record the exact timestamp with date and time, not date only.
- Use median results as the baseline reference, not the fastest run.

## Classification Rubric

Use this practical rubric for LaundryHub until real benchmark data suggests we should tighten it.

- `fast`
  - cold start around `< 1.2s`
  - warm start around `< 700ms`
  - no frozen frames
  - tap to visible response feels almost immediate
- `acceptable`
  - cold start around `1.2s - 2.0s`
  - warm start around `700ms - 1.2s`
  - no frozen frames
  - only minor occasional jank
- `slow`
  - cold start around `> 2.0s`
  - repeated visible jank
  - delayed tap feedback
  - any frozen-frame behavior

## Recording Format

Every baseline entry should include a timestamp with time, for example:

- `2026-04-24 21:45 WIB`

Recommended fields for each run:

- `captured_at`
- `app_version`
- `git_commit`
- `device`
- `android_version`
- `build_type`
- `scenario`
- `time_to_initial_display_ms`
- `time_to_full_display_ms`
- `submit_to_success_ms`
- `submit_to_pending_order_refresh_ms`
- `jank_notes`
- `frozen_frames`
- `verdict`
- `notes`

## First Baseline Table

Use this table for the first real recorded measurements of the full `Tambah Order` flow.

| Captured At | Device | Android | Build | Scenario | TTI Display | TTF Display | Submit To Success | Submit To Pending Refresh | Frozen Frames | Verdict | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `YYYY-MM-DD HH:mm WIB` | `<DEVICE>` | `<ANDROID_VERSION>` | `release` | `Home -> Open Tambah Order -> Submit Success -> Pending Order Updated` | `<TTI_MS>` | `<TTF_MS>` | `<SUBMIT_SUCCESS_MS>` | `<SUBMIT_PENDING_REFRESH_MS>` | `<COUNT>` | `<fast/acceptable/slow>` | `<NOTES>` |

## Planned Next Step

For now, keep the first baseline manual and lightweight.

After the first real measurements are recorded, expand to:

- `Cold start -> Home`
- `Open Spreadsheet Setup`
- `Tap Validate & Continue`

## Manual Baseline Prerequisites

The first local baseline run assumes the device is already ready to land on Home.

Required preconditions:

- the app is already signed in
- spreadsheet setup is already completed
- the main shell opens to Home instead of onboarding or setup
- the benchmark device should be a real device when possible, not an emulator

The first baseline flow intentionally uses the minimum valid order input:

- customer name
- price
- existing default package selection
- existing default payment method selection

Phone number is intentionally left blank so the flow stays inside LaundryHub and does not branch into WhatsApp after submit.

## How To Capture The First Baseline

Use a real device when possible and keep the flow practical.

Recommended run steps:

- install and open the `release` build
- make sure LaundryHub lands directly on Home
- create one real benchmark order with a unique customer name
- measure:
  - app launch to Home interactive
  - open `Tambah Order`
  - submit success
  - pending order refresh visibility

Useful quick checks before capturing:

- `adb devices -l`
- confirm LaundryHub opens straight to Home on the target device
- confirm spreadsheet setup is already valid on that device
- confirm the device is allowed to create a real benchmark order entry in the connected spreadsheet

## Debug Diagnostics

For local debugging, LaundryHub now includes `LeakCanary` as a `debugImplementation` dependency only.

Current intent:

- catch obvious Activity, Fragment, View, or Compose-hosting leaks while iterating locally
- keep the leak detector fully out of release builds
- use it as a debugging aid, not as the main source of truth for overall app speed

Practical notes:

- no extra app wiring is required for the default LeakCanary setup
- leak reports should appear through the standard LeakCanary notification flow in debug builds
- when needed, local logs can be filtered with `LeakCanary` in Logcat

This is intentionally separate from the main performance baseline.

Reason:

- `LeakCanary` helps answer whether the app is leaking memory
- it does not replace startup, jank, or end-to-end flow timing checks
- it is still useful because memory leaks often make a screen feel progressively heavier over time

## Macrobenchmark Baseline

LaundryHub now has a focused `:macrobenchmark` module again, but the scope stays intentionally narrow.

Current formal flow:

- `Home -> Open Add Order -> Submit -> Pending Order updated`

What this benchmark is for:

- measure the user-visible order flow on a real Android device
- keep the setup aligned with Android Macrobenchmark best practice instead of ad-hoc timing only
- give us a repeatable baseline before we optimize the submit path

Current benchmark file:

- `macrobenchmark/src/main/java/com/raylabs/laundryhub/macrobenchmark/AddOrderFlowBenchmark.kt`

Important prerequisites:

- use a real device when possible
- keep the device already signed in and already connected to a valid spreadsheet
- prefer a dedicated spreadsheet for benchmarking, because this flow creates real orders and does not auto-clean them yet
- keep the benchmark scope limited to the add-order flow before expanding to other screens

Recommended command:

- `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest --no-daemon`

Useful log filter:

- `adb logcat -s AddOrderFlowBenchmark`

Expected benchmark logs:

- `BENCHMARK_ITERATION ...`
- `BENCHMARK_SUMMARY ...`

The benchmark currently logs these flow timings per iteration:

- `open_add_order_ms`
- `submit_to_success_ms`
- `success_to_pending_ms`
- `total_flow_ms`

These logs are intended to populate the same `Baseline | Current | Delta` table after the first successful device capture.

### Captured Device Baseline

First successful real-device Macrobenchmark capture:

- captured at: `2026-04-26 01:26 WIB`
- device: `SM_S931B`
- Android: `16`
- build: `benchmark`
- scenario: `Home -> Open Add Order -> Submit -> Pending Order updated`

Current flow baseline from `BENCHMARK_SUMMARY`:

- `open_add_order_ms_median=661`
- `submit_to_success_ms_median=3633`
- `success_to_pending_ms_median=3926`
- `total_flow_ms_median=13412`

Current frame metrics from AndroidX Benchmark output:

- `frameCount=528`
- `frameDurationCpuMs P50=2.9`
- `frameDurationCpuMs P90=4.4`
- `frameDurationCpuMs P95=5.5`
- `frameDurationCpuMs P99=8.7`
- `frameOverrunMs P50=-2.9`
- `frameOverrunMs P90=0.5`
- `frameOverrunMs P95=0.7`
- `frameOverrunMs P99=4.2`

Practical reading:

- the add-order flow is now measurable end to end on a real device
- submit-to-success and pending-refresh are both in the multi-second range, which supports the earlier feeling that the flow is functionally smooth but still network-heavy
- use these values as the first comparison point before changing submit refresh orchestration or local caching strategy

## Post-Submit Refresh Refactor

The first optimization pass keeps the data model unchanged and focuses only on orchestration.

What changed:

- `HomeViewModel` now owns post-mutation refresh methods:
  - `refreshAfterOrderChanged()`
  - `refreshAfterOutcomeChanged()`
- order submit/update no longer waits for `fetchOrder`, `fetchTodayIncome`, `fetchSummary`, and `fetchGross` sequentially before closing the sheet
- the fixed `500 ms` post-submit delay was removed from the order sheet flow
- the sheet can dismiss and show submit success after the order write succeeds, while Home refresh runs in the background
- Home refresh still updates Pending Orders, Today Income, Summary, Gross, and reminder discovery through the same source-of-truth fetch methods

Local virtual-time guard:

- `refreshAfterOrderChanged()` with `1000 ms` artificial delay per fetch completes in `1000 ms`
- the same work would take `5000 ms` if the five Home refresh sections were accidentally made sequential

Latest real-device rerun after the refactor:

- captured at: `2026-04-26 09:34 WIB`
- device: `SM_S931B`
- Android: `16`
- build: `benchmark`
- scenario: `Home -> Open Add Order -> Submit -> Pending Order updated`

Flow timing comparison:

| Metric | Before refactor | After refactor | Delta | Reading |
| --- | --- | --- | --- | --- |
| `open_add_order_ms_median` | `661` | `652` | `-9 ms` | effectively unchanged |
| `submit_to_success_ms_median` | `3633` | `1245` | `-2388 ms` | major UX win; success feedback arrives much sooner |
| `success_to_pending_ms_median` | `3926` | `4655` | `+729 ms` | pending refresh still waits on live Sheets data and became the remaining bottleneck |
| `total_flow_ms_median` | `13412` | `11944` | `-1468 ms` | modest end-to-end improvement; perceived submit speed should feel meaningfully better |

Frame metrics after the refactor:

- `frameCount=423`
- `frameDurationCpuMs P50=4.1`
- `frameDurationCpuMs P90=6.4`
- `frameDurationCpuMs P95=7.5`
- `frameDurationCpuMs P99=10.3`
- `frameOverrunMs P50=-0.8`
- `frameOverrunMs P90=2.3`
- `frameOverrunMs P95=4.0`
- `frameOverrunMs P99=8.3`

Benchmark verifier note:

- pending-order verification still prefers filtering by the pending search field
- if the search field does not appear after the search button is tapped, the benchmark now falls back to scanning for `Order #<id>`
- this keeps the benchmark focused on the real product result instead of failing on one brittle UIAutomator interaction

## Post-Delete UI Optimization

The delete-order flow was optimized to avoid redundant network overhead.

What changed:

- `HistoryViewModel.deleteOrder()` now uses **Local State Mutation**.
- Instead of calling `loadHistory()` (which re-downloads the entire list from Google Sheets), the app now filters out the deleted item from the local `HistoryUiState` in memory immediately after a successful API response.
- This removes one full sequential network fetch from the user's wait time.

Latest real-device Macrobenchmark for Delete Flow:

- captured at: `2026-04-26 14:42 WIB`
- device: `SM_S931B` (via 192.168.1.65:36043)
- Android: `16`
- build: `benchmark`
- scenario: `Home -> History -> Tap Order -> Delete -> Success Snackbar`

Delete flow comparison:

| Metric | Before optimization | After optimization | Delta | Interpretation |
| --- | --- | --- | --- | --- |
| `open_history_ms_median` | `537` | `607` | `+70 ms` | normal UI noise |
| `delete_to_success_ms_median` | `2434` | `1716` | `-718 ms` | **Major win**; removed redundant list fetch |
| `total_flow_ms_median` | `5540` | `4871` | `-669 ms` | meaningful end-to-end improvement |

New benchmark file:

- `macrobenchmark/src/main/java/com/raylabs/laundryhub/macrobenchmark/DeleteOrderFlowBenchmark.kt`

## Local JVM Baselines

There are now two local, no-device baseline tests that can be run through normal JVM unit tests.

Current local baselines:

- `app/src/test/java/com/raylabs/laundryhub/core/data/repository/GoogleSheetRepositoryImplPerformanceBaselineTest.kt`
- `app/src/test/java/com/raylabs/laundryhub/ui/home/HomeViewModelPerformanceBaselineTest.kt`

What they cover:

- repository-side cost for mapping, filtering, and sorting a large `income` sheet when reading `Pending Order`
- virtual-time confirmation that `HomeViewModel.refreshAllData()` still behaves like a parallel refresh instead of silently becoming sequential

These are intentionally local baselines, not UX truth.

Reason:

- they can run without a connected Android device
- they are stable enough to compare before and after refactors
- they help catch logic-side performance regressions even when real UI timing is not available

Recommended command:

- `./gradlew testDebugUnitTest --tests com.raylabs.laundryhub.core.data.repository.GoogleSheetRepositoryImplPerformanceBaselineTest --tests com.raylabs.laundryhub.ui.home.HomeViewModelPerformanceBaselineTest --no-daemon`
- `./scripts/run_local_perf_baseline.sh`

Expected output:

- `PERF_BASELINE repository=GoogleSheetRepositoryImpl ...`
- `PERF_BASELINE owner=HomeViewModel method=refreshAllData ...`
- `PERF_BASELINE owner=HomeViewModel method=refreshAfterOrderChanged ...`

Tracked comparison table:

| Metric | Baseline | Current | Delta | Interpretation |
| --- | --- | --- | --- | --- |
| `GoogleSheetRepositoryImpl.readIncomeTransaction(FILTER.SHOW_UNPAID_DATA)` with `5000` rows | `221 ms` | `230 ms` | `+9 ms` | current local transform/filter cost is still acceptable; this small movement is normal local-test noise |
| `HomeViewModel.refreshAllData()` virtual-time orchestration with `1000 ms` per fetch | `1000 ms` | `1000 ms` | `0 ms` | refresh orchestration is still parallel, not sequential |
| `HomeViewModel.refreshAfterOrderChanged()` virtual-time orchestration with `1000 ms` per fetch | `5000 ms sequential equivalent` | `1000 ms` | `-4000 ms` | post-order Home refresh now uses the shared parallel orchestration instead of the old sequential caller path |

Initial capture:

| Captured At | Metric | Result |
| --- | --- | --- |
| `2026-04-25 07:13 WIB` | `GoogleSheetRepositoryImpl.readIncomeTransaction(FILTER.SHOW_UNPAID_DATA)` with `5000` rows | `221 ms` |
| `2026-04-25 07:13 WIB` | `HomeViewModel.refreshAllData()` virtual-time orchestration with `1000 ms` per fetch | `1000 ms` total, versus `5000 ms` sequential equivalent |
| `2026-04-26 01:47 WIB` | `GoogleSheetRepositoryImpl.readIncomeTransaction(FILTER.SHOW_UNPAID_DATA)` with `5000` rows | `230 ms` |
| `2026-04-26 01:47 WIB` | `HomeViewModel.refreshAllData()` virtual-time orchestration with `1000 ms` per fetch | `1000 ms` total, versus `5000 ms` sequential equivalent |
| `2026-04-26 01:47 WIB` | `HomeViewModel.refreshAfterOrderChanged()` virtual-time orchestration with `1000 ms` per fetch | `1000 ms` total, versus `5000 ms` sequential equivalent |

Update rule:

- keep `Baseline` fixed until we intentionally reset the benchmark generation
- update `Current` after each meaningful performance-related code change
- fill `Delta` as `Current - Baseline`
- if a new local run changes the shape of the benchmark itself, record that in notes before comparing numbers

## Verification

Latest successful verification:

- `./gradlew testDebugUnitTest --tests com.raylabs.laundryhub.ui.home.HomeViewModelTest --tests com.raylabs.laundryhub.ui.home.HomeViewModelPerformanceBaselineTest --no-daemon`
- `./scripts/run_local_perf_baseline.sh`
- `./gradlew testDebugUnitTest --no-daemon`
- `./gradlew :app:assembleBenchmark --no-daemon`
- `./gradlew :macrobenchmark:assembleBenchmark --no-daemon`
- `adb install -r app/build/outputs/apk/benchmark/app-benchmark.apk`
- `adb install -r macrobenchmark/build/outputs/apk/benchmark/macrobenchmark-benchmark.apk`
- `adb devices -l`
- `adb shell am instrument -w -e class com.raylabs.laundryhub.macrobenchmark.AddOrderFlowBenchmark com.raylabs.laundryhub.macrobenchmark/androidx.test.runner.AndroidJUnitRunner`
- `adb shell am instrument -w -e class com.raylabs.laundryhub.macrobenchmark.DeleteOrderFlowBenchmark com.raylabs.laundryhub.macrobenchmark/androidx.test.runner.AndroidJUnitRunner`
