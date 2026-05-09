# Core API Brief

## Goal

LaundryHub is in the KMP cutover phase where Android talks to a Ktor backend instead of using the legacy direct Google Sheets repository path. The API contract should keep the existing app flows stable while the backend owns PostgreSQL and optional Sheets sync.

## Current Contract

- `GET /api/orders` supports paging plus `filter`, `startDate`, `endDate`, `searchQuery`, and `sort`.
- `GET /api/orders/{id}` returns one order for edit flows.
- `GET /api/orders/last-id` returns the next usable numeric order ID for submit flows. The name is legacy, but Android expects the value to be safe for the next create.
- `POST /api/orders`, `PUT /api/orders/{id}`, and `DELETE /api/orders/{id}` cover mutations.
- `GET /api/outcomes`, `GET /api/outcomes/{id}`, and `GET /api/outcomes/last-id` mirror the outcome flow.
- Package, gross, and summary routes stay on backend CRUD/read endpoints.

## Backend Decisions

- Runtime database config must come from environment or `application.yaml`; no project-specific fallback credentials are allowed.
- Background Sheets jobs start only when `SPREADSHEET_ID` is configured.
- Migration/debug routes are disabled by default and require `ENABLE_MIGRATION_ROUTES=true`.
- Rows imported from Sheets migrations should be marked already synced. Rows created or updated through app writes remain unsynced until the relevant sync job confirms them.
- Order delete clears the matching Sheets row only when backend Sheets config is available. The app delete still succeeds if the database delete succeeds.
- `OrderRepository.getNextId()` owns the next-ID calculation by scanning numeric order IDs and returning max + 1. This preserves the legacy Sheets behavior where Android submits an already-incremented ID.

## Android Decisions

- Android first calls `GET /api/orders/last-id` for the next order ID.
- Until the deployed backend exposes that endpoint, Android falls back to paginating `GET /api/orders` with a larger page size and scanning all returned numeric IDs up to the guard limit. This prevents duplicate IDs caused by reading only the first page.
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
