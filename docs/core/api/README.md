# Core API Brief

## Goal

LaundryHub is in the KMP cutover phase where Android talks to a Ktor backend instead of using the legacy direct Google Sheets repository path. The API contract should keep the existing app flows stable while the backend owns PostgreSQL and optional Sheets sync.

## Current Contract

- `GET /api/orders` supports paging plus `filter`, `startDate`, `endDate`, `searchQuery`, and `sort`.
- `GET /api/health` returns a lightweight success payload for app startup backend availability checks. `GET /health` is a root-level alias for deployment platforms and manual smoke checks.
- `GET /api/orders/{id}` returns one order for edit flows.
- `GET /api/orders/last-id` remains available as a legacy/debug route, but Android Add Order no longer depends on it.
- `POST /api/orders` assigns the next order id on the backend and returns `status`, `message`, and `orderId`.
- `PUT /api/orders/{id}` and `DELETE /api/orders/{id}` cover existing-order mutations.
- `GET /api/outcomes`, `GET /api/outcomes/{id}`, and `GET /api/outcomes/last-id` mirror the outcome read flow.
- `POST /api/outcomes` now assigns the next outcome id on the backend and returns `status`, `message`, and `outcomeId`.
- `PUT /api/outcomes/{id}` and `DELETE /api/outcomes/{id}` cover existing-outcome mutations.
- `POST /api/notifications/token` registers the current device FCM token for backend push delivery.
- Package create still posts package data. Package update/delete now target the original package name with `PUT /api/packages/{name}` and `DELETE /api/packages/{name}` instead of relying on a Sheets row index.
- Package, gross, and summary reads stay on backend endpoints.
- `POST /api/sync/preview` compares Google Sheets and the app database without writing data.
- `POST /api/sync/runs` starts a confirmed manual sync from a preview id.
- `GET /api/sync/runs/{runId}` returns sync stage, progress counts, final difference count, and any error.
- `GET /api/sync/status` now reports queued push work, app-owned cross-store data differences, and reporting-cache differences separately. `syncQueueState` is based on app-owned drift only so Sheet-owned `gross`/`summary` cache drift does not look like failed Supabase data.
- `POST /api/sync/trigger` is deprecated for Android because manual sync now requires preview and confirmation first.

## Backend Decisions

