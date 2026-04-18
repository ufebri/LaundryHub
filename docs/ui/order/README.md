# Order UI

Status: active living brief
Last updated: 2026-04-19
Primary area: `ui/order`

## Goal

Keep the order entry and update sheet usable in dark mode without relying on platform popup surfaces that can render visible corner artifacts.

## Current Behavior

The order bottom sheet now uses lighter in-form selectors:

- `Package` uses horizontal selectable cards
- `Payment Method` uses horizontal single-select chips

The current order flow now:

- keeps `Order Date` on the existing date picker field
- shows `Package` as horizontally scrollable cards with friendlier supporting text so the user can compare starting price and turnaround at a glance
- uses a clearer package card hierarchy:
  - line 1: package name
  - line 2 left: base rate such as `Rp10.000/kg`
  - line 2 right: turnaround badge such as `6 jam` or `3 hari`
- keeps the package cards horizontal, but gives them a little more height so longer package names still feel tidy
- uses a small `Base rate ...` helper directly under the package row so the selected package context stays visible in the form
- keeps the currently selected package visible in the form after selection
- shows `Payment Method` as horizontally scrollable choice chips inside the form
- makes selected payment chips use higher-contrast text so the active state is easier to read
- keeps the whole interaction in one form surface without opening a second popup or pseudo-page

## Important Decisions

- platform popup dropdowns are no longer the preferred pattern for order form selection because the dark-mode result was inconsistent across devices
- `Package` uses cards instead of chips because the package choice needs more context than a short label
- package cards now preserve the package `unit` from sheet data so the rate can be shown as `price/unit` inside the selection UI
- `Payment Method` uses chips because the option count is small and the labels are short enough to scan quickly
- shared selection building blocks now live in `ui/component/SelectionControls.kt`

## Related Files

- [OrderBottomSheetScreen.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/order/OrderBottomSheetScreen.kt)
- [SelectionControls.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/component/SelectionControls.kt)
- [OutcomeBottomSheet.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/component/OutcomeBottomSheet.kt)

## Verification

```bash
./gradlew assembleDebug --no-daemon
```
