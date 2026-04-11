# Reminder UI

Status: active living brief
Last updated: 2026-04-11
Primary area: `ui/reminder`

## Goal

Add a lightweight reminder system that helps users cross-check overdue order updates without changing the spreadsheet schema.

## Current Behavior

### Product framing

- reminder is a cross-check feature, not a claim that an order is definitely still waiting for pickup
- `dueDate` is used only as a heuristic trigger to review orders that may need a status update
- reminder state stays local to the device through DataStore Preferences, not Room, and does not mutate the sheet by itself

### Placement

- `Home` shows a discovery card when reminder is still off and at least one eligible order exists
- `Home` also keeps a compact reminder summary visible after reminder is on, so users can still see the current review count and jump straight into the inbox
- `Profile` now exposes reminder as a single row inside `Settings`
- that row opens the dedicated reminder settings screen, which now owns:
  - `Reminder active`
  - `Daily notifications`
  - `Notification time`
  - `Send test notification`
  - `Open Reminder Inbox`
- `Reminder Inbox` is a dedicated route outside bottom navigation
- daily notifications deep-link into `Reminder Inbox`

### Inbox behavior

`Reminder Inbox` groups eligible orders into:

- `Due today`
- `Overdue 1-2 days`
- `Overdue 3-6 days`
- `Overdue 1 week`
- `Overdue 2 weeks`
- `Overdue 3 weeks`
- `Overdue 1 month or more`

Each reminder item can:

- mark as checked
- assume picked up
- snooze until tomorrow
- dismiss locally
- open the existing order edit flow in the shared bottom sheet
- action labels are intentionally short and more direct in the inbox UI:
  - `Reviewed`
  - `Likely picked up`
  - `Remind tomorrow`
  - `Dismiss`
  - `Open order`
- the inbox uses one compact hint near the overview card instead of a long explanation block, so the reminder list stays the visual focus

### Eligibility and layout safeguards

- transactions with a blank or whitespace-only `dueDate` are excluded from reminder discovery and inbox evaluation
- reminder discovery on `Home` only counts orders whose `dueDate` can be parsed into a valid reminder bucket
- reminder now accepts the same common app date variants used elsewhere in the product, including `dd/MM/yyyy`, `dd-MM-yyyy`, and `yyyy-MM-dd`
- reminder intro and inbox screens now respect the status bar area
- reminder intro and inbox now tint the status bar to match the top app bar surface color
- reminder settings content is scrollable so smaller devices can still reach the lower actions
- reminder inbox no longer flashes an empty/refresh-first state before the initial transaction fetch completes

### Notification behavior

- notifications stay off by default until the user enables them
- Android notification permission is requested only when the user explicitly enables daily notifications
- users can choose their own daily reminder time instead of being locked to one fixed schedule
- the daily notification is a single summary, not one notification per order
- reminder settings also provide a local `Send test notification` action so users can preview the notification on demand
- notification scheduling is restored on app start and after device reboot
- the boot restore receiver is now declared `android:exported="false"` because it only needs system delivery for `BOOT_COMPLETED`

### Coverage status

- reminder repository, use case wrappers, view models, and UI-state mapping now have dedicated unit coverage
- daily reminder time persistence and scheduler trigger calculation now have dedicated unit coverage
- reminder notification plumbing now also has dedicated Robolectric-style coverage for:
  - notification config/channel creation
  - notification publishing and cancel behavior
  - notification permission support
  - reminder sheet reader wiring
  - status-bar styling helper used by reminder screens
- JaCoCo now keeps `core/reminder/**` in the report and enables `includeNoLocationClasses` so Robolectric-backed reminder coverage is counted
- some reminder receiver and scheduler branches still sit below 100% in the local full-file report, but the package is no longer effectively uncovered

## Important Decisions

- no spreadsheet schema changes were introduced for reminder support
- `Home` is used for feature discovery instead of first-run onboarding
- `Profile` acts as the entry point, while the dedicated reminder settings screen is the actual control surface
- reminder resolution actions are stored locally so users can clear noisy items without mutating sheet data
- the boot-completed receiver is treated as system-only plumbing and should stay non-exported unless Android behavior proves otherwise on-device
- receiver logic now delegates into testable internal helpers so security hardening and notification branching can be covered without fighting Hilt wiring in every test

## Verification

These commands passed after the reminder feature integration:

```bash
./gradlew testDebugUnitTest --tests com.raylabs.laundryhub.core.reminder.ReminderBootReceiverTest --tests com.raylabs.laundryhub.core.reminder.ReminderAlarmReceiverTest --tests com.raylabs.laundryhub.core.reminder.ReminderNotificationConfigTest --tests com.raylabs.laundryhub.core.reminder.ReminderNotificationPublisherTest --tests com.raylabs.laundryhub.core.reminder.ReminderNotificationSheetReaderTest --tests com.raylabs.laundryhub.core.reminder.ReminderNotificationSupportTest --tests com.raylabs.laundryhub.ui.common.SystemBarStyleTest --tests com.raylabs.laundryhub.ui.common.util.DateUtilTest --tests com.raylabs.laundryhub.core.data.repository.ReminderRepositoryImplTest --tests com.raylabs.laundryhub.ui.order.OrderViewModelTest --tests com.raylabs.laundryhub.ui.reminder.ReminderIntroViewModelTest --tests com.raylabs.laundryhub.core.domain.usecase.reminder.ReminderUseCasesTest
./gradlew cleanTestDebugUnitTest testDebugUnitTest jacocoTestReport
```

## Follow-Up Notes

- if reminder state ever needs to sync across devices, the local-only DataStore model will need a product decision first
- if users later ask for multiple reminder windows in a day, extend the settings model instead of branching a second scheduler flow ad hoc