- Runtime database config must come from environment or `application.yaml`; no project-specific fallback credentials are allowed.
- Deployment provider URLs must stay HTTPS because Android Remote Config rejects non-HTTPS remote backend URLs. Render is the temporary host; the Oracle Cloud Docker Compose + Caddy plan is deferred as a draft until A1 capacity is available.
- Background Sheets jobs start only when `SPREADSHEET_ID` is configured.
- Migration/debug routes are disabled by default and require `ENABLE_MIGRATION_ROUTES=true`.
- Rows imported from Sheets migrations should be marked already synced. Rows created or updated through app writes remain unsynced until the relevant push job confirms them.
- App database writes are the success boundary. Google Sheets is a mirror/reporting surface, so create/update/delete responses should not wait for Sheets API completion.
- App Database is the default sync master for app-owned write tabs. This keeps app-created rows moving to Google Sheets automatically after service restarts, including on Render free instances.
- Summary and gross reads prefer live Google Sheets data when `SPREADSHEET_ID` and the service account are configured, then fall back to the database cache. This keeps formula-driven reporting cards fresh while the backend still owns order/outcome writes.
- `GET /api/gross` returns gross rows newest month first for both live Sheet reads and database fallback. Sheet-backed reads apply `page` and `size` after month sorting so Home and Gross Detail do not accidentally treat the oldest Sheet row as the current gross value.
- Sheets push is near-real-time for all mutation routes. `SHEETS_PUSH_DEBOUNCE_MILLIS` defaults to `3000`, so rapid writes coalesce into one batch push instead of one Sheets request per API call.
- After a successful order, outcome, or package mutation, the backend schedules a debounced DB -> Sheets push only when App Database is the configured master source. This prevents the temporary Sheets-master recovery mode from pushing stale database rows back into Sheets.
- `gross` and `summary` are Sheet-owned reporting tabs. Backend reads can cache or fall back to database rows, and Sheet-source manual sync may refresh the database cache, but DB -> Sheets push/delete intentionally skips those tabs.
- The fallback DB -> Sheets job runs from the configurable interval, defaulting to 5 minutes, only when App Database is the configured master source. It retries app-owned rows that still have `is_synced=false`.
- Deletes are recorded in a durable sync delete outbox and cleared from Sheets by the same push path. Delete API responses report Sheets cleanup as queued, not complete.
- Order creation id allocation belongs to `POST /api/orders`, not Android. `OrderRepository.insertWithNextId()` serializes allocation with a Postgres advisory lock, calculates max numeric id + 1, inserts the row, and returns the created id.
- `OrderRepository.getNextId()` remains for the legacy `last-id` route, but it is not part of the Android submit flow.
- Outcome creation now follows the same ownership model as orders. `OutcomeRepository.insertWithNextId()` serializes allocation with an advisory lock, calculates max numeric id + 1, inserts the row, and returns the created id.
- The batch Sheets job processes unsynced orders, outcomes, packages, and queued deletes through the normal repository/service path. It should still be enabled only when the target spreadsheet configuration is intentionally set.
- Google Sheets push uses one key-column read per tab and batches updates/appends/clears where practical to reduce request pressure.
- Package name is the current stable external identifier for package update/delete. Android sends the original package name in the route and the edited package data in the body so rename flows can update the same database row. When a package rename succeeds, the backend records a delete event for the old package name so the legacy Sheet row can be cleared before/alongside writing the new package row.
- Reverse sync from Sheets to the database is no longer started as a scheduled background job. Pulling from Sheets now belongs to a confirmed sync run after preview, because unsupervised pull can overwrite app-owned data.
- Manual sync is now preview-confirm-progress: preview counts only-in-Sheets, only-in-database, changed rows, duplicate keys, suspicious header-like rows, and pending deletes; confirmed runs expose entity-stage progress; two-way sync is blocked until conflict resolution exists.
- Sync preview now includes row-level drift details for each entity: keys only in Sheets, keys only in the database, changed keys, duplicate key values, suspicious key values, and changed-field values. This keeps live drift repair reviewable without guessing from aggregate counts.
- If a Sheet-source preview has only Gross/Summary differences and Orders, Outcomes, and Packages are already clean, the confirmed run refreshes only the reporting cache from Sheets. It does not replay app-owned rows unnecessarily.
- DB -> Sheets sync now marks app-owned rows as synced when Google Sheets acknowledges the update/append for that row key. Supabase remains the source of truth, so read-back signature mismatches are logged as warnings instead of leaving confirmed Sheets writes stuck as pending.
- Read-back verification still normalizes common Sheet formatting differences for app-owned tabs, including currency/grouping text, payment-status casing/language (`lunas`/`Paid`), payment method casing, and numeric strings. It is a drift signal, not the success boundary for app-owned pushes.
- Google Sheets batch update requests explicitly send `valueInputOption=USER_ENTERED` in the request body, and Google Sheets API non-2xx responses now surface the response body in backend logs. This keeps update failures from collapsing into vague zero-acknowledgement errors.
- Income Sheet reads and key lookups ignore header-like rows found inside the data range, such as `orderID`. This prevents a duplicated header row in `income` from polluting previews or being treated as an order key.
- If pending app-owned rows exist but Sheets acknowledges zero written keys, the batch job records a sync failure instead of reporting a misleading `SUCCESS` with `changesCount=0`.
- Manual App Database -> Sheets runs push the full app-owned data set for orders, outcomes, and packages so already-synced but drifted rows can be repaired. Automatic background push remains limited to `is_synced=false` rows plus pending deletes.
- A read-only drift audit job runs when Sheets sync is configured. It previews `SUPABASE` as source of truth on a long interval, logs app-owned entity keys and changed rows when drift exists, and ignores Sheet-owned `gross`/`summary` as actionable Supabase failures. The interval defaults to 360 minutes and can be tuned with `SYNC_DRIFT_AUDIT_INTERVAL_MINUTES`.
- `/api/health` and its root-level `/health` alias must stay lightweight and independent of heavy sync work. They are used by Android startup gating, deployment platform checks, and manual smoke tests to answer whether the deployed API process is reachable.
- Order filtering uses the shared payment-status normalization helpers. `UNPAID` includes `Unpaid`, `belum`, and blank legacy rows; `PAID` includes `Paid`, `lunas`, and paid-by-method display labels. This keeps Home Pending Orders aligned with History data instead of letting paid rows pollute the pending page.
- Order date sorting and range checks accept both storage formats such as `15/05/2026` and display/import formats such as `15 May 2026` or `15 Mei 2026`, so pending-order sorting does not hide older imported rows behind unparseable dates.
- Outcome list ordering is date-first, then id. This prevents a newly-created or high-id outcome dated `8 May 2026` from appearing above a lower-id outcome dated `15 May 2026`.
- Notification token registration trims tokens, rejects blank payloads, and keeps enough column capacity for longer FCM registration tokens. Existing PostgreSQL deployments widen `device_tokens.token` during startup when needed.

