# Theme UI

Status: active living brief
Last updated: 2026-04-12
Primary area: `ui/theme`, shell-level surfaces that follow `MaterialTheme.colors`

## Goal

Keep the app's dark and light foundations internally consistent before sweeping screen-level hardcoded colors.

## Current Behavior

### Theme foundation

- dark theme now uses a true dark `background` and dark `surface`
- dark theme no longer mixes a dark app background with a bright light-mode `surface`
- dark theme now keeps both `onBackground` and `onSurface` light so screens that follow `MaterialTheme.colors` stay readable by default
- light theme now uses a slightly softer app `background` while keeping cards and sheets on a clean white `surface`
- both light and dark palettes now define `error` and `onError` explicitly
- typography and shape source-of-truth now start from the same `ui/theme` area instead of being left as mostly screen-local decisions

### Step 1 scope

This pass changes the palette contract and fixes the two immediate regressions surfaced during dark-mode review:

- [Theme.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/theme/Theme.kt)
- [Color.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/theme/Color.kt)
- [ThemeTokens.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/theme/ThemeTokens.kt)
- [Type.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/theme/Type.kt)
- [Shape.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/theme/Shape.kt)
- [DatePickerField.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/component/DatePickerField.kt)
- [SpreadsheetSetupScreen.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/spreadsheet/SpreadsheetSetupScreen.kt)
- [OrderBottomSheetScreen.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/order/OrderBottomSheetScreen.kt)
- [OutcomeBottomSheet.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/component/OutcomeBottomSheet.kt)
- [OutcomeScreenView.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/outcome/OutcomeScreenView.kt)
- [LaundryHubStarter.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/LaundryHubStarter.kt)
- [AppConfirmationDialog.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/component/AppConfirmationDialog.kt)
- [ProfileScreenView.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/profile/ProfileScreenView.kt)
- [SelectionControls.kt](/Users/ray/StudioProjects/LaundryHub/app/src/main/java/com/raylabs/laundryhub/ui/component/SelectionControls.kt)

Additional fixes included in the same step:

- date fields used by order and outcome flows now inherit text, label, icon, and border colors from `MaterialTheme`
- spreadsheet setup now uses light/dark-aware local surface tokens instead of assuming a bright white/pastel screen in every theme
- the first batch of reusable semantic color tokens now lives in `ui/theme/ThemeTokens.kt` instead of staying as screen-local literals
- typography baseline now lives in `Type.kt`, and custom tiny text treatments start moving out of screen-local `copy(fontSize = ...)`
- shape baseline now lives in `Shape.kt`, and the shared modal-sheet/card/pill shapes start moving out of repeated `RoundedCornerShape(...)`
- confirmation dialogs now use an app-owned dialog surface instead of relying on default `AlertDialog` chrome
- popup-based dropdown surfaces are no longer the preferred pattern for high-focus selection flows because dark-mode window chrome produced visible corner artifacts on real devices
- app-owned selection surfaces now live in `SelectionControls.kt`, and important flows are moving to in-form cards or chips when that feels lighter than opening another layer
- package choices now prefer horizontal cards when the user needs richer context such as price and turnaround
- short option groups like payment method now prefer choice-chip treatment instead of popup chrome

It does not yet fix:

- bottom navigation hardcoded colors
- custom profile palette behavior
- reusable cards and list items that still assume white backgrounds or black text
- full screen-by-screen dark-mode sweep outside the first selection-related flows

## Important Decisions

- palette repair comes before component-by-component cleanup
- this step is meant to improve every screen that already respects `MaterialTheme.colors`
- the repo is moving toward one centralized theme area as the source of truth for shared colors
- that source of truth is intentionally split by responsibility:
  - `Color.kt` for base palette values
  - `Theme.kt` for light/dark palette wiring
  - `ThemeTokens.kt` for semantic app tokens consumed by screens
- `Type.kt` for the shared typography baseline and reusable text-style extensions
- `Shape.kt` for the shared shape baseline and reusable shape extensions
- custom hardcoded screen palettes should be cleaned up in later passes instead of being mixed into the theme-foundation step

## Recommended Review Screens

For this palette-only pass, the most useful screens to review are:

- `Home`
- `Reminder Intro`
- `Reminder Inbox`
- `Profile`
- `Spreadsheet Setup`

Focus on:

- whether dark mode finally feels dark in the background and card surfaces
- whether text remains readable on app-level surfaces
- whether light mode still feels clean and not dull

## Verification

```bash
./gradlew assembleDebug --no-daemon
```
