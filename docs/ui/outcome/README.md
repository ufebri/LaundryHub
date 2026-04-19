# Outcome UI

Status: active living brief
Last updated: 2026-04-19
Primary area: `ui/outcome`

## Goal

Keep Outcome entry management simple by letting one tap open shared actions while preserving the existing outcome bottom sheet for edit and create.

## Current Behavior

Outcome rows still use the current `OutcomeBottomSheet` for editing, but the entry tap flow is now broader.

The current Outcome flow now:

- opens an in-screen action sheet when an outcome entry is tapped
- offers `Update outcome` and `Delete outcome` from the same reusable action surface used by History
- routes `Update outcome` back into the existing `OutcomeBottomSheet`
- shows a shared delete confirmation sheet before removing the outcome row from the spreadsheet
- uses a visible delete icon fallback inside the confirmation sheet when no delete-specific Lottie asset is provided
- refreshes the outcome list and last generated outcome id after submit, update, and delete
- asks Home to refresh summary and gross data after outcome-side mutations
- uses `strings.xml` for new success, error, and confirmation messages

## Important Decisions

- the reusable part is the action UX, not the repository delete pipeline
- Outcome keeps its own edit form owner and delete use case so the income and outcome data paths stay independent
- the action surfaces stay app-owned inside the current screen instead of opening dialog-like window layers

## Related Files

- [OutcomeScreenView.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/outcome/OutcomeScreenView.kt)
- [OutcomeViewModel.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/outcome/OutcomeViewModel.kt)
- [TransactionEntryActionSheet.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/component/TransactionEntryActionSheet.kt)
- [GoogleSheetRepositoryImpl.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/core/data/repository/GoogleSheetRepositoryImpl.kt)

## Verification

These commands passed after the change:

```bash
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
```