## Android Decisions

- Android resolves the active backend API root from Firebase Remote Config at startup, falling back to the build-time `BASE_URL` when Remote Config is blank or invalid.
- Remote Config keys are `api_base_url`, `api_fallback_base_urls`, `api_maintenance_enabled`, `api_maintenance_message`, and `api_config_version`.
- Remote `api_base_url` values must be HTTPS. A host-only URL is normalized to `/api`; explicit API paths are preserved.
- `api_fallback_base_urls` accepts multiple comma-separated or newline-separated HTTPS URLs. Android checks the health path on each normalized API root in order: `api_base_url`, each `api_fallback_base_urls` entry, then the build-time fallback URL.
- The startup unavailable screen appears only when Remote Config explicitly enables maintenance or when all health candidates fail.
- Android submits new orders without prefetching an id. The created id comes from the backend `POST /api/orders` response.
- Android submits new outcomes without prefetching an id. The created id comes from the backend `POST /api/outcomes` response.
- Query parameters are sent through Ktor request parameters instead of manual URL string assembly.
- Mutation calls now validate HTTP status codes explicitly. A non-2xx response is surfaced as a `Resource.Error`, using the API `message` field when available, so duplicate-order `409` responses cannot show a false submit success.
- Payment status helpers accept both canonical storage values and user-facing display values to keep edit and history flows stable.
- Home pending orders pass server-side search and sort options into paging. Search input stays local while the user types; backend search is debounced and only starts once the query has at least two characters, so one-letter edits do not trigger repeated loading states.
- Home pending orders now rely on the same normalized paid/unpaid status semantics used by shared transaction mapping, so display values from the app and canonical values from Sheets are treated consistently.
- Device token registration waits until startup has resolved a healthy active backend root, then posts to the active API root's `notifications/token` route. This avoids sending FCM tokens to the wrong fallback URL or to a doubled `/api/api/...` path.
- Sync Settings now presents a manual `Check differences` workflow. The screen keeps only `Google Sheets` and `App Database` as source choices, separates app-owned drift from reporting-cache freshness, removes user-facing interval/pull schedule/two-way controls, and requires confirmation before any write or reporting-cache refresh.
- Home refresh and post-write refresh no longer call manual sync. They refresh visible backend data only; cross-store reconciliation belongs to Sync Settings.
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

Latest pending-order parity check:

- `./gradlew :backend:test --tests com.raylabs.laundryhub.backend.db.repository.OrderRepositoryTest`
- `./gradlew :backend:test`
- `./gradlew :app:testDebugUnitTest --tests com.raylabs.laundryhub.core.domain.model.sheets.OrderDataTest --tests com.raylabs.laundryhub.core.domain.model.sheets.TransactionDataTest`
- `./gradlew testDebugUnitTest`

