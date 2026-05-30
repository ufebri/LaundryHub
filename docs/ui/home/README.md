# Home UI Brief

## Goal

Home should show the fastest useful business snapshot without making the user inspect reporting sheets manually. The Gross Income card is a monthly reporting card, not the all-time `Total Income` summary value.

## Current Behavior

- Gross Income is sourced from `GET /api/gross`, which is Sheet-owned when the backend has `SPREADSHEET_ID` and service-account access.
- The Home card selects the current month row when it exists. If the current month is missing, it falls back to the latest parseable gross month, then the last nonblank row.
- The gross detail screen uses the same backend endpoint and expects rows newest month first.
- Order count text is normalized for display, so both `115` and `115 order` render as `115 order` on the Home card.

## Verification

- `./gradlew :shared:jvmTest --no-daemon`
- `./gradlew :backend:test --tests com.raylabs.laundryhub.backend.routes.GrossRouteBehaviorTest --no-daemon`
- `./gradlew :app:testDebugUnitTest --tests com.raylabs.laundryhub.ui.home.HomeViewModelTest --tests com.raylabs.laundryhub.ui.home.state.GrossItemTest --no-daemon`
- `./gradlew :app:compileDebugAndroidTestKotlin --no-daemon`
- `./gradlew testDebugUnitTest --no-daemon`
- `./gradlew :backend:test --no-daemon`
- `git diff --check`
