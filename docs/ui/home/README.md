# Home UI

Status: active living brief
Last updated: 2026-04-04
Primary area: `ui/home`

## Goal

Keep Home-related screens aligned with current edge-to-edge behavior and avoid deprecated system bar APIs.

## Current Behavior

`GrossDetailScreenView` no longer changes `window.statusBarColor`.

The screen now:

- relies on activity-level edge-to-edge setup
- keeps using `statusBarsPadding()` for layout safety
- keeps using `WindowInsetsControllerCompat` only for status bar icon appearance
- keeps Home header imagery stable during ordinary recomposition by using a deterministic daily image seed instead of a random URL on every render
- renders pending order rows as keyed lazy items instead of one large manual block, so list updates feel more local
- keeps stale section content visible while refresh loading is in progress and shows a lighter inline loading indicator instead of replacing the whole section immediately

## Important Decisions

- deprecated status bar color mutation was removed instead of replaced
- layout insets are the preferred path for this screen style
- home background imagery should feel fresh but stable, so the current strategy uses `uid/email + today` as a daily seed
- loading new Home data should update the screen without making the whole section feel rebuilt from scratch

## Verification

These commands passed after the cleanup:

```bash
./gradlew assembleDebug
```
