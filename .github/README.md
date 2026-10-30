# Thunderbird for Android — Folder Navigation and Compact Message Widget

> **Original project:** [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android), maintained by the Thunderbird project.
>
> **Unofficial modification.** This repository contains experimental changes to folder navigation and the classic Android message-list widget. It is not an official Thunderbird release.

## Purpose

The folder-navigation changes make deeply nested IMAP folders navigable without sacrificing the limited screen space available for folder names.

The compact message-list widget brings readable messages and frequently used actions to a small home-screen widget, with configurable row height.

## Current status

**Prototype 1: Deep folder hierarchy**

- Folder trees can be expanded beyond the original three-level display limit.
- Visible folders are rendered as individual entries in the existing lazy list.
- Indentation is compact and capped without limiting the logical nesting depth.
- Subtle vertical guides help distinguish nesting levels without widening the rows.
- Expansion states are preserved when parent folders are collapsed and expanded again within the current UI session.

The maintainer reports successful module tests and a debug APK build for the initial folder-tree implementation. The visual guides were subsequently accepted following an on-device evaluation; full regression testing after this refinement is not yet documented. This is experimental work, not a fully validated release.

**Prototype 3: Persistent folder expansion and tree navigation (device-tested)**

- Individual open/closed states survive app restarts, independently for each mail account.
- A single tap toggles one folder. A rapid second tap also expands its direct children.
- A long press opens or closes the selected folder and all its descendants without changing other branches.
- Subtle hierarchy guides follow visible descendant rows and finish at the midpoint of the last visible descendant.
- Existing arrow and message-counter placement, 4-dp indentation, and the 1-dp/12%-opacity guide style are retained.

These behaviors were confirmed on an Android device. The changes remain experimental and are not an official Thunderbird release.

**Prototype 4: Compact, resizable message-list widget**

- Targets a 2-column × 1-row home-screen widget, with vertical message scrolling and launcher-dependent horizontal/vertical resizing.
- Shows sender, subject, absolute date/time (day/month and hour/minute), unread emphasis, and a narrow account-color stripe. Sender/subject order follows the application's preference.
- Has four icon-only header actions even at 2×1: synchronize all accounts, compose, widget settings, and toggle per-message delete mode. Wider layouts additionally show Unified Inbox at the far left; delete mode stays at the far right.
- Delete mode replaces each message's timestamp with an individual trash button. It moves one message to its account's Trash folder when possible, without batch deletion.
- The row height is configured at **Settings → General settings → Display → Widget**, accessible directly from the header. A 32–64 dp slider (1-dp increments, reference markers, and reset to 32 dp) applies to all instances; delete-mode state is per widget.
- The compact row layout and interactions were iteratively tested on an Android device. A complete regression-test run has not been documented, and launcher behavior may vary.

See the [widget technical notes](../local/message-list-widget/README.md) for implementation details, limitations, and build instructions.

## OAuth configuration — not yet adapted

This fork still contains Thunderbird's original OAuth client IDs and provider-specific redirect settings. OAuth sign-in (including Google and Microsoft) has **not** been validated for this fork and may fail or conflict with an installed upstream app. Before distributing an independently usable build, register and configure appropriate OAuth clients and redirect URIs for the fork. Standard IMAP/SMTP connections without OAuth are separate from this limitation.

## Planned improvements

**Folder navigation**

- Let users inspect unusually long individual folder names without interfering with the drawer closing gesture.
- Eventually improve long folder titles, for example `INBOX/.../parent/child`.

The original expand/collapse control layout is retained. An earlier arrow-alignment experiment was reverted because it made unread counters jump between positions.

These folder improvements are **not yet implemented** unless stated otherwise in the [technical notes](../local/folder-tree/README.md).

**Message-list widget**

- Evaluate icon touch targets, resizing, and row-height settings on additional launchers and display/font sizes.
- Consider further UI refinements separately; no double-tap shortcut or bulk-delete action is implemented.

## Fork application ID

This fork uses `de.cross7.thunderbird.android` as the Thunderbird application ID (debug builds add `.debug`). It can therefore coexist with the upstream Thunderbird app. This change does not provide separate OAuth client registrations or establish release-signing compatibility.

## AI disclosure

**All custom implementation code, automated tests, helper scripts, and documentation for these modifications were written by AI (OpenAI ChatGPT).** The human maintainer supplied requirements, design decisions, critical feedback, performed builds and device tests, and evaluated the results. The maintainer did not personally author the implementation code.

## Documentation

- [Folder-navigation implementation status, tests, and design notes](../local/folder-tree/README.md)
- [Compact message-list widget: implementation and usage](../local/message-list-widget/README.md)
- [Original project's README](../README.md)

The original upstream README and source history are retained. Fork-specific documentation lives separately to minimize conflicts when incorporating upstream changes.

