# Custom Folder Navigation — Technical Notes

## Project and scope

Original project: https://github.com/thunderbird/thunderbird-android

These notes describe an **unofficial modification** of Thunderbird for Android. They are separate from the upstream project's README so that future upstream updates are easier to incorporate.

## Status — October 29, 2026

### Prototype 1: Tested on an Android device

- The production call builds the folder tree with `maxDepth = Int.MAX_VALUE`. This removes the practical three-level display limit while retaining the existing `maxDepth` interface for compatibility with current tests.
- The existing `DisplayTreeFolder` model is retained. Only visible folders become independent rows in the existing `LazyColumn`.
- Indentation is currently 8 dp per level and capped at 48 dp. This cap applies to the presentation, not the logical tree depth.
- Subtle 1-dp vertical guides are drawn with 12% opacity along indentation steps. They were accepted after a visual check on the Android device. They are a visual aid, not connected tree branches.
- Expansion state is managed centrally using stable keys rather than being owned only by visible UI rows. Collapsing and reopening a parent preserves its expanded descendants during the current UI session. Persistence across app restarts is **not guaranteed**.
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

- Keep expand/collapse controls on the **right** for one-handed use. Try to align folder and account controls horizontally.
- Do not reserve width for empty or disabled counters. Unread counts appear when `unreadCount > 0`; starred counts are optional and disabled by default.
- The 8-dp indentation was difficult to distinguish on the tested device. **Subtle vertical guides are now part of Prototype 1**, following a successful visual evaluation. Further adjustments to indentation can be considered if needed.

The one-time helper `local/folder-tree/try-guides.py` was used to apply the experiment. It is not a runtime dependency and should not be run again on the already modified source file.

### Open folder: title-bar improvement (deferred)

Long folder paths should preserve recognizable beginnings **and** endings, e.g. `INBOX/.../parent/child`, instead of simply hiding the beginning. Future horizontal text navigation should work in the title bar as well.

### Further prototypes

1. **Compact folder UI:** Align the existing right-hand expand/collapse controls; do not combine them with folder icons at this stage.
2. **Manual horizontal text navigation:** A normal horizontal drag moves all displayed names; long-press and drag moves only one. Fast swipes jump to the beginning or end. Limit drag acceleration for unusually long paths.
3. **Automatic text movement:** After inactivity, move long names together and quickly to their respective ends, then hold position. Manual interaction always takes priority; movement speed should be configurable.

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

