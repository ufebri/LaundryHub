# Core API Brief

## Goal

LaundryHub is in the KMP cutover phase where Android talks to a Ktor backend instead of using the legacy direct Google Sheets repository path. The API contract should keep the existing app flows stable while the backend owns PostgreSQL and optional Sheets sync.

## Current Contract

- `GET /api/orders` supports paging plus `filter`, `startDate`, `endDate`, `searchQuery`, and `sort`.
- `GET /api/orders/{id}` returns one order for edit flows.
- `GET /api/orders/last-id` remains available as a legacy/debug route, but Android Add Order no longer depends on it.
- `POST /api/orders` assigns the next order id on the backend and returns `status`, `message`, and `orderId`.
- `PUT /api/orders/{id}` and `DELETE /api/orders/{id}` cover existing-order mutations.
- `GET /api/outcomes`, `GET /api/outcomes/{id}`, and `GET /api/outcomes/last-id` mirror the outcome read flow.
- `POST /api/outcomes` now assigns the next outcome id on the backend and returns `status`, `message`, and `outcomeId`.
- `PUT /api/outcomes/{id}` and `DELETE /api/outcomes/{id}` cover existing-outcome mutations.
- Package create still posts package data. Package update/delete now target the original package name with `PUT /api/packages/{name}` and `DELETE /api/packages/{name}` instead of relying on a Sheets row index.
- Package, gross, and summary reads stay on backend endpoints.

## Backend Decisions

- Runtime database config must come from environment or `application.yaml`; no project-specific fallback credentials are allowed.
- Background Sheets jobs start only when `SPREADSHEET_ID` is configured.
- Migration/debug routes are disabled by default and require `ENABLE_MIGRATION_ROUTES=true`.
- Rows imported from Sheets migrations should be marked already synced. Rows created or updated through app writes remain unsynced until the relevant sync job confirms them.
- Order delete clears the matching Sheets row only when backend Sheets config is available. The app delete still succeeds if the database delete succeeds.
- Order creation id allocation belongs to `POST /api/orders`, not Android. `OrderRepository.insertWithNextId()` serializes allocation with a Postgres advisory lock, calculates max numeric id + 1, inserts the row, and returns the created id.
- `OrderRepository.getNextId()` remains for the legacy `last-id` route, but it is not part of the Android submit flow.
- Outcome creation now follows the same ownership model as orders. `OutcomeRepository.insertWithNextId()` serializes allocation with an advisory lock, calculates max numeric id + 1, inserts the row, and returns the created id.
- The batch Sheets job now processes unsynced orders, outcomes, and packages through the normal repository/service path. It should still be enabled only when the target spreadsheet configuration is intentionally set.
- Outcome and package deletes clear matching Sheets rows only when backend Sheets config is available. The database mutation remains the app-facing success boundary.
- Package name is the current stable external identifier for package update/delete. Android sends the original package name in the route and the edited package data in the body so rename flows can update the same database row.

## Android Decisions

- Android submits new orders without prefetching an id. The created id comes from the backend `POST /api/orders` response.
- Android submits new outcomes without prefetching an id. The created id comes from the backend `POST /api/outcomes` response.
- Query parameters are sent through Ktor request parameters instead of manual URL string assembly.
- Mutation calls now validate HTTP status codes explicitly. A non-2xx response is surfaced as a `Resource.Error`, using the API `message` field when available, so duplicate-order `409` responses cannot show a false submit success.
- Payment status helpers accept both canonical storage values and user-facing display values to keep edit and history flows stable.
- Home pending orders pass server-side search and sort options into paging.
- Inventory update/delete no longer depends on `sheetRowIndex`. The ViewModel uses the package name contract and treats package writes as successful as soon as the backend write succeeds, then refreshes silently.
- Outcome, History, and Inventory keep write success feedback separate from follow-up refresh work. A slow refresh should not make a confirmed write feel failed.

## Supabase Notes

- The app should continue to use the Ktor backend, not Supabase client-side table access.
- If Supabase Data API access is introduced later, RLS policies must be created first for every exposed table.
- Existing live non-order rows with `is_synced=false` should be reviewed before enabling the expanded outcome/package sync job against a shared production spreadsheet.

## Verification

- `./gradlew :app:testDebugUnitTest :backend:test --no-daemon`
- `./gradlew :shared:jvmTest --no-daemon`
- `./gradlew assembleRelease --no-daemon`
- `./gradlew :macrobenchmark:assembleBenchmark --no-daemon`
- `./gradlew :app:connectedDebugAndroidTest --no-daemon`
- `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest --no-daemon`

The latest safe connected instrumentation run passed on `SM-S931B - 16`: Gradle reported 21 finished, 0 failed, 4 skipped. The skips were the four guarded mutating flows because the safe run intentionally did not pass sandbox mutation arguments; the signed-in shell smoke passed.

After the branch backend deployment, guarded order and outcome E2E flows passed against the deployed API with backend-created ids. The connected add/delete macrobenchmark also passed against the same target.

Package API verification was run directly against the deployed backend because the device session returned to onboarding before the focused inventory UI rerun. Create, update-by-name, delete-by-name, and post-delete list checks all succeeded. The package delete response reported `sheetSynced=true`, confirming the deployed backend has the Sheets sync configuration active for that delete path.

Android also now treats a backend package with no valid Sheets row index as an edit-mode package by using the original package name as the edit marker. Connected inventory UI E2E still needs a signed-in focused rerun before the UI matrix is fully closed.
