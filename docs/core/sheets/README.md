# Core Sheets

Status: active living brief
Last updated: 2026-04-04
Primary area: `core/sheets`, `ui/spreadsheet`, parts of `ui/profile`

## Goal

Support one app build for many laundries by using runtime spreadsheet configuration instead of a hardcoded spreadsheet ID.

## Current Behavior

### Spreadsheet connection

- spreadsheet ID is no longer read from `BuildConfig` at runtime
- the active spreadsheet is stored locally through spreadsheet config
- spreadsheet config is scoped per signed-in Firebase user, not shared globally on the device
- all sheet-backed repositories resolve the current spreadsheet ID dynamically

### First-time setup gate

Current app flow:

1. `Login`
2. `Spreadsheet Setup` if no spreadsheet has been connected yet
3. `Main App`

If a spreadsheet was already saved before:

- the app opens the main shell directly on next launch for that same signed-in account
- it does not force setup again on every cold start
- manual validation remains available from Profile
- configs saved before the editor-access validation upgrade must pass one more setup validation before they can skip setup again

If the user signs out and logs back in with a different account:

- the new account starts with its own spreadsheet config state
- it is sent to spreadsheet setup when no spreadsheet has been connected for that account yet
- the previous account keeps its own saved spreadsheet connection

### Spreadsheet validation

Setup validates spreadsheet input before first connection:

- URL or ID can be normalized
- spreadsheet is reachable
- the signed-in account must have spreadsheet-level edit access, not just view access
- required tabs exist:
  - `summary`
  - `gross`
  - `income`
  - `notes`
  - `outcome`
- required headers exist for the expected tabs

### Missing access recovery

When the current Google account still cannot use the spreadsheet:

- setup stops before entering the main app
- setup offers a direct handoff to Google Sheets
- the app opens the sheet in Google Sheets when possible, or falls back to a browser
- the user then uses Google’s native `Request access` flow there
- no Firestore workspace or request record is required for this recovery path
- the app no longer ships Firestore-only spreadsheet request/workspace layers

### User-based Sheets auth

- Google Sheets access now runs through the signed-in Google user account
- setup can distinguish:
  - missing Google Sheets OAuth scope
  - missing spreadsheet sharing permission
- setup now expects both spreadsheet access and the extra metadata scope needed to confirm whether the current account can edit the file
- the Sheets client now reuses a cached access token from `AuthorizationClient` instead of silently re-authorizing on every API call
- validation treats Google Drive API project misconfiguration differently from real spreadsheet sharing denial, so editor accounts are less likely to be misrouted into `Request Access`
- project setup must enable both Sheets API and Drive API, because editor-access verification uses Drive file capabilities

## Important Decisions

### No hardcoded runtime spreadsheet

This is the foundation for a reusable multi-tenant app instead of one build per laundry.

### Setup is a first-connect flow, not a repeated checkpoint

The app should only send users to setup when:

- no spreadsheet has been connected yet
- a different signed-in account has no saved spreadsheet connection
- the user explicitly changes spreadsheet later

### Google owns the request-access flow

LaundryHub does not create Google Drive access proposals itself.

If sharing is missing, the app only helps the user open the sheet so Google Sheets can handle the request directly.

## Affected Flows

- first login for a new user goes through spreadsheet setup
- returning users with a saved spreadsheet skip setup
- `Request Access` is now a Google Sheets handoff instead of a LaundryHub backend workflow
- sheet-backed features fail less ambiguously because spreadsheet configuration is explicit

## Verification

These commands passed across the sheets/platform updates:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests com.raylabs.laundryhub.ui.spreadsheet.SpreadsheetSetupViewModelTest
```

## Follow-Up Notes

- add a user-facing revoke or reconnect flow for Google Sheets authorization
- consider better diagnostics when saved spreadsheet access later breaks outside setup
- protected tabs or protected ranges can still block specific writes even when spreadsheet-level editor access exists; current setup validation is meant to catch the larger and more common "viewer only" case early
