# Core API Brief

## Goal

LaundryHub is in the KMP cutover phase where Android talks to a Ktor backend instead of using the legacy direct Google Sheets repository path. The API contract should keep the existing app flows stable while the backend owns PostgreSQL and optional Sheets sync.

## Current Contract

- `GET /api/orders` supports paging plus `filter`, `startDate`, `endDate`, `searchQuery`, and `sort`.
- `GET /api/orders/{id}` returns one order for edit flows.
- `GET /api/orders/last-id` remains available as a legacy/debug route, but Android Add Order no longer depends on it.
- `POST /api/orders` assigns the next order id on the backend and returns `status`, `message`, and `orderId`.
- `PUT /api/orders/{id}` and `DELETE /api/orders/{id}` cover existing-order mutations.
- `GET /api/outcomes`, `GET /api/outcomes/{id}`, and `GET /api/outcomes/last-id` mirror the outcome flow.
- Package, gross, and summary routes stay on backend CRUD/read endpoints.

## Backend Decisions

- Runtime database config must come from environment or `application.yaml`; no project-specific fallback credentials are allowed.
- Background Sheets jobs start only when `SPREADSHEET_ID` is configured.
- Migration/debug routes are disabled by default and require `ENABLE_MIGRATION_ROUTES=true`.
- Rows imported from Sheets migrations should be marked already synced. Rows created or updated through app writes remain unsynced until the relevant sync job confirms them.
- Order delete clears the matching Sheets row only when backend Sheets config is available. The app delete still succeeds if the database delete succeeds.
- Order creation id allocation belongs to `POST /api/orders`, not Android. `OrderRepository.insertWithNextId()` serializes allocation with a Postgres advisory lock, calculates max numeric id + 1, inserts the row, and returns the created id.
- `OrderRepository.getNextId()` remains for the legacy `last-id` route, but it is not part of the Android submit flow.

## Android Decisions

- Android submits new orders without prefetching an id. The created id comes from the backend `POST /api/orders` response.
- Query parameters are sent through Ktor request parameters instead of manual URL string assembly.
- Mutation calls now validate HTTP status codes explicitly. A non-2xx response is surfaced as a `Resource.Error`, using the API `message` field when available, so duplicate-order `409` responses cannot show a false submit success.
- Payment status helpers accept both canonical storage values and user-facing display values to keep edit and history flows stable.
- Home pending orders pass server-side search and sort options into paging.

## Supabase Notes

- The app should continue to use the Ktor backend, not Supabase client-side table access.
- If Supabase Data API access is introduced later, RLS policies must be created first for every exposed table.
- Existing live non-order rows with `is_synced=false` should be reviewed before any future non-order sync job is enabled.

## Verification

- `./gradlew testDebugUnitTest :backend:test :shared:jvmTest`
- `./gradlew assembleDebug`
- `./gradlew assembleRelease`
- `./gradlew jacocoTestReport`
- `./gradlew :macrobenchmark:assembleBenchmark`
- `./gradlew connectedDebugAndroidTest --no-daemon`

The connected device run passed on `SM-S931B - 16`. The safe default app instrumentation suite does not mutate live data. Manual guarded add-order E2E also passed after the app was explicitly pointed at the approved current environment: device logs showed `POST /api/orders` returning `201`, and a narrow Supabase check confirmed the generated `e2e*` row existed.

After the backend-owned order id allocation change, the focused Android repository/ViewModel tests and `:backend:test` passed. Connected Add Order E2E and macrobenchmark should be rerun only after the branch backend is deployed or otherwise serving the updated `POST /api/orders` response, and after the device is charged enough for AndroidX Benchmark.
