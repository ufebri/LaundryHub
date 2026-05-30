# Startup Connection Brief

## Goal

LaundryHub now checks the backend connection once during app startup before it routes users to onboarding or the main app. The check exists so production API URL changes can be controlled from Firebase Remote Config without shipping a new APK.

## Current Behavior

- App startup begins in a quiet checking state.
- If Remote Config resolves a usable backend URL and `/api/health` responds successfully, the user continues into the normal auth route.
- Startup checks backend candidates in this order: `api_base_url`, each comma/newline-separated `api_fallback_base_urls` entry, then the build-time fallback URL.
- If Remote Config sets maintenance on, the startup screen shows a calm service-unavailable state with the configured message when available.
- If every candidate backend URL fails the health check, the same service-unavailable state appears.
- The screen includes a **Check again** action. Manual retry forces a fresh Remote Config fetch before checking health again.
- There is no Profile settings entry in this pass. Manual sync exists only on the startup failure surface.

## UX Decisions

- The normal healthy path must not show maintenance wording.
- The checking state uses neutral copy and should feel like app preparation, not an outage.
- Failure copy is intentionally short: it tells users their data is safe and gives one clear action.
- The retry button is disabled while retrying, with progress feedback in place.
- The startup surface uses the existing LaundryHub theme, brand image, rounded surfaces, and shared strings.

## Affected Flow

`MainActivity` still installs the Android splash screen first. After Compose starts, `AppRoot` observes `StartupConnectionViewModel`. Only `Ready` allows onboarding or the main shell to render. `Checking`, `Retrying`, `Maintenance`, and `Unavailable` render `StartupConnectionScreen`.

## Verification

- `./gradlew :app:testDebugUnitTest --tests com.raylabs.laundryhub.core.data.config.RemoteBackendConfigProviderTest --tests com.raylabs.laundryhub.ui.startup.StartupConnectionViewModelTest --tests com.raylabs.laundryhub.core.data.repository.LaundryRepositoryImplTest --no-daemon`
- `./gradlew :app:testDebugUnitTest :backend:test --no-daemon`
