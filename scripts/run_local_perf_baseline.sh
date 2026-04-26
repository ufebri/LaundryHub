#!/bin/zsh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

export GRADLE_USER_HOME="$ROOT_DIR/.gradle"

./gradlew testDebugUnitTest \
  --tests com.raylabs.laundryhub.core.data.repository.GoogleSheetRepositoryImplPerformanceBaselineTest \
  --tests com.raylabs.laundryhub.ui.home.HomeViewModelPerformanceBaselineTest \
  --no-daemon

REPO_XML="app/build/test-results/testDebugUnitTest/TEST-com.raylabs.laundryhub.core.data.repository.GoogleSheetRepositoryImplPerformanceBaselineTest.xml"
HOME_XML="app/build/test-results/testDebugUnitTest/TEST-com.raylabs.laundryhub.ui.home.HomeViewModelPerformanceBaselineTest.xml"

repo_elapsed_ms="$(grep -o 'elapsed_ms=[0-9]*' "$REPO_XML" | head -n 1 | cut -d= -f2)"
home_refresh_elapsed_ms="$(grep 'method=refreshAllData' "$HOME_XML" | grep -o 'virtual_elapsed_ms=[0-9]*' | head -n 1 | cut -d= -f2)"
home_refresh_sequential_ms="$(grep 'method=refreshAllData' "$HOME_XML" | grep -o 'sequential_equivalent_ms=[0-9]*' | head -n 1 | cut -d= -f2)"
home_post_order_elapsed_ms="$(grep 'method=refreshAfterOrderChanged' "$HOME_XML" | grep -o 'virtual_elapsed_ms=[0-9]*' | head -n 1 | cut -d= -f2)"
home_post_order_sequential_ms="$(grep 'method=refreshAfterOrderChanged' "$HOME_XML" | grep -o 'sequential_equivalent_ms=[0-9]*' | head -n 1 | cut -d= -f2)"

captured_at="$(date '+%Y-%m-%d %H:%M %Z')"

echo
echo "LOCAL_PERF_BASELINE captured_at=$captured_at"
echo "LOCAL_PERF_BASELINE metric=repository_pending_order_5000_rows elapsed_ms=$repo_elapsed_ms"
echo "LOCAL_PERF_BASELINE metric=home_refresh_virtual elapsed_ms=$home_refresh_elapsed_ms sequential_equivalent_ms=$home_refresh_sequential_ms"
echo "LOCAL_PERF_BASELINE metric=home_post_order_refresh_virtual elapsed_ms=$home_post_order_elapsed_ms sequential_equivalent_ms=$home_post_order_sequential_ms"
