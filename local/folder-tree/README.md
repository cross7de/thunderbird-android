# Custom Folder Navigation — Technical Notes

## Project and scope

Original project: https://github.com/thunderbird/thunderbird-android

These notes describe an **unofficial modification** of Thunderbird for Android. They are separate from the upstream project's README so that future upstream updates are easier to incorporate.

## Status — October 29, 2026

### Prototype 1: Tested on an Android device

- The production call builds the folder tree with `maxDepth = Int.MAX_VALUE`. This removes the practical three-level display limit while retaining the existing `maxDepth` interface for compatibility with current tests.
- The existing `DisplayTreeFolder` model is retained. Only visible folders become independent rows in the existing `LazyColumn`.
- Indentation is currently 4 dp per level and capped at 24 dp (6 levels). This cap applies to the presentation, not the logical tree depth.
- Subtle 1-dp vertical guides are drawn with 12% opacity along indentation steps. They were accepted after a visual check on the Android device. They are a visual aid, not connected tree branches.
- Expansion state is managed centrally using stable keys rather than being owned only by visible UI rows. Collapsing and reopening a parent preserves its expanded descendants during the current UI session. Prototype 3 adds persistent expansion states across app restarts.
- Existing folder actions, unread counts, optional starred counts, and right-aligned expand/collapse controls are retained in principle.
- The maintainer reported successful module tests, a working debug APK, and visible changes during an initial device test before the guide-line refinement. Full regression tests after adding the guides have not yet been reported. This is not a claim that the entire upstream test suite was executed.

### Main implementation files

- `feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/DrawerViewModel.kt`
- `feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/DrawerContract.kt`
- `feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/folder/FolderList.kt`
- `feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/folder/FolderListItem.kt`
- `feature/navigation/drawer/dropdown/src/main/kotlin/net/thunderbird/feature/navigation/drawer/dropdown/ui/folder/VisibleFolderRows.kt`

### Build and tests

Run from the repository root:

```bash
./gradlew :feature:navigation:drawer:dropdown:testDebugUnitTest
./gradlew :app-thunderbird:assembleFossDebug
```

The debug APK is normally generated at:

`app-thunderbird/build/outputs/apk/foss/debug/app-thunderbird-foss-debug.apk`

The Thunderbird debug app uses a different Android application ID from the normal release. Record the tested Git commit when reporting results.

### OAuth configuration (outstanding)

Prototype 1 did not change the original OAuth clients or provider-specific redirect URIs. Thunderbird's [upstream forking instructions](https://github.com/thunderbird/thunderbird-android#forking) require a fork to configure its own OAuth clients and redirect URIs:

- `app-thunderbird/src/{debug,daily,beta,release}/kotlin/net/thunderbird/android/auth/TbOAuthConfigurationFactory.kt`
- `app-k9mail/src/{debug,release}/kotlin/app/k9mail/auth/K9OAuthConfigurationFactory.kt`

For the current `fossDebug` APK, some redirects derive from `BuildConfig.APPLICATION_ID` and therefore already differ from the release app. However, provider client IDs are still upstream IDs. Microsoft uses a hard-coded `msauth://` redirect that must match the fork's package and signing certificate. No OAuth account-login test has been reported for this fork. Obtain provider registrations before replacing their IDs; do not invent values or commit secrets.

## Next steps

### Small UI refinements

- Retain the original right-hand expand/collapse controls and the original message-count placement.
- Do not reserve width for empty or disabled counters. Unread counts appear when `unreadCount > 0`; starred counts are optional and disabled by default.
- Keep the accepted 4-dp indentation (maximum 24 dp) and the subtle vertical guides unchanged.

The one-time helper `local/folder-tree/try-guides.py` was used to apply the experiment. It is not a runtime dependency and should not be run again on the already modified source file.

### Open folder: title-bar improvement (deferred)

Long folder paths should preserve recognizable beginnings **and** endings, e.g. `INBOX/.../parent/child`, instead of simply hiding the beginning. Future horizontal text navigation should work in the title bar as well.

### Prototype 2: Reverted

The right-alignment experiment was device-tested and subsequently reverted because unread counters shifted horizontally depending on whether a folder had children. The original folder-arrow and counter layout is retained.

### Prototype 3: Persistent expansion and hierarchical controls (device-tested)

- With `drawerExpandAllFolder` disabled (the default), newly encountered folders start collapsed.
- Individual open/closed states are saved locally and restored after restarting the application.
- Folder keys include the account identity and full path, so different mail accounts retain independent states.
- An expanded descendant remains remembered while its parent is collapsed.
- The global "expand all folders" user preference is respected if explicitly enabled.
- A short tap toggles only the selected folder.
- A second quick tap expands its immediate child folders, without explicitly changing deeper descendants.
- A long press expands or collapses the selected folder and all of its descendants to the same target state; ancestors and unrelated branches remain unchanged.
- Short-tap, double-tap, and long-press operations use the same persistent expansion-state storage.
- The 4-dp indentation and the 1-dp/12%-opacity guide styling remain unchanged. The vertical guide for an expanded folder follows all currently visible descendants and ends at the midpoint of the last visible descendant (not merely its last direct child).
- The maintainer confirmed these interactions, including state restoration after an app restart, on an Android device. A complete upstream regression suite has not been reported.

Manual navigation of unusually long individual folder names remains deferred. Global multi-row swiping was abandoned because it conflicts with the drawer's close gesture.

## Deferred alternatives

- Horizontal depth bars and small depth-position indicators.
- Dynamically recalculating indentation against the currently visible maximum depth.
- Numerically showing the absolute nesting level.

## Implementation considerations

- `Int.MAX_VALUE` for the production tree-building call is a pragmatic interim solution. A future upstream contribution could reconsider the obsolete `maxDepth` interface.
- Missing IMAP intermediate folders may be represented by placeholders. Their temporary negative IDs must not be used as persistent UI keys.
- The GitHub landing page is intentionally `.github/README.md`, **not** the original root `README.md`.
- For changes affecting Compose UI, run targeted module tests and build `assembleFossDebug`, then review behavior on a real device.
- Any upstream pull request must disclose AI-generated work and report test commands and remaining limitations as required by the upstream [AGENTS.md](https://github.com/thunderbird/thunderbird-android/blob/main/AGENTS.md).

## AI disclosure

**All custom implementation code, automated tests, helper scripts, and documentation for this modification were written by AI (OpenAI ChatGPT).** The human maintainer supplied requirements, design decisions, critical feedback, performed builds and device tests, and evaluated the results. The maintainer did not personally author the implementation code.

