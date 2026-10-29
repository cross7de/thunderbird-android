# Thunderbird for Android — Folder Navigation Enhancements

> **Original project:** [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android), maintained by the Thunderbird project.
>
> **Unofficial modification.** This repository contains experimental changes to the Android folder navigation. It is not an official Thunderbird release.

## Purpose

The goal is to make deeply nested IMAP folders navigable without sacrificing the limited screen space available for folder names.

## Current status

**Prototype 1: Deep folder hierarchy**

- Folder trees can be expanded beyond the original three-level display limit.
- Visible folders are rendered as individual entries in the existing lazy list.
- Indentation is compact and capped without limiting the logical nesting depth.
- Subtle vertical guides help distinguish nesting levels without widening the rows.
- Expansion states are preserved when parent folders are collapsed and expanded again within the current UI session.

The maintainer reports successful module tests and a debug APK build for the initial folder-tree implementation. The visual guides were subsequently accepted following an on-device evaluation; full regression testing after this refinement is not yet documented. This is experimental work, not a fully validated release.

## OAuth configuration — not yet adapted

This fork still contains Thunderbird's original OAuth client IDs and provider-specific redirect settings. OAuth sign-in (including Google and Microsoft) has **not** been validated for this fork and may fail or conflict with an installed upstream app. Before distributing an independently usable build, register and configure appropriate OAuth clients and redirect URIs for the fork. Standard IMAP/SMTP connections without OAuth are separate from this limitation.

## Planned improvements

- Keep the expand/collapse controls on the right and align folder and account controls where practical.
- Support horizontal navigation of long folder names, including direct access to their ends.
- Optionally animate long names after a period of inactivity, with manual input always taking priority.
- Eventually improve long folder titles, for example `INBOX/.../parent/child`.

These improvements are **not yet implemented** unless stated otherwise in the [technical notes](../local/folder-tree/README.md).

## AI disclosure

**All custom implementation code, automated tests, helper scripts, and documentation for these modifications were written by AI (OpenAI ChatGPT).** The human maintainer supplied requirements, design decisions, critical feedback, performed builds and device tests, and evaluated the results. The maintainer did not personally author the implementation code.

## Documentation

- [Implementation status, tests, and design notes](../local/folder-tree/README.md)
- [Original project's README](../README.md)

The original upstream README and source history are retained. Fork-specific documentation lives separately to minimize conflicts when incorporating upstream changes.

