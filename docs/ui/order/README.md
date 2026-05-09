# Order Flow Brief

## Current Behavior

- Add Order opens from the main app shell as an in-screen bottom sheet.
- The form collects package data, price, payment status, order date, due date, and optional WhatsApp follow-up without pre-allocating an order id on Android.
- On submit, Android sends the order payload to `POST /api/orders` with an empty order id. The backend assigns the next order id and returns it in the response.
- After submit succeeds, the app shows success feedback, dismisses the sheet, and starts the Home refresh in the background.

## Important Decisions

- Submit success should not wait for Home summary or unpaid-order refresh work.
- Write success and refresh failure stay separate. A submitted order should not feel failed just because a later Home refresh is slow.
- Android continues to go through the KMP backend. It should not read Supabase tables directly.
- Order id ownership belongs to the backend create path, not the Android form. This avoids stale local ids when multiple devices create orders around the same time.
- The backend keeps the current string id schema for Sheets compatibility, but serializes id allocation inside the database transaction with a Postgres advisory lock.

## Verification

- `./gradlew :app:testDebugUnitTest --tests com.raylabs.laundryhub.ui.order.OrderViewModelTest --no-daemon`
- `./gradlew :app:testDebugUnitTest --tests com.raylabs.laundryhub.core.data.repository.LaundryRepositoryImplTest --no-daemon`
- `./gradlew :backend:test --no-daemon`

## Follow-ups

- Rerun the Add Order macrobenchmark once the connected device is charged enough for AndroidX Benchmark.
- Longer term, consider a dedicated database sequence or numeric internal id if the legacy string order id no longer needs to stay Sheets-compatible.
