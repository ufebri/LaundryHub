# Core Auth

Status: active living brief
Last updated: 2026-04-04
Primary area: `core/auth`, `core/firebase`, `ui/onboarding`, `ui/spreadsheet`

## Goal

Keep Google sign-in, Sheets authorization, and Firebase runtime dependencies aligned with the current app architecture.

## Current Behavior

### Sign-in

- Google login now uses Credential Manager instead of the deprecated Google Sign-In flow
- the app still exchanges the Google ID token with Firebase Auth
- sign-out clears both Firebase session state and Credential Manager session state

### Google Sheets authorization

- Google Sheets permission is separate from Firebase login
- setup can detect whether the blocker is:
  - no Google Sheets OAuth scope yet
  - spreadsheet sharing or access problem
- Sheets API calls now run on behalf of the signed-in Google user account, not a client-side service account
- authorization no longer forces a synthetic `Account(email, "com.google")` into `AuthorizationRequest`
- Play Services now decides whether it can reuse a saved account or needs to show a resolution flow
- Google Sheets and Drive capability checks now send a bearer token returned by `AuthorizationClient`, instead of using `GoogleAccountCredential`

## Important Decisions

### Modern auth APIs

The app now relies on:

- `CredentialManager` for Google login
- `AuthorizationClient` for Sheets authorization
- Firebase Auth current user as the active account identity source

### Auth tracing

When auth issues happen on-device, Logcat now has dedicated tags:

- `GoogleSignIn`
- `SheetsAuth`
- `AppRootAuth`

These logs capture:

- package name and web client ID suffix during sign-in start
- credential type returned by Credential Manager
- signed-in email used during Sheets authorization checks
- whether `AuthorizationClient` returned scopes directly or required a resolution flow
- whether an access token is present before the app builds the Sheets client
- full exceptions from the app side when sign-in or Sheets grant fails

### Runtime dependency alignment

The project is intentionally not on the newest Firebase BoM line yet.

Reason:

- newer Firebase auth artifacts require newer Kotlin metadata than the repo currently uses

Current aligned strategy:

- Firebase BoM `31.0.0`
- explicit `firebase-crashlytics:20.0.4`
- removed `firebase-ui-auth`
- removed Firestore after the spreadsheet access flow moved fully to Google Sheets

This keeps auth/runtime dependencies aligned without carrying unused Firebase database artifacts.

## Affected Flows

1. user signs in with Google
2. app authenticates with Firebase
3. spreadsheet setup checks Google Sheets authorization separately
4. setup can ask for Sheets grant only when needed
5. app sign-out clears both Firebase and Credential Manager state

## Verification

These commands passed across the auth/runtime updates:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Follow-Up Notes

- upgrade Kotlin/AGP/toolchain before moving to a newer Firebase BoM line
- consider an explicit revoke or disconnect flow for Google Sheets authorization
