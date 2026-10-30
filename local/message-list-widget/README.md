# Compact Message-List Widget — Technical Notes

## Scope and status (October 30, 2026)

This is an **unofficial Thunderbird for Android fork** of [thunderbird/thunderbird-android](https://github.com/thunderbird/thunderbird-android). The changes described here are implemented in the **classic message-list widget** (`feature/widget/message-list`), not the separate Glance widget. They are independent of the [folder-navigation prototypes](../folder-tree/README.md).

The source was developed on `proto4/compact-widget`, with the compact-widget implementation in `4690f80` and its row-height/settings extension in `7d1a948`. The maintainer tested the compact list and subsequent layout refinements on an Android device and accepted the current settings design. A full project regression suite and comprehensive cross-launcher testing have **not** been reported.

## Widget behavior

- **Default footprint:** 2 launcher columns × 1 row; resizable horizontally and vertically, subject to launcher constraints.
- **Message list:** Vertical scrolling through the unified inbox, sorted by date descending. The widget does not yet replicate every sorting option from the in-app Unified Inbox.
- **Rows:** A 3-dp account-color stripe; one line each for sender and subject; bold styling for unread messages; absolute time shown as `dd.MM.` and `HH:mm` on two lines (no relative dates). Sender/subject order follows Thunderbird's existing setting.
- **Row height:** 32 dp by default, adjustable from **32 to 64 dp in 1-dp increments**. This is a minimum/target achieved by adding vertical padding; large system fonts may require more space. A single saved value is shared by all classic message-list widget instances.
- **No preview text** in the compact rows. Tapping a message opens that specific message.

### Header actions

The header contains only icons, with equally distributed clickable areas and no text labels.

| Layout | Actions from left to right |
| --- | --- |
| 2×1 / narrow | Synchronize → Compose → Settings → Delete mode |
| Wider, at reported minimum width ≥ 210 dp | Unified Inbox → Synchronize → Compose → Settings → Delete mode |

The launcher reports available widget width through Android's `AppWidgetManager`; cell counts and thresholds may behave differently across launchers.

- **Unified Inbox:** Opens Thunderbird's unified inbox.
- **Synchronize:** Calls `MessagingControllerWrapper.checkMail()` for all accounts, performing actual mail synchronization rather than just refreshing the local widget.
- **Compose:** Opens Thunderbird's new-message screen.
- **Settings:** Opens the normal Thunderbird general settings directly at the widget subsection.
- **Delete mode:** Toggles a separate state for each widget instance. Only while this mode is active does each row show a small trash control in place of its timestamp.

The per-message trash action uses the message's account and folder reference to **move exactly that message to the account's Trash folder**. It refuses the action when no suitable trash destination is available or movement is not supported. There is no bulk delete, double-click action, or swipe-to-delete gesture.

### Row-height setting

Navigation in the app: **Settings → General settings → Display → Widget**. The header's settings icon opens the same subsection directly, while retaining the normal settings navigation/back stack.

The `SeekBar` supports every integer height from 32 through 64 dp and displays four reference marks (32, 40, 48, 64); the mark at 40 is emphasized. The marks are guides, **not snap positions**. The reset button restores 32 dp.

The setting is stored using the app's local `SharedPreferences` and applied to all widget instances when the user releases the slider or resets it. The widget provider then triggers a remote-list refresh. The implementation uses `RemoteViews.setViewPadding()` rather than newer APIs for dynamically changing view height.

## Implementation map

| Purpose | Location |
| --- | --- |
| Widget header/actions and sizing | `feature/widget/message-list/src/main/kotlin/app/k9mail/feature/widget/message/list/BaseMessageListWidgetProvider.kt` |
| Message rows and per-instance mode | `feature/widget/message-list/src/main/kotlin/app/k9mail/feature/widget/message/list/MessageListRemoteViewFactory.kt` |
| Safe single-message move and message opening | `feature/widget/message-list/src/main/kotlin/app/k9mail/feature/widget/message/list/MessageListWidgetActionActivity.kt` |
| Widget action constants and mode persistence | `feature/widget/message-list/src/main/kotlin/app/k9mail/feature/widget/message/list/MessageListWidgetActions.kt` |
| Header and row layout | `feature/widget/message-list/src/main/res/layout/message_list_widget_layout.xml`, `message_list_widget_list_item.xml` |
| Shared row-height setting and change broadcast | `legacy/core/src/main/java/com/fsck/k9/widget/WidgetRowHeightSettings.kt` |
| Slider, marks, and general-settings navigation | `legacy/ui/legacy/src/main/java/com/fsck/k9/ui/settings/general/` and `legacy/ui/legacy/src/main/res/xml/general_settings.xml` |

The original widget provider class name remains `net.thunderbird.android.widget.provider.MessageListWidgetProvider` for existing widget compatibility.

## Build and local verification

From the repository root:

```sh
./gradlew :app-thunderbird:assembleFossDebug
adb install -r app-thunderbird/build/outputs/apk/foss/debug/app-thunderbird-foss-debug.apk
```

A local release build can be attempted with `./gradlew :app-thunderbird:assembleFossRelease` if the appropriate signing configuration is available. Neither a reproducible release-signing setup nor a full automated test pass is asserted by these notes.

Recommended manual checks: widget placement at 2×1; each header action; widening to display the Unified Inbox icon at the left; opening a row; enabling/disabling delete mode without changing row height; moving one expendable message to Trash; testing 32, 40, 48, and 64 dp; checking persistence across app restarts and effects on multiple widget instances.

## Fork-specific considerations

- The Thunderbird app ID was changed to `de.cross7.thunderbird.android`, with `.debug` added for debug builds, to permit side-by-side installation with upstream Thunderbird.
- The existing upstream OAuth client registration/redirect setup remains **unadapted and unverified** for the fork. See the [fork landing page](../../.github/README.md) and the upstream [forking instructions](https://github.com/thunderbird/thunderbird-android#forking) before distributing a build.
- The UI relies on Android `RemoteViews`, whose supported view classes and click behavior are restricted; do not introduce unsupported elements such as `android.widget.Space` into widget layouts.
- The 2×1 header has four touch regions: launcher geometry can make these comparatively narrow. Further testing with accessibility settings, large fonts, and different launchers is advisable.
- Changes to the classic widget are not automatically mirrored into the Glance widget.

## AI disclosure

**All custom implementation code and documentation for this widget were written with AI assistance (OpenAI ChatGPT).** The human maintainer specified and refined behavior, performed local builds and device testing, and reviewed the outcomes. This is an unofficial experiment, not an official Thunderbird release.
