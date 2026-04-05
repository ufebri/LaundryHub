# Ads

Primary area: `core/ads`, `ui/component`, selected scroll screens in `ui/home`, `ui/history`, `ui/outcome`, `ui/profile`

## Goal

Add a reusable AdMob banner integration that is safe for the current app shell and easy to swap to real ad unit IDs later.

## Current behavior

- AdMob keys are read from `config.properties`
- AdMob keys are separated by build type:
  - `ADMOB_APP_ID_DEBUG`
  - `ADMOB_BANNER_AD_UNIT_ID_DEBUG`
  - `ADMOB_APP_ID_RELEASE`
  - `ADMOB_BANNER_AD_UNIT_ID_RELEASE`
- `AndroidManifest.xml` now declares `com.google.android.gms.ads.APPLICATION_ID`
- `LaundryHubApp` initializes Google Mobile Ads SDK once at app launch
- `InlineAdaptiveBannerAd` is the single reusable Compose banner component
- `rememberInlineAdaptiveBannerAdState` should be created at screen level when a banner is rendered inside lazy scroll content
- main bottom-tab screens hoist their banner state in `LaundryHubStarter`, so tab switching does not create a brand-new banner state for every revisit
- previews do not try to load real ads; they render a lightweight placeholder card instead
- banner sizing uses a more compact adaptive height to avoid oversized ad blocks on phones
- the SDK is pinned to `play-services-ads:23.6.0` because the current project still uses Kotlin `1.9.23`, while newer Google Mobile Ads SDK lines pull Kotlin `2.1.0` metadata
- Gradle no longer contains embedded AdMob IDs or sample fallbacks; it only reads the values from `config.properties`
- if the AdMob values are still placeholders or blank, the app skips SDK initialization and does not attempt to render banners

## Placement decisions

The app keeps banners inline with content on supported screens.

Reason:

- users prefer the ad to feel like part of the screen content instead of sitting above bottom navigation
- the real issue was not inline placement itself, but banner state being tied too closely to lazy item composition
- banner visibility is now more stable because inline placements hoist banner state at screen level before rendering inside `LazyColumn`
- `No fill` responses no longer kill a banner state forever; the current implementation allows a timed retry on the same state after a cooldown
- banners are still kept away from onboarding, setup, and order-entry surfaces

## Placement map

- `Onboarding`: no ads
- `Spreadsheet Setup`: no ads
- `Order bottom sheet / order form`: no ads
- `Home`: inline banner between Today Activity and Pending Orders
- `History`: inline banner near the top of the history list
- `Outcome`: inline banner near the top of the outcome list
- `Profile`: inline banner between Spreadsheet and Settings
- `Inventory`: inline banner between package sections
- `Gross Detail`: inline banner near the top of the detail list

## Important decisions

- placeholders stay safe by using official Google sample IDs until real AdMob IDs are generated
- screens remain compatible with ads, but focused flows stay clean
- all placements reuse one component so future placement or style changes do not require duplicated ad code
- when a banner lives inside lazy content, its ad state should be remembered at screen level so the ad can reattach after the user scrolls away and back
- banner debugging currently uses Logcat tags `InlineAdaptiveBanner` and `BottomNavDebug`

## Future screen checklist

Every new screen should be reviewed before adding ads. Use this rule of thumb:

- show ads only on content-heavy or maintenance screens
- if the ad belongs inside `LazyColumn` content, hoist the banner state outside the list and only render the view from inside the item slot
- avoid screens that are focused on login, setup, form completion, editing, or other high-attention tasks
- avoid placements too close to bottom navigation, FABs, submit buttons, destructive actions, or other frequent taps
- if a new screen is intentionally ad-free, keep that decision documented when it changes monetization scope or screen behavior

## Replace placeholders later

Before release, replace the four placeholder values in `config.properties` with the real IDs from AdMob:

- `ADMOB_APP_ID_DEBUG`
- `ADMOB_BANNER_AD_UNIT_ID_DEBUG`
- `ADMOB_APP_ID_RELEASE`
- `ADMOB_BANNER_AD_UNIT_ID_RELEASE`

## Verification

- `./gradlew testDebugUnitTest --no-daemon`
- `./gradlew assembleDebug --no-daemon`
