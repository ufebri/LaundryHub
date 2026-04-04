# Spreadsheet Setup UI

Status: active living brief
Last updated: 2026-04-04
Primary area: `ui/spreadsheet`

## Goal

Keep spreadsheet setup easy to understand and limited to the moments when a user actually needs to connect a sheet.

## Current Behavior

The setup screen uses a step-based flow:

1. connect Google Sheets access
2. add and validate spreadsheet URL or ID
3. open the sheet in Google Sheets only when access is still missing

The screen also:

- visually locks step 2 until Sheets access is ready
- keeps the Google Sheets handoff as a recovery path, not a primary action
- gives the user a direct way to sign out and return to onboarding when the wrong Google account was used
- blocks setup success when the account can only view the spreadsheet and cannot edit it
- asks previously saved connections to re-validate once when the app needs stronger spreadsheet checks than older builds stored
- keeps the header compact so the first two setup steps stay visible sooner on smaller devices
- maps setup errors to shorter user-facing copy instead of showing raw Google or API responses
- keeps `Request Access` hidden for auth and app-configuration failures so users are not asked to request spreadsheet sharing for the wrong problem
- opens the Google Sheets app first when available, then falls back to a browser if Sheets is not installed

## Important Decisions

- spreadsheet setup is for first connection and reconnect scenarios, not a screen users should hit on every launch
- UI copy is intentionally lighter than the first draft so setup feels guided without overexplaining
- account switching from setup uses sign-out first, then re-entry through onboarding, so the app never mixes setup progress with the wrong signed-in account
- setup should stop non-editor accounts before they enter the main app, instead of waiting for order submission or outcome updates to fail later
- step 3 should clearly read as a Google Sheets handoff, not as a LaundryHub-owned request workflow
- request access must only appear for actual spreadsheet-sharing problems, not for Google auth reconnect issues or missing Google Drive API project setup
- the app does not create Google Drive access proposals itself; the user must trigger Google’s native `Request access` flow after the app opens the sheet

## Verification

These commands passed across setup UI updates:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
