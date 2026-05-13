# Performance Brief

## 2026-05-09 Macrobenchmark Baseline

This baseline was captured after hardening the add/delete macrobenchmark flows against UIAutomator timing drift. The run used the benchmark build on a connected physical `SM-S931B - 16` device, with `BaselineProfileMode.UseIfAvailable`, `warmupIterations = 0`, and one measured iteration per flow.

Command:

- `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest --no-daemon`

Results:

| Flow | Main Timings | Frame CPU | Frame Overrun |
| --- | --- | --- | --- |
| Add order -> pending card | `open_add_order_ms=638`, `submit_to_success_ms=2092`, `success_to_pending_ms=1867`, `total_flow_ms=9973` | `P50=4.40ms`, `P90=7.54ms`, `P95=8.36ms`, `P99=11.95ms` | `P50=3.27ms`, `P90=5.33ms`, `P95=6.15ms`, `P99=10.00ms` |
| Delete order -> history update | `open_history_ms=6159`, `delete_to_success_ms=1882`, `total_flow_ms=8960` | `P50=2.55ms`, `P90=4.18ms`, `P95=5.15ms`, `P99=11.26ms` | `P50=-3.01ms`, `P90=-0.23ms`, `P95=1.16ms`, `P99=7.61ms` |

Notes:

- Full Add + Delete macrobenchmark passed on the connected device.
- The Add flow now measures the app's visible success feedback and the time until the submitted order card is visible in Home.
- The Delete flow creates its own order setup, then measures opening History and deleting that generated order.
- This is a live-backend/device baseline, not a master-branch delta yet. Re-run the same command on `master` with the same device and build type before making a branch-to-master performance claim.
- The Add flow leaves generated unpaid benchmark orders in the test environment. Clean them up deliberately if the shared test data needs to be reset.

## 2026-05-09 Add Order Backend ID Allocation

The Add Order optimization was corrected to avoid stale local id assumptions. Android no longer requests or increments the next order id. `POST /api/orders` now owns id allocation on the backend and returns the created `orderId` to the app for snackbar and benchmark verification.

Backend note:

- The current database schema keeps order ids as strings for Sheets compatibility.
- The backend serializes id allocation with a Postgres transaction-level advisory lock before calculating max numeric id + 1 and inserting the row.
- This avoids the multi-device race where two clients could read the same next id before either write finishes.

Verification:

- `./gradlew :app:testDebugUnitTest --tests com.raylabs.laundryhub.ui.order.OrderViewModelTest --tests com.raylabs.laundryhub.core.data.repository.LaundryRepositoryImplTest --no-daemon` passed.
- `./gradlew :backend:test --no-daemon` passed.

Benchmark status:

- Add Order macrobenchmark was rerun on a charged physical `SM-S931B - 16` device after implementing Optimistic UI.
- Results for backend-owned ID allocation with Optimistic UI: `open_add_order_ms=644`, `submit_to_success_ms=4026`, `success_to_pending_ms=1897`, `total_flow_ms=12813`.

Notes:

- In that benchmark run, `success_to_pending_ms` dropped from about 10.6 seconds to 1.8 seconds after the Optimistic UI implementation.
- The flow captures the allocated id from the backend response instead of relying on a local id guess.
Next command, only on a safe mutating target:

- `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.raylabs.laundryhub.macrobenchmark.AddOrderFlowBenchmark`

## 2026-05-10 Stabilization Check

This pass was a correctness and recovery pass, not a new measured performance refactor. The main runtime-facing changes were immediate success feedback after backend writes, silent follow-up refreshes, local deletion overlays, and backend-owned IDs for outcomes. Those should make the app feel calmer, but they are not a benchmark claim until the same connected scenario is measured again.

## 2026-05-10 Deployed Backend Macrobenchmark

After the backend deployment was confirmed and the benchmark target was treated as safe for sandbox mutations, the connected macrobenchmark was rerun on the same physical `SM-S931B - 16` device.

Command:

- `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest --no-daemon`

Results:

| Flow | Main Timings | Frame CPU | Frame Overrun |
| --- | --- | --- | --- |
| Add order -> pending card | `open_add_order_ms=1141`, `submit_to_success_ms=3149`, `success_to_pending_ms=1919`, `total_flow_ms=10871` | `P50=4.8ms`, `P90=7.9ms`, `P95=9.0ms`, `P99=21.9ms` | `P50=1.7ms`, `P90=5.9ms`, `P95=7.4ms`, `P99=21.3ms` |
| Delete order -> history update | `open_history_ms=2336`, `delete_to_success_ms=2369`, `total_flow_ms=6272` | `P50=3.7ms`, `P90=5.4ms`, `P95=6.2ms`, `P99=13.0ms` | `P50=-1.8ms`, `P90=1.7ms`, `P95=4.3ms`, `P99=10.6ms` |

Notes:

- Full Add + Delete macrobenchmark passed on the connected device: 2 tests completed, 0 failed.
- The add benchmark's generated order was deleted after the run.
- The delete benchmark created and removed its own generated order.
- A backend delete response during cleanup reported `sheetSynced=true`, which confirms the deployed backend has the Sheets sync configuration active for that delete path.
- These are deployed-backend numbers for the KMP branch. They should not be described as a master-vs-branch performance delta until the same device/build/scenario is measured on the comparison branch.

## Verification

- `./gradlew :app:testDebugUnitTest :backend:test --no-daemon` passed.
- `./gradlew :shared:jvmTest --no-daemon` passed.
- `./gradlew :macrobenchmark:assembleBenchmark` passed.
- `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest --no-daemon` passed on `SM-S931B - 16`: 2 tests finished, 0 failed.
- `./gradlew assembleRelease` passed with R8/minification.
- `./gradlew :app:connectedDebugAndroidTest --no-daemon` passed on `SM-S931B - 16`: Gradle reported 21 finished, 0 failed, 4 skipped.

## Notes

- An earlier connected run was blocked by Wi-Fi ADB/device transport. After reconnecting the device and fixing the robot launch path, the safe connected suite passed. The signed-in shell smoke passed in the latest safe connected run.
- Guarded macrobenchmarks still mutate backend data. Keep using a confirmed sandbox or an explicit cleanup plan before running them again.
- Before claiming a master-vs-branch E2E performance result, rerun the same benchmark scenario on a stable device/build pair and record the metric names and deltas here.
- Keep the test device awake and prefer a stable USB connection before connected test runs.
