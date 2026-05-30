# Profile UI Brief

## Goal

Profile owns operational settings that should be safe to use during normal store work. Sync settings are treated as a manual data-reconciliation surface, not a background automation control panel.

## Current Behavior

- The Profile settings row now describes Sync Master Data as a place to check differences before syncing.
- Sync Settings uses a `Check differences` primary action. It compares Google Sheets and the app database before any write happens.
- Sync Settings now separates app-owned data differences from Sheet-owned reporting-cache freshness. Supabase repair remains scoped to Orders, Outcomes, Packages, and Deletes, while Gross/Summary mismatches are presented as a reporting-cache refresh from Sheet.
- When differences exist, the user gets a confirmation sheet with `Sync now` and `Not now`.
- `Not now` dismisses the preview and does not schedule reminders because this is a manual workflow.
- During a confirmed run, the screen shows progress by backend stage and item counts.

## Decisions

- Removed user-facing auto-sync interval, pull schedule, and two-way source controls from the screen.
- Kept source selection, but limited it to `Google Sheets` and `App Database`.
- Google Sheets is the temporary default source while production users are relying on the direct-to-Spreadsheet app path.
- Home refresh no longer triggers manual sync. The sync menu is the only Android-owned entrypoint for cross-store reconciliation.

## Verification

- `./gradlew testDebugUnitTest --no-daemon`
