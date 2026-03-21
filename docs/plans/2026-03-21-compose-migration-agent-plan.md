# LocalMusic Compose-First Migration Control Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete LocalMusic's migration from a hybrid View/XML + Compose UI stack to an Android-official, Compose-first UI architecture without breaking media playback, Bluetooth scan, storage deletion, or notification behavior.

**Architecture:** Keep `MediaService`, `MediaBrowserCompat`, `MediaControllerCompat`, and the existing media/data layer stable during the migration. Move screen state ownership out of `Activity` fields and legacy MVP/View glue into screen-level `ViewModel`/state holders with unidirectional data flow, stateless composables, and lifecycle-aware state collection. Retain View/XML only where Android platform APIs still require it, such as `RemoteViews` notifications.

**Tech Stack:** AGP `9.1.0`, Kotlin `2.3.10`, Jetpack Compose BOM `2026.02.01`, Material3 `1.4.0`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`, MediaBrowserCompat/MediaControllerCompat, Activity Result APIs.

---

## Controller Notes

- This file supersedes the outdated "current state" description in `docs/upgrade/README.md`.
- Existing upgrade docs remain useful as historical phase records, but AgentA and AgentB must treat this file as the active source of truth.
- Finish line for this migration is **not** "zero XML files". Platform-mandated XML such as `RemoteViews` notification layouts may remain when Compose has no supported replacement.

## Current Audit As Of 2026-03-21

### Already on Compose

- `app/src/main/java/com/zy/ppmusic/mvp/view/MediaActivity.kt`
- `app/src/main/java/com/zy/ppmusic/mvp/view/BlScanActivity.kt`
- `app/src/main/java/com/zy/ppmusic/mvp/view/ErrorActivity.kt`
- `app/src/main/java/com/zy/ppmusic/widget/ChooseStyleDialog.kt`
- `app/src/main/java/com/zy/ppmusic/compose/MediaComposeScreen.kt`
- `app/src/main/java/com/zy/ppmusic/compose/BluetoothComposeScreen.kt`
- `app/src/main/java/com/zy/ppmusic/compose/theme/LocalMusicComposeTheme.kt`

### Still Hybrid / Still Blocking A True Compose-First Architecture

- `app/src/main/java/com/zy/ppmusic/mvp/view/MediaActivity.kt`
  Activity owns long-lived screen state via `mutableStateOf` and `mutableStateListOf`.
- `app/src/main/java/com/zy/ppmusic/mvp/view/BlScanActivity.kt`
  Activity owns Compose screen state instead of a screen-level state holder.
- `app/src/main/java/com/zy/ppmusic/compose/MediaComposeScreen.kt`
  Composable reads `DataProvider` directly, which breaks state hoisting and testability.
- `app/src/main/java/com/zy/ppmusic/mvp/base/AbstractBaseMvpActivity.java`
  Still inflates XML and installs View-era tint plumbing that Compose screens do not need.
- `app/src/main/res/layout/activity_compose_host.xml`
  XML shell only exists to satisfy the old base activity flow.
- `app/src/main/java/com/zy/ppmusic/widget/Loader.kt`
- `app/src/main/java/com/zy/ppmusic/widget/LoadingDialog.java`
- `app/src/main/res/layout/loading_layout.xml`
  Loading UI still depends on View/XML overlay infrastructure.
- `app/src/main/java/com/zy/ppmusic/widget/EasyTintView.java`
  In-app message delivery still uses a custom View implementation instead of Compose-first snackbar/state events.

### XML That May Legitimately Remain

- `app/src/main/res/layout/notify_copy_layout.xml`
  This is used by `RemoteViews` in `app/src/main/java/com/zy/ppmusic/utils/NotificationUtils.kt`.
  Keep it unless the app drops custom notification style or switches to a fully platform-supported non-custom style.

### Current Quality Gaps

- No Compose UI tests exist under `app/src/androidTest`.
- No screen-level `ViewModel` currently exposes `StateFlow` + `collectAsStateWithLifecycle`.
- UI models are not yet isolated from `DataProvider`, presenters, or Android framework objects.
- Deprecated `onActivityResult` is still present in the two main screens.

## Non-Negotiable Technical Rules

### Android Official / Compose Rules

1. State flows down, events flow up.
2. Long-lived screen state must live in a `ViewModel` or explicit screen state holder, not in `Activity` fields.
3. Screen composables collect `StateFlow` with `collectAsStateWithLifecycle()`.
4. Leaf composables stay stateless and do not reach into `DataProvider`, presenters, services, or `Activity`.
5. Use `remember` / `derivedStateOf` only for UI-local derivations, never as the source of truth for business state.
6. Use `AndroidView` / `ComposeView` interop only for components that cannot be removed in the current slice.
7. New regression coverage must prefer Compose UI tests and semantics-driven assertions.
8. Deprecated `startActivityForResult` / `onActivityResult` must be replaced with Activity Result APIs once the screen state holder is stable.

### Migration Boundary Rules

1. Do not rewrite `MediaService`, playback engine, database layer, or scan engine as part of the UI migration.
2. Do not mix permission-model modernization and large UI refactors in the same task unless the UI change cannot ship without it.
3. Do not delete a legacy View/XML asset until its Compose replacement is wired, verified, and reviewed.
4. Do not remove `notify_copy_layout.xml` while custom `RemoteViews` notifications are still supported.

## Agent Team Contract

### AgentA Responsibilities

- Execute one task at a time.
- Keep each change set reviewable and scoped.
- Run the required validation commands before handing off.
- Self-review for stale state, lifecycle leaks, and Compose anti-patterns.
- Never bypass this plan with opportunistic refactors.

### AgentB Responsibilities

- Review AgentA output in two passes:
  1. Spec compliance review against this plan.
  2. Senior Compose architecture review against Android official guidance.
- Block the next task if any blocker remains.
- Explicitly confirm whether XML/View interop that remains is justified.

### Controller Responsibilities

- Update this plan when scope changes.
- Decide which task AgentA executes next.
- Send AgentB only the relevant task scope and changed files.
- Keep a residual-risk list until final completion.

## Definition Of Done

- `MediaActivity` and `BlScanActivity` use screen-level `ViewModel`/state holders instead of `Activity`-owned Compose state.
- `MediaComposeScreen.kt` and `BluetoothComposeScreen.kt` are fed only UI models and callbacks.
- `AbstractBaseMvpActivity.java` and `activity_compose_host.xml` are no longer required by Compose screens.
- Legacy loading/message overlays are replaced or isolated behind Compose-first APIs.
- `viewBinding` is removed if no remaining production code needs it.
- In-app UI XML is removed except for platform-mandated resources such as notification `RemoteViews`.
- Compose UI regression coverage exists for at least the media screen and Bluetooth scan screen.
- A final residual list documents every remaining non-Compose UI asset and why it remains.

## Task 1: Media Screen State-Holder Extraction

**Files:**
- Create: `app/src/main/java/com/zy/ppmusic/ui/media/MediaScreenUiState.kt`
- Create: `app/src/main/java/com/zy/ppmusic/ui/media/MediaScreenViewModel.kt`
- Modify: `app/src/main/java/com/zy/ppmusic/mvp/view/MediaActivity.kt`
- Modify: `app/src/main/java/com/zy/ppmusic/compose/MediaComposeScreen.kt`

**Step 1: Define immutable UI models**

- Create immutable screen models for:
  - playback header state
  - queue preview items
  - artwork pager items
  - dialog sheet state
  - countdown state

**Step 2: Move screen state out of Activity fields**

- Replace `mutableStateOf`, `mutableIntStateOf`, and `mutableStateListOf` fields in `MediaActivity` with a `ViewModel` backed by `MutableStateFlow`.
- Keep presenter/service calls in the Activity for now, but route every UI mutation through `MediaScreenViewModel`.

**Step 3: Stop composables from reading global singletons**

- Remove direct `DataProvider.get()` calls from `MediaComposeScreen.kt`.
- Pass all artwork/queue/title/subtitle/progress data via parameters or a single `MediaScreenUiState`.

**Step 4: Collect state with lifecycle**

- In the screen entry point, use `collectAsStateWithLifecycle()` to render the state from `MediaScreenViewModel`.

**Step 5: Preserve behavior**

- Keep the existing `MediaBrowserCompat`, `MediaControllerCompat`, delete flow, countdown flow, and queue selection behavior unchanged.

**Run:**

- `.\gradlew.bat :app:assembleDebug`

**Manual verification:**

- Open media page
- Scan local media
- Play/pause
- Drag seek bar
- Open queue sheet
- Open queue detail
- Delete a queue item
- Start and stop countdown

**AgentB must reject if:**

- `MediaComposeScreen.kt` still reads `DataProvider` directly
- Screen state still lives primarily in `MediaActivity`
- Lifecycle-aware state collection is missing

## Task 2: Bluetooth Screen State-Holder Extraction

**Files:**
- Create: `app/src/main/java/com/zy/ppmusic/ui/bluetooth/BluetoothScreenUiState.kt`
- Create: `app/src/main/java/com/zy/ppmusic/ui/bluetooth/BluetoothScreenViewModel.kt`
- Modify: `app/src/main/java/com/zy/ppmusic/mvp/view/BlScanActivity.kt`
- Modify: `app/src/main/java/com/zy/ppmusic/compose/BluetoothComposeScreen.kt`

**Step 1: Define immutable Bluetooth UI models**

- Include toolbar title, enabled state, refresh state, section rows, device rows, and transient message events.

**Step 2: Move screen state out of Activity**

- Replace `scanDeviceList`, `uiItems`, and `screenState` as the UI source of truth with a `ViewModel` state flow.

**Step 3: Keep platform flows stable**

- Broadcast receivers, scan start/stop, bond/remove bond, and connect/disconnect logic stay in the Activity/presenter layer for this task.
- Activity only translates system callbacks into `ViewModel` state updates.

**Step 4: Prepare for Activity Result / permission modernization**

- Do not change permission APIs yet unless required for correctness.
- Isolate permission UI decisions so the later Activity Result migration is straightforward.

**Run:**

- `.\gradlew.bat :app:assembleDebug`

**Manual verification:**

- Toggle Bluetooth on/off
- Refresh scan list
- Pair/unpair a device
- Enter/leave the page during scanning

**AgentB must reject if:**

- Device list rendering still depends on mutable Activity collections
- Composables receive `BluetoothDevice` mutation side effects directly instead of stable UI models where avoidable

## Task 3: Remove XML Host Shell And View-Era Base Activity

**Files:**
- Create: `app/src/main/java/com/zy/ppmusic/ui/base/BaseComposePresenterActivity.kt`
- Modify: `app/src/main/java/com/zy/ppmusic/mvp/view/MediaActivity.kt`
- Modify: `app/src/main/java/com/zy/ppmusic/mvp/view/BlScanActivity.kt`
- Modify: `app/src/main/java/com/zy/ppmusic/mvp/base/AbstractBaseMvpActivity.java`
- Delete: `app/src/main/res/layout/activity_compose_host.xml`

**Step 1: Introduce a Compose-first base if a base class is still needed**

- If presenter lifecycle wiring is still shared, create a Compose-first base activity that does not inflate XML.
- If the base class is no longer justified, remove it rather than porting old View hooks.

**Step 2: Remove XML host dependency**

- Stop routing Compose screens through `getContentViewId()`.
- Ensure `setContent {}` is the only UI entry point for migrated screens.

**Step 3: Remove View tint plumbing from the Compose path**

- Theme color mutation for Compose must happen through state/theme models, not `LayoutInflaterFactory2` interception.

**Run:**

- `.\gradlew.bat :app:assembleDebug`

**AgentB must reject if:**

- Any Compose-only screen still requires `activity_compose_host.xml`
- The new base class still performs XML inflation or view interception for Compose screens

## Task 4: Replace Legacy Loading, Toast, And Dialog Interop

**Files:**
- Modify: `app/src/main/java/com/zy/ppmusic/mvp/view/MediaActivity.kt`
- Modify: `app/src/main/java/com/zy/ppmusic/mvp/view/BlScanActivity.kt`
- Modify: `app/src/main/java/com/zy/ppmusic/widget/ChooseStyleDialog.kt`
- Create: `app/src/main/java/com/zy/ppmusic/ui/common/UiMessage.kt`
- Create: `app/src/main/java/com/zy/ppmusic/ui/common/LoadingOverlay.kt`
- Delete: `app/src/main/java/com/zy/ppmusic/widget/Loader.kt`
- Delete: `app/src/main/java/com/zy/ppmusic/widget/LoadingDialog.java`
- Delete: `app/src/main/res/layout/loading_layout.xml`

**Step 1: Replace loading overlay**

- Use a Compose loading layer inside the screen scaffold instead of `WindowManager` or dialog-based loading.

**Step 2: Replace in-app message delivery**

- Route transient messages through snackbar or one-shot UI events consumed by Compose.
- Remove new usages of `EasyTintView` from migrated screens.

**Step 3: Simplify choose-style flow**

- Either keep `DialogFragment` temporarily with a hard reason, or move the dialog state fully into the media screen Compose tree.

**Run:**

- `.\gradlew.bat :app:assembleDebug`

**AgentB must reject if:**

- A new Compose screen still depends on `Loader`, `LoadingDialog`, or a `WindowManager` overlay
- Dialog state is duplicated between Fragment/Dialog and screen state holder

## Task 5: Activity Result API Modernization For Migrated Screens

**Files:**
- Modify: `app/src/main/java/com/zy/ppmusic/mvp/view/MediaActivity.kt`
- Modify: `app/src/main/java/com/zy/ppmusic/mvp/view/BlScanActivity.kt`

**Step 1: Replace deprecated result APIs**

- Replace `onActivityResult` flows with `registerForActivityResult`.

**Step 2: Keep behavior stable**

- Preserve document-tree authorization, Bluetooth enable flow, and app-settings return handling.

**Run:**

- `.\gradlew.bat :app:assembleDebug`

**Manual verification:**

- Document tree grant flow
- Bluetooth enable flow
- App settings return flow after permission denial

## Task 6: Compose Regression Coverage

**Files:**
- Create: `app/src/androidTest/java/com/zy/ppmusic/ui/media/MediaComposeScreenTest.kt`
- Create: `app/src/androidTest/java/com/zy/ppmusic/ui/bluetooth/BluetoothComposeScreenTest.kt`
- Modify: `app/build.gradle`

**Step 1: Add stable semantics/test tags only where needed**

- Add minimal, reviewable tags for queue, seek bar, playback button, Bluetooth toggle, and refresh action.

**Step 2: Add Compose UI tests**

- Cover rendering, interaction callbacks, and critical empty/non-empty states.

**Run:**

- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:testDebugUnitTest`

