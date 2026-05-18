# Outcome Flow Brief

## Goal

Outcome should behave like Order: the backend owns ids, Android gives success feedback as soon as the write succeeds, and slow follow-up refresh work should not make a confirmed create/update/delete feel failed.

## Current Behavior

- Add Outcome opens from the Outcome tab as an in-screen bottom sheet with the current screen still visible behind the scrim.
- New outcomes are submitted with a blank id. `POST /api/outcomes` assigns the next id and returns it as `outcomeId`.
- Submit, update, and delete success callbacks run after the backend write succeeds. The list refresh runs afterward as silent follow-up work.
- Delete hides the removed outcome id locally so the row disappears immediately after backend success while Paging catches up.
- Outcome rows are shown newest-date first, with id used only as the tie-breaker within the same date.
- The FAB is hidden while the outcome sheet is visible so the sheet covers the same visual area as the Add Order flow.

## Important Decisions

- Android does not call `GET /api/outcomes/last-id` to create a new outcome. That route remains for legacy/debug use only.
- Write success and refresh failure are separate outcomes. The user should see the write result promptly, and any later refresh issue should be handled as a refresh problem.
- The sheet stays inside the Compose hierarchy instead of using a custom window-backed dialog, matching the modal surface direction used by Order.
- Backend Sheets sync for outcomes goes through the batch sync service when spreadsheet config is enabled.
- Date parsing accepts app storage dates and imported display dates such as `15 May 2026` and `15 Mei 2026`, so imported rows do not drift above newer outcome data just because their id is larger.

## Verification

- `./gradlew :app:testDebugUnitTest :backend:test --no-daemon`
- `./gradlew :shared:jvmTest --no-daemon`
- `./gradlew assembleRelease --no-daemon`
- `./gradlew :backend:test --tests com.raylabs.laundryhub.backend.db.repository.OutcomeRepositoryTest`
- `./gradlew :app:testDebugUnitTest --tests com.raylabs.laundryhub.ui.outcome.state.EntryItemTest --tests com.raylabs.laundryhub.ui.common.util.DateUtilTest`

## Follow-ups

- Add or rerun guarded mutating Outcome E2E against a confirmed sandbox backend/database.
- Capture outcome create/update/delete macrobenchmark numbers only after the same device/build/backend target is stable.