Latest near-real-time Sheets mirror sync check:

- `./gradlew :backend:test`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew testDebugUnitTest`

Latest manual sync preview-confirm-progress check:

- `./gradlew :backend:test --no-daemon`
- `./gradlew testDebugUnitTest --no-daemon`

Latest drift-aware backend sync hardening check:

Latest backend health alias check:

- `./gradlew :backend:test --no-daemon`

- `./gradlew :backend:test --no-daemon`
- Follow-up verification-format fix also passed `./gradlew :backend:test --no-daemon`.
- Header-filter and zero-verified failure tests passed with `./gradlew :backend:test --no-daemon`.
- Backend coverage report generated with `./gradlew :backend:jacocoTestReport --no-daemon`.
- Follow-up write-ack fix passed `./gradlew :backend:test --no-daemon` and `./gradlew :backend:jacocoTestReport --no-daemon`.
- Full audit update-400 fix passed `./gradlew :shared:jvmTest --no-daemon`, `./gradlew :backend:test --no-daemon`, `./gradlew :backend:jacocoTestReport --no-daemon`, and `./gradlew :app:compileDebugKotlin --no-daemon`.
- Supabase read-only aggregate audit on May 28, 2026: `orders=1672`, `outcomes=274`, `packages=5`, `gross=15`, `summary=27`, `orders.header_like_rows=0`, all checked tables had `is_synced=false` count `0`.
- Deployed Render preview before this code was deployed still reported `33` differences: Orders `1` only-in-database and `18` changed, Gross `1` changed, Summary `13` changed. No live cleanup run was applied from that preview because the deployed endpoint exposes counts, not the exact row list required for safe mutation review.
- On May 29, 2026, live status after an order payment update showed one pending push for order `1674`. The row was updated in Supabase but remained `is_synced=false`, which matched the exact read-back verification risk; the local fix normalizes Sheet formatting before deciding whether a pushed row is verified.
- A follow-up live preview on May 29, 2026 showed `onlyInDatabase=1` and `suspiciousRows=1` for Orders while Supabase had no header-like order id, which points to a header-like row inside the Sheet data range. The local fix filters that row from Sheet reads and key lookup.

Latest search/outcome/notification registration check:

- `./gradlew :backend:test --tests com.raylabs.laundryhub.backend.db.repository.OutcomeRepositoryTest --tests com.raylabs.laundryhub.backend.db.repository.DeviceTokenRepositoryTest`
- `./gradlew :app:testDebugUnitTest --tests com.raylabs.laundryhub.ui.home.HomeViewModelTest --tests com.raylabs.laundryhub.ui.outcome.state.EntryItemTest --tests com.raylabs.laundryhub.ui.common.util.DateUtilTest --tests com.raylabs.laundryhub.core.fcm.DeviceTokenManagerTest`
- `./gradlew :backend:test`
- `./gradlew testDebugUnitTest`
- `./gradlew jacocoTestReport`
- `git diff --check`

The latest safe connected instrumentation run passed on `SM-S931B - 16`: Gradle reported 21 finished, 0 failed, 4 skipped. The skips were the four guarded mutating flows because the safe run intentionally did not pass sandbox mutation arguments; the signed-in shell smoke passed.

After the branch backend deployment, guarded order and outcome E2E flows passed against the deployed API with backend-created ids. The connected add/delete macrobenchmark also passed against the same target.

Package API verification was run directly against the deployed backend because the device session returned to onboarding before the focused inventory UI rerun. Create, update-by-name, delete-by-name, and post-delete list checks all succeeded. The package delete response reported `sheetSynced=true`, confirming the deployed backend has the Sheets sync configuration active for that delete path.

Android also now treats a backend package with no valid Sheets row index as an edit-mode package by using the original package name as the edit marker. Connected inventory UI E2E still needs a signed-in focused rerun before the UI matrix is fully closed.
