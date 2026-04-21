# Profile UI

Status: active living brief
Last updated: 2026-04-19
Primary area: `ui/profile`

## Goal

Keep Profile as a compact maintenance hub for account, spreadsheet, store setup, and app settings.

## Current Behavior

### Layout

- Profile uses a single scrollable content flow
- lower sections stay reachable on shorter devices
- sections are grouped into:
  - `Store`
  - `Spreadsheet`
  - `Access Requests`
  - `Settings`
  - `Account`

### Spreadsheet management

Profile can now:

- show the connected spreadsheet name
- show a shortened spreadsheet ID
- revalidate the current spreadsheet
- change spreadsheet through a bottom confirmation sheet flow

`Change Spreadsheet` now uses an in-app bottom confirmation sheet with a small Lottie visual, a shorter explanation, and two quick bullets so the impact is easier to scan before continuing.

The action still clears the saved connection and lets the app return to setup.

### Access request review

If the current signed-in user is the registered spreadsheet owner, Profile can:

- list pending spreadsheet access requests
- approve a request
- reject a request

Important limitation:

- app approval updates LaundryHub request state only
- spreadsheet sharing in Google still has to exist separately

### UI polish

- the top account area is now compact instead of text-heavy
- section headers no longer include repetitive explanatory subtitles
- spreadsheet request status is not duplicated when the badge already shows it
- secondary spreadsheet actions use a transparent outlined button style
- the spreadsheet secondary action label is shortened to avoid awkward wrapping on narrow phones

### Inventory

Profile still routes to a dedicated Inventory screen, and that screen now supports real package management against the spreadsheet-backed package master.

Inventory can now:

- pull to refresh package data from the connected spreadsheet
- show package master rows in a scan-first layout with name, rate (`price/unit`), and duration
- normalize package rate display more consistently from spreadsheet values such as `10000` or `Rp 10.000`
- show real snackbar feedback when package reads fail
- add a new package from Inventory and sync it back to the `notes` sheet
- update an existing package from Inventory without opening a separate spreadsheet flow
- delete an existing package with an in-screen confirmation sheet
- open package actions from the package row itself instead of scattering edit/delete buttons inline
- treat empty `other packages` as a normal empty state instead of an error
- highlight package names seen in transactions that are not part of the package master yet
- prefill a new package form by tapping an unmatched package name from the audit section
- show Inventory as a dedicated route with an explicit back action instead of relying only on system back
- keep Inventory copy intentionally shorter so each section stays focused on one main action instead of stacking helper text
- keep generous top and bottom content breathing room so the first and last content blocks do not feel cramped against the app bar or screen edge

Important limitation:

- package rename or delete only changes the current master list for future order selection
- existing history is intentionally not migrated because past orders already store package names as text
- package writes target the current spreadsheet row identity, not historical transaction cleanup

## Inventory One-Go Outcome

The one-go Inventory pass was completed on 2026-04-19 with the current spreadsheet architecture still intact.

What shipped:

- package CRUD now writes to the `notes` sheet instead of stopping at a read-only audit surface
- add/edit/delete flows stay inside the current screen using in-screen action and confirmation sheets
- package rows are identified by spreadsheet row index when editing or deleting, so writes target the selected row instead of guessing by package name
- unmatched package names can open a prefilled add-package sheet, which makes the maintenance loop shorter when staff start typing new package names in remarks
- order package selection still reads from the same package master model, so new orders see the latest package list after Inventory changes
- package name normalization is still applied when comparing `other packages`, which keeps false positives lower even while CRUD is available

Safety decisions that were kept:

- spreadsheet schema was not changed
- historical orders are not migrated on package rename/delete
- package management remains spreadsheet-backed; there is still no second local package source of truth
- the latest UX cleanup intentionally removed several helper paragraphs in favor of clearer section titles, row affordances, and bottom-sheet follow-up actions

Read first if Inventory package management needs follow-up:

- `app/src/main/java/com/raylabs/laundryhub/ui/profile/inventory/InventoryScreenView.kt`
- `app/src/main/java/com/raylabs/laundryhub/ui/profile/inventory/InventoryViewModel.kt`
- `app/src/main/java/com/raylabs/laundryhub/ui/profile/inventory/state/PackageItem.kt`
- `app/src/main/java/com/raylabs/laundryhub/ui/component/InventoryPackageSheets.kt`
- `app/src/main/java/com/raylabs/laundryhub/core/domain/repository/GoogleSheetRepository.kt`
- `app/src/main/java/com/raylabs/laundryhub/core/data/repository/GoogleSheetRepositoryImpl.kt`
- `app/src/main/java/com/raylabs/laundryhub/core/domain/usecase/sheets/ReadPackageUseCase.kt`
- `app/src/main/java/com/raylabs/laundryhub/core/domain/usecase/sheets/GetOtherPackageUseCase.kt`
- `app/src/main/java/com/raylabs/laundryhub/core/domain/usecase/sheets/SubmitPackageUseCase.kt`
- `app/src/main/java/com/raylabs/laundryhub/core/domain/usecase/sheets/UpdatePackageUseCase.kt`
- `app/src/main/java/com/raylabs/laundryhub/core/domain/usecase/sheets/DeletePackageUseCase.kt`
- `app/src/main/java/com/raylabs/laundryhub/ui/order/OrderBottomSheetScreen.kt`
- `app/src/main/res/values/strings.xml`

## Important Decisions

- no separate `Disconnect Spreadsheet` action yet because the current next state is the same as `Change Spreadsheet`
- Profile should stay utility-first, so copy is intentionally brief
- spreadsheet confirmation now uses a bottom-sheet pattern instead of a generic dialog so it feels more like part of the app flow
- custom confirmation sheets should keep the current Profile screen visible behind the scrim instead of switching to a blank dialog-style background
- inventory package CRUD is now supported directly against the spreadsheet package master, so the screen no longer needs a fake read-only stance
- unmatched package names should be framed as a maintenance hint for the package master, not as a fatal error state
- package edit/delete targets the current spreadsheet row identity instead of relying on package-name matching alone

## Verification

These commands passed across the profile updates:

```bash
./gradlew testDebugUnitTest --tests com.raylabs.laundryhub.ui.inventory.InventoryViewModelTest --no-daemon
./gradlew testDebugUnitTest --tests com.raylabs.laundryhub.ui.inventory.state.PackageItemTest --no-daemon
./gradlew testDebugUnitTest --tests com.raylabs.laundryhub.core.domain.usecase.sheets.GetOtherPackageUseCaseTest --no-daemon
./gradlew testDebugUnitTest --tests com.raylabs.laundryhub.core.domain.usecase.sheets.PackageCrudUseCasesTest --no-daemon
./gradlew testDebugUnitTest --tests com.raylabs.laundryhub.core.data.repository.GoogleSheetRepositoryImplTest --no-daemon
./gradlew assembleDebug
./gradlew assembleDebug --no-daemon
./gradlew testDebugUnitTest
```

## Follow-Up Notes

- consider adding `Open Spreadsheet` or `Copy Spreadsheet ID` only if they solve a real user need
- if Profile grows further, consider collapsible sections before adding more descriptive text
- if package names become messy in real usage, consider adding a lightweight package-alias policy before changing how historical orders are interpreted
- if spreadsheet edits happen concurrently outside the app more often, consider an extra stale-row guard before package update/delete writes
