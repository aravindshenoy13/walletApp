# Wallet V4 implementation plan — draft

Planning reference: [ROADMAP.md](ROADMAP.md) and [V4 wireframe revision 02](wireframes/wallet-v4-wireframe-r02.html). The requested behavior below takes precedence over illustrative wireframe text. This is a plan, not authorization to start implementation.

## Scope

### Settings, search, and navigation

- Place a Settings icon at the far left of the Wallet header, immediately before and vertically aligned with the title. Settings provides dark mode, CSV export, full backup, and full restore.
- Keep Records search compact at the top right until tapped; expand it for text entry. Preserve the existing live description/payee and amount filtering, clear action, and today's summary behavior.
- Keep bottom-navigation icons and add simple line-art icons throughout the app. Use black line art in light mode and white in dark mode. Prefer native vector assets and minimal dependencies.

### Dark mode and selection controls

- Add a persistent dark-mode control in Settings and apply the theme across screens, dialogs, charts, selection controls, and icons. Preserve the existing simple visual style and make text and controls readable in both themes.
- Replace radio-button appearances throughout the app with square checkbox styling. Preserve the underlying selection semantics and accessibility: each single-choice group permits one selection; CSV account export permits multiple selections.

### Category icons

- Add an **Additional Icons** button to the category editor. It opens a separate screen containing a borderless row-and-column grid of additional line-art icons.
- Preserve independent icon and background-color selection, including the existing 16 colors. Existing categories keep their icon/color choices. The wireframe's additional icon names are examples for the initial inventory.

### CSV export

- Expose CSV export through Settings with multiple-account selection and existing transaction-type choices.
- Include a transfer once when either endpoint is selected, preserving both account references.
- Keep existing export columns and spreadsheet-safety behavior.
- CSV import is removed from product scope by user direction on 12 November 2026.

### Full backup and restore

- Provide a full backup distinct from transaction CSV export. Include accounts and opening balances, categories (including archived state, icon and color), and transactions and their relationships. Preserve persisted data needed for a faithful restore.
- Use a versioned backup representation and the Android file picker. Validate a backup before changing wallet data and perform accepted restore changes atomically.
- Allow restore only into an empty wallet: no accounts or transactions, with only untouched built-in categories. A fresh install with those default categories counts as empty. Do not merge with or replace a wallet containing user data.

## Implementation sequence

1. Start from the accepted V3 baseline and wire the Settings entry, compact/expanded search, and navigation changes.
2. Add the persistent theme and app-wide line-art icons, then apply square selection styling while preserving single- and multi-select behavior.
3. Add the Additional Icons screen and connect its selection to the existing category editor and 16-color palette.
4. Extend CSV export with multi-account selection, including a transfer once if either endpoint is selected.
5. Implement full-backup creation and empty-wallet-only restore.
6. Verify V3 regressions and the V4 flows, including theme persistence, search, single/multiple selections, category icon/color independence, selected-account exports, and data round trips. Exercise error and cancellation paths for restore.

## Implementation checkpoint — 12 November 2026

Implementation started from V3 after verifying all six archived checksums and the complete Git history bundle in `repository/archives/wallet-v3-2026-11-12-r01/`. The archived source, history, debug APK, and unsigned release APK remain untouched.

Implemented Settings, persistent light/dark selection, compact/expanded live Records search, native square radio styling, navigation/action/account/type vectors, seven additional category icons on a separate borderless grid, and selected-account CSV export. Transfers match either selected endpoint once. Existing export columns and spreadsheet-safety behavior remain unchanged.

Full backup uses versioned INR JSON with exact integer strings, account opening balances, category metadata/archived state, transaction IDs and relationships, and preserved legacy note data. Restore validates the complete document before writing and rechecks the untouched-empty-wallet condition inside one Room transaction. Backup selection uses Android document pickers; files over 20 MB are rejected on restore. Theme preference stays device-local and is separate from ledger backup.

CSV import has been removed from Settings and product scope. Full-backup restore remains supported. Final V4 acceptance remains separate from this scope change.

Build version is `4.0-dev` (code 4), database schema remains 2. No new runtime dependencies were added.

### Checkpoint verification

- 23 JVM tests passed, including selected-account export and either-endpoint transfer inclusion.
- 32 API 37 instrumentation tests passed. These include backup round trips, invalid reference/version rejection, nonempty-wallet protection, compact search/Settings/theme controls, additional icon selection across recreation, and V1–V3 regressions.
- Lint: zero errors, 44 warnings. Debug and optimized unsigned release builds passed.
- Direct instrumentation result: `repository/.tools/v4-instrumentation-final.txt`. The first attempt encountered an emulator System UI ANR; the successful run followed clearing that dialog and stopping build daemons.
- A final visual check found the launcher wallet asset unsuitable for a tinted navigation icon; it was replaced with a dedicated line-art account vector. This resource-only correction was rebuilt and checked with lint.
- Development APKs and checksums are under `repository/dist/wallet-v4-dev-*`. Release APK requires signing. Final V4 acceptance remains outstanding.
