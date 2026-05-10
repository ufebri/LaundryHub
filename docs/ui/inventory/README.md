# Inventory Flow Brief

## Goal

Inventory package CRUD should use backend-owned data instead of Sheets row indexes, resolve every loading state into content, empty, or error, and keep the UI responsive after successful writes.

## Current Behavior

- Package rows are loaded from the backend and mapped with backend id, name, price, duration, unit, and legacy sheet row data when available.
- Other package suggestions treat `Resource.Empty` as a successful empty state, so the screen no longer stays stuck in loading when there are no suggestions.
- Add, update, and delete show success feedback after the backend write succeeds, update local UI immediately, and then refresh silently.
- Update sends the original package name in the route and the edited package body, which allows package rename flows without relying on `sheetRowIndex`.
- The package editor now treats `originalName` as the edit-mode marker. Backend packages with `sheetRowIndex=-1` still open as update forms instead of falling back to add-package behavior.
- Delete calls `DELETE /api/packages/{name}` and updates the `deletePackage` state instead of reusing save state.

## Important Decisions

- Package name is the current stable external identifier for update/delete. If the product later needs duplicate package names, add a dedicated immutable package id contract before loosening this rule.
- `sheetRowIndex` remains legacy metadata for sync/import context, not the Android mutation target.
- Local UI updates use the best available match order: backend id, sheet row index when valid, then case-insensitive package name.
- Backend Sheets sync for packages goes through the batch sync service when spreadsheet config is enabled.

## Verification

- `./gradlew :app:testDebugUnitTest :backend:test --no-daemon`
- `./gradlew :shared:jvmTest --no-daemon`
- `./gradlew assembleRelease --no-daemon`
- `./gradlew :app:connectedDebugAndroidTest --no-daemon`
- Deployed backend package API smoke passed: create package, update package by name, delete package by name, and verify the deleted test package no longer appears in the package list.
- Package delete returned `sheetSynced=true`, confirming the deployed backend sync configuration is active for package delete cleanup.
- Focused guarded inventory UI E2E compiled and launched. A signed-in attempt exposed the edit-mode bug fixed here; later focused reruns skipped because the debug app started at onboarding after reinstall.

## Follow-ups

- Rerun guarded mutating Inventory E2E after signing the device back into the app.
- If package names ever become user-editable to non-unique values, migrate update/delete routes to an immutable package id before shipping that product change.