**Optional if device/emulator is available:**

- `.\gradlew.bat :app:connectedDebugAndroidTest`

## Task 7: Final Cleanup And Residual List

**Files:**
- Modify: `app/build.gradle`
- Modify: `docs/upgrade/README.md`
- Create: `docs/upgrade/compose-final-residuals.md`
- Delete: unused XML/View assets after proof of replacement

**Step 1: Remove dead build/config**

- Remove `viewBinding` if nothing in production uses it.
- Remove dead adapters, decorations, or view helpers that are no longer referenced.

**Step 2: Keep an explicit residual list**

- Document every remaining non-Compose UI asset and the reason it remains.
- Expected likely residual: `notify_copy_layout.xml` for `RemoteViews`.

**Step 3: Update team defaults**

- Mark Compose as the default for all new UI work.

**Run:**

- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:assembleRelease`

## Official Guidance Used For This Plan

- Compose migration strategy: [developer.android.com/develop/ui/compose/migrate/strategy](https://developer.android.com/develop/ui/compose/migrate/strategy)
- Compose interoperability APIs: [developer.android.com/develop/ui/compose/migrate/interoperability-apis](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis)
- State and state hoisting: [developer.android.com/develop/ui/compose/state](https://developer.android.com/develop/ui/compose/state)
- Architecture state holders: [developer.android.com/topic/architecture/ui-layer/stateholders](https://developer.android.com/topic/architecture/ui-layer/stateholders)
- Lifecycle-aware Compose collection: [developer.android.com/reference/kotlin/androidx/lifecycle/compose/package-summary](https://developer.android.com/reference/kotlin/androidx/lifecycle/compose/package-summary)
- Compose testing: [developer.android.com/develop/ui/compose/testing](https://developer.android.com/develop/ui/compose/testing)
- Compose performance guidance: [developer.android.com/develop/ui/compose/performance](https://developer.android.com/develop/ui/compose/performance)
