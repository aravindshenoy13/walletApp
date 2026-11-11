# Wallet V4 implementation plan — draft

Planning reference: [ROADMAP.md](ROADMAP.md) and [V4 wireframe revision 02](wireframes/wallet-v4-wireframe-r02.html). The requested behavior below takes precedence over illustrative wireframe text. This is a plan, not authorization to start implementation.

## Scope

### Settings, search, and navigation

- Place a Settings icon at the far left of the Wallet header, immediately before and vertically aligned with the title. Settings provides dark mode, CSV export, CSV import, full backup, and full restore.
- Keep Records search compact at the top right until tapped; expand it for text entry. Preserve the existing live description/payee and amount filtering, clear action, and today's summary behavior.
- Keep bottom-navigation icons and add simple line-art icons throughout the app. Use black line art in light mode and white in dark mode. Prefer native vector assets and minimal dependencies.

### Dark mode and selection controls

- Add a persistent dark-mode control in Settings and apply the theme across screens, dialogs, charts, selection controls, and icons. Preserve the existing simple visual style and make text and controls readable in both themes.
- Replace radio-button appearances throughout the app with square checkbox styling. Preserve the underlying selection semantics and accessibility: each single-choice group permits one selection; CSV account export permits multiple selections.

### Category icons

- Add an **Additional Icons** button to the category editor. It opens a separate screen containing a borderless row-and-column grid of additional line-art icons.
- Preserve independent icon and background-color selection, including the existing 16 colors. Existing categories keep their icon/color choices. The wireframe's additional icon names are examples for the initial inventory.

### CSV export and import

- Move or expose CSV export and import through Settings. Extend the existing export flow with multiple-account selection while retaining its transaction-type choices.
- Include a transfer once when either its source or destination account is selected, preserving both account references in the exported row.
- Support importing both Wallet exports and bank CSVs with column mapping. When imported account or category names do not match existing wallet entries, ask the user to map them to existing entries rather than creating entries automatically.
- Reject a bank CSV that has no category column in V4. Income and expense entries must map to existing categories; transfers do not require a category.
- Reject the entire import if any row is invalid or identified as a duplicate; do not import a partial file. Give a clear rejection reason. Detailed row-error previews and selecting valid rows for partial import are deferred to V5.
- Treat transactions as duplicates when date, type, amount, and accounts match. For transfers, compare source and destination in their respective roles. Check against the wallet and within the imported file; time, category, description, and local record ID do not distinguish a match.
- This rule can flag two genuine transactions as duplicates when those fields match; V4 rejects the file rather than offering a row-level override.
- Confirm the remaining bank-column mapping details before implementing import.
- The existing export columns are `record_id,date,time,type,amount,currency,account,to_account,category,description`. Its record IDs are local to a wallet, and some text is prefixed for spreadsheet safety; a lossless import/decoding contract must be settled rather than assumed.

### Full backup and restore

- Provide a full backup distinct from transaction CSV export. Include accounts and opening balances, categories (including archived state, icon and color), and transactions and their relationships. Preserve persisted data needed for a faithful restore.
- Use a versioned backup representation and the Android file picker. Validate a backup before changing wallet data and perform accepted restore changes atomically.
- Allow restore only into an empty wallet: no accounts or transactions, with only untouched built-in categories. A fresh install with those default categories counts as empty. Do not merge with or replace a wallet containing user data.

## Implementation sequence

1. Start from the accepted V3 baseline and wire the Settings entry, compact/expanded search, and navigation changes.
2. Add the persistent theme and app-wide line-art icons, then apply square selection styling while preserving single- and multi-select behavior.
3. Add the Additional Icons screen and connect its selection to the existing category editor and 16-color palette.
4. Extend CSV export with multi-account selection, including a transfer once if either endpoint is selected.
5. Implement full-backup creation; implement CSV import and empty-wallet-only restore after their remaining details are confirmed.
6. Verify V3 regressions and the V4 flows, including theme persistence, search, single/multiple selections, category icon/color independence, selected-account exports, and data round trips. Exercise error and cancellation paths for import and restore after their specifications are settled.

## Deferred to V5

- Add a row-level preview of invalid and duplicate entries and allow the user to select valid rows for partial import.
- Add more comprehensive handling for bank CSVs that have no category column; the specific behavior is to be decided later.

## Details awaiting confirmation before implementation

- Bank CSV column mapping, including supported amount-direction formats and missing time values. The user chose to decide the amount formats later.
- A lossless handling rule for spreadsheet-safety prefixes in existing Wallet CSV exports.

## Implementation checkpoint — 12 November 2026

Implementation started from V3 after verifying all six archived checksums and the complete Git history bundle in `repository/archives/wallet-v3-2026-11-12-r01/`. The archived source, history, debug APK, and unsigned release APK remain untouched.

Implemented Settings, persistent light/dark selection, compact/expanded live Records search, native square radio styling, navigation/action/account/type vectors, seven additional category icons on a separate borderless grid, and selected-account CSV export. Transfers match either selected endpoint once. Existing export columns and spreadsheet-safety behavior remain unchanged pending the import contract.

Full backup uses versioned INR JSON with exact integer strings, account opening balances, category metadata/archived state, transaction IDs and relationships, and preserved legacy note data. Restore validates the complete document before writing and rechecks the untouched-empty-wallet condition inside one Room transaction. Backup selection uses Android document pickers; files over 20 MB are rejected on restore. Theme preference stays device-local and is separate from ledger backup.

CSV import is pending: the user will provide example bank statements so bank formats and missing-field handling can be determined. The Settings entry currently explains that import is unavailable in this development build. Do not mark V4 complete or archive its chat until import and remaining acceptance checks are finished.

Build version is `4.0-dev` (code 4), database schema remains 2. No new runtime dependencies were added.

### Checkpoint verification

- 23 JVM tests passed, including selected-account export and either-endpoint transfer inclusion.
- 32 API 37 instrumentation tests passed. These include backup round trips, invalid reference/version rejection, nonempty-wallet protection, compact search/Settings/theme controls, additional icon selection across recreation, and V1–V3 regressions.
- Lint: zero errors, 44 warnings. Debug and optimized unsigned release builds passed.
- Direct instrumentation result: `repository/.tools/v4-instrumentation-final.txt`. The first attempt encountered an emulator System UI ANR; the successful run followed clearing that dialog and stopping build daemons.
- A final visual check found the launcher wallet asset unsuitable for a tinted navigation icon; it was replaced with a dedicated line-art account vector. This resource-only correction was rebuilt and checked with lint.
- Development APKs and checksums are under `repository/dist/wallet-v4-dev-*`. Release APK requires signing. CSV import and final V4 acceptance remain outstanding.
