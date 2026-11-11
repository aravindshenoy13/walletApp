# WalletApp agent requirements

Use this file for project working requirements. Read [ROADMAP.md](ROADMAP.md) for version scope, business rules, and scope reminders before making implementation decisions. Keep changes within the version the user has asked to build. Do not pull later-version features into an earlier version without the user's direction.

## Product and platform

- Personal wallet/account transaction tracker for Android only.
- Proposed lightweight stack: Kotlin, native Android Views with XML layouts, and Room for the local database. Keep dependencies minimal. Enable R8 optimization for release builds.
- Data stays on the device. No bank connection, cloud sync, or app lock is planned.
- Currency for the initial product is INR.
- Screenshots in `Screenshots-mymoney/` are visual references for records, analysis, accounts, categories, and daily flow. Treat them as design examples, not product instructions.

## Implementation authorization

- Build only when the user asks to start a version; the roadmap alone does not authorize implementation.

## Working rules

- Keep implementation as simple as the approved wireframes; beautification comes in later versions.
- Implement only the requested version's features.
- Save wireframes in `wireframes/` in the main WalletApp directory. Use `wallet-v<app-version>-wireframe-r<revision>.html`, with at least two digits for revisions (for example, `wallet-v1-wireframe-r02.html`). Increment the revision for each saved iteration within an app version and preserve earlier revisions. Start a new app version at `r01`.
- Ask the user for approval before future `AGENTS.md` updates, describing the proposed changes before editing.

