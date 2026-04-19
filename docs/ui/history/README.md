# History UI

Status: active living brief
Last updated: 2026-04-19
Primary area: `ui/history`

## Goal

Let History entries open safe update and delete actions without creating a second edit flow for orders.

## Current Behavior

History rows are now tappable.

The current History flow now:

- opens an in-screen action sheet when a history entry is tapped
- offers `Update order` and `Delete order` from the same action surface
- routes `Update order` into the existing `OrderBottomSheet` edit flow instead of creating a parallel History-specific editor
- supports pull-to-refresh directly on the History list
- shows a shared delete confirmation sheet before removing the order row from the spreadsheet
- refreshes History data after a successful delete
- asks Home to refresh unpaid orders, today income, summary, and gross data after a History-side delete

## Important Decisions

- History reuses the existing order edit owner in `OrderViewModel` and `LaundryHubStarter`
- the action sheet and confirmation sheet are shared UI building blocks, but order delete still uses its own repository and use case path
- History now follows the same pull-to-refresh gesture style already used on Outcome
- user-facing action and confirmation copy was moved into `strings.xml`

## Related Files

- [HistoryScreenView.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/history/HistoryScreenView.kt)
- [HistoryViewModel.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/history/HistoryViewModel.kt)
- [TransactionEntryActionSheet.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/component/TransactionEntryActionSheet.kt)
- [LaundryHubStarter.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/LaundryHubStarter.kt)

## Verification

These commands passed after the change:

```bash
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
```
