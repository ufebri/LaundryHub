# Sheets Sync Brief

## Goal

LaundryHub keeps Supabase/Postgres as the source of truth for app-owned orders, outcomes, and packages, while Google Sheets remains the reporting and mirror surface. Home summary values can drift when the Sheets mirror is behind the database, so sync failures need to be visible and recoverable.

## Current Behavior

- `summary` remains a Sheet-owned reporting tab.
- `GET /api/summary` reads live Google Sheets data when `SPREADSHEET_ID` is configured, then falls back to the database cache.
- `GET /api/gross` reads live Google Sheets data when `SPREADSHEET_ID` is configured, then falls back to the database cache. If the reporting rows do not contain the current month, the backend appends a current-month gross row computed from app-owned orders before sorting and paging.
- App-owned order, outcome, and package writes return success after the database write, then schedule a DB -> Sheets push.
- Sync status reports pending push work separately from reporting-cache differences so a successful app write does not look failed only because Sheets is still catching up.

## June 2026 Summary Drift Check

- Live Render `/api/summary` matched the Supabase `summary` cache, so the app was not receiving a different backend summary payload.
- Supabase `orders` had more rows than the `summary` key `Total Order Masuk`, and `Pending Orders` in summary did not match unpaid rows in `orders`.
- Live `/api/sync/status` reported pending push work and a failed sync because no verified order rows were acknowledged for the pending order set.
- The fix now lets batch sync mark a row as synced when Google Sheets either acknowledges the write or a post-write read-back already matches the database row. This covers idempotent retry cases where a row is already present in Sheets but the write response reports no changed cells.
- There was no `Juni 2026` gross row during the check, so Home Gross Income would use `Mei 2026` as the latest available reporting row.
- After the sync fix deployed, live sync status reported success and no unsynced orders, but `summary` and `gross` reporting rows were still stale because the Sheet/reporting data had not produced `Juni 2026`.
- The backend computes the current-month gross fallback from `orders` when the reporting rows miss the current month. For the June 6, 2026 crosscheck, Supabase had 23 June orders totaling `Rp674.000`, so the computed gross row is `Juni 2026`, `Rp674.000`, `23`, `Rp3.370`.

## Verification

- `./gradlew :backend:test --tests com.raylabs.laundryhub.backend.service.SheetsSyncServiceTest --no-daemon`
- `./gradlew :backend:test --no-daemon`
- `./gradlew :backend:test --tests com.raylabs.laundryhub.backend.routes.GrossRouteBehaviorTest --tests com.raylabs.laundryhub.backend.db.repository.OrderRepositoryTest --no-daemon`
