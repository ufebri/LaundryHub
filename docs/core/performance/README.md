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

## Verification

- `./gradlew :app:testDebugUnitTest :backend:test --no-daemon` passed.
- `./gradlew :shared:jvmTest --no-daemon` passed.
- `./gradlew :macrobenchmark:assembleBenchmark` passed.
- `./gradlew assembleRelease` passed with R8/minification.

## Notes

- `./gradlew connectedDebugAndroidTest --no-daemon` was attempted, but the device transport dropped during service/property queries and Gradle reported no compatible device. A separate earlier attempt exposed that force-stopping the target app from the robot can kill the instrumentation process, so the robot launch path was corrected before the final attempt.
- `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest --no-daemon` was intentionally skipped in this pass because the active benchmark flow mutates live backend data by creating orders. Use a confirmed sandbox or explicit cleanup plan before running it again.
- Before claiming a master-vs-branch E2E performance result, rerun the same benchmark scenario on a stable device/build pair and record the metric names and deltas here.
- Keep the test device awake and prefer a stable USB connection before connected test runs.
