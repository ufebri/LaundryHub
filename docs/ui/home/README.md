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

## Important Decisions

- deprecated status bar color mutation was removed instead of replaced
- layout insets are the preferred path for this screen style

## Verification

These commands passed after the cleanup:

```bash
./gradlew assembleDebug
```
