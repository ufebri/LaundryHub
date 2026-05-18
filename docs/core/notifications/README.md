# Notifications Brief

## Goal

Push notification registration should reliably store the current device FCM token in the backend database after the app has selected the healthy API root.

## Current Behavior

- Android waits for startup backend resolution before registering the current FCM token.
- Token registration uses the active API root and posts to `notifications/token` relative to that root. If the configured root is `<BACKEND_URL>/api`, the final route is `<BACKEND_URL>/api/notifications/token`.
- `FirebaseMessagingService.onNewToken()` also registers refreshed tokens and asks the backend config provider to refresh first, so token rotation outside normal app startup still has a chance to use the latest configured API root.
- The backend trims incoming tokens, rejects blank values, and upserts by token.
- `device_tokens.token` is sized for longer FCM tokens. PostgreSQL deployments widen the column on startup if an older table already exists.

## Execution Checklist

- [x] Delay app-start token registration until `StartupConnectionUiState.Ready`.
- [x] Stop using raw `BuildConfig.BASE_URL` for notification registration.
- [x] Fix notification endpoint construction so `/api` is not duplicated.
- [x] Treat backend non-2xx registration responses as failures instead of logging false success.
- [x] Reject blank token payloads on the backend.
- [x] Add focused unit tests for client endpoint construction and backend token storage.
- [ ] Run a live device smoke after deploy and confirm the device row appears in `device_tokens`.
- [ ] Send one real FCM smoke notification after `GOOGLE_SERVICE_ACCOUNT_JSON` is confirmed in the backend environment.

## Verification

- `./gradlew :backend:test --tests com.raylabs.laundryhub.backend.db.repository.DeviceTokenRepositoryTest`
- `./gradlew :app:testDebugUnitTest --tests com.raylabs.laundryhub.core.fcm.DeviceTokenManagerTest`
- `./gradlew :backend:test`
- `./gradlew testDebugUnitTest`
- `./gradlew jacocoTestReport`
