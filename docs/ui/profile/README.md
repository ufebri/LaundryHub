# Profile UI

Status: active living brief
Last updated: 2026-04-04
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
- change spreadsheet through a confirmation flow

`Change Spreadsheet` clears the saved connection and lets the app return to setup.

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

## Important Decisions

- no separate `Disconnect Spreadsheet` action yet because the current next state is the same as `Change Spreadsheet`
- Profile should stay utility-first, so copy is intentionally brief

## Verification

These commands passed across the profile updates:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Follow-Up Notes

- consider adding `Open Spreadsheet` or `Copy Spreadsheet ID` only if they solve a real user need
- if Profile grows further, consider collapsible sections before adding more descriptive text
