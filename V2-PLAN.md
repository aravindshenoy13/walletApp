# Wallet V2 Plan Draft

V2 adds custom categories, category icons, category filtering, and transaction CSV export, following [ROADMAP.md](ROADMAP.md). This is a planning draft; Android implementation has not started.

V1 is frozen at Git tag `wallet-v1-2026-11-11-r01`. Its source, complete Git history, and fresh APKs are saved in [the V1 backup](backups/wallet-v1-2026-11-11-r01/README.md). All application work for V2 will stay inside `repository/`.

## Scope

| Area | V2 behavior |
| --- | --- |
| Category management | Create categories and manage their names, icons, and availability for new records. Keep one shared category set for income and expenses. |
| Category icons | Choose from a small bundled icon palette. Show the icon in category management, category selection, and record rows. |
| Record filtering | Filter the existing date-grouped record list by category, with a clear All categories option. |
| CSV export | Save transaction rows as a CSV file through Android's save-file picker. |

Keep the accepted V1 form arrangement, description/payee field, income and expense colors, transfers, exact money calculations, and account behavior. The Records summary stays on the current day; Records gets no historical date navigation. No separate note field is added.

Historical analysis and date-range totals remain V3. CSV import, full backup/restore, dedicated dark-mode work, and app-wide icons remain V4. V2 adds category icons only.

## Choices to confirm

| Choice | Proposed default | Alternative |
| --- | --- | --- |
| Existing V1 categories | Allow editing names/icons and archiving them alongside custom categories. | Keep built-in categories fixed and manage custom categories only. |
| Category filter | Select one category at a time. | Select multiple categories. |
| Today's totals with a filter | Show today's totals for the selected category and label that scope. | Keep today's totals for all categories and label that scope. |

These defaults are proposals. The user's answers determine the wireframe and behavior.

## Category behavior proposal

- Add a Categories tab beside Records and Accounts. Keep the simple lines-and-text layout.
- A category needs a nonblank name and an icon from a bundled palette; provide a default icon. Trim names and reject duplicates without regard to letter case.
- Category direction stays separate from transaction type. The same category can be selected for income or expense. Transfers have no category.
- Renaming a category updates its displayed name on linked records without changing amounts, occurrence times, or balances.
- Archive and restore categories instead of deleting their historical records. Archived categories disappear from new-record choices but remain visible on existing records and available for historical category filtering.
- Editing an existing record can preserve its archived category. Choosing a different category uses the active list. Keep at least one active category available.

The built-in-category editing rule remains subject to the choice above. The archive/restore policy will be reviewed with the category wireframe.

## Records and form changes

Put the category filter near the Records summary. With All categories selected, show all transaction types, including transfers. A specific category matches income and expense records; transfers do not match because they have no category.

Continue grouping visible records by their occurrence date, newest first. A filter never changes account balances. Provide an empty state when nothing matches and keep the selected filter across screen rotation. Whether today's totals follow the filter is an open choice above; either way, the label must make the scope clear.

Use the managed category list in the record form's existing category position. Show category names with their icons. The transfer form continues to replace category selection with the destination account.

## CSV export proposal

Add Export CSV on Records. Default to exporting all records; when a category filter is active, also offer exporting the matching records. Do not add a date-range export control in V2.

Use the Android save-file picker so the user chooses the destination without broad storage permission. Take a consistent snapshot and write the file off the main thread. Report success only after writing finishes; cancellation or a write failure must leave wallet data unchanged. See [Android document storage](https://developer.android.com/training/data-storage/shared/documents-files).

Proposed columns for review:

```csv
record_id,date,time,type,amount,currency,account,to_account,category,description
17,2026-11-11,13:30:00,expense,250.50,INR,Cash,,Food,Lunch
18,2026-11-11,14:00:00,transfer,2000.00,INR,Bank,Cash,,ATM withdrawal
```

- Export one row per transaction, ordered oldest first, with record ID as the tie-breaker.
- Preserve the entered local date/time, using dates such as `2026-11-11`, 24-hour times such as `13:30:00`, and `income`, `expense`, or `transfer`.
- Amounts stay positive, use two decimal places without digit grouping, and carry `INR` in the currency column. Transaction type determines direction.
- A transfer uses `account` for its source and `to_account` for its destination; its category is blank. Income and expense leave `to_account` blank.
- Use UTF-8 with a header. Confirm the BOM choice with the CSV preview. Quote commas, quotes, and line breaks correctly, following [RFC 4180 quoting rules](https://www.rfc-editor.org/info/rfc4180/).
- Agree on and test a literal-text escape policy for formula-like account names, category names, and descriptions before implementation. Spreadsheet handling varies; [CWE 1236](https://cwe.mitre.org/data/definitions/1236.html) describes the issue.

This exports transactions, not opening balances or a complete account/category backup. Record IDs are local to the wallet database. Future CSV import rules are a separate V4 decision.

## Upgrade and data preservation

Replace the fixed category enum with persisted categories containing stable IDs, names, icon keys, and archive status. Seed the V1 categories and preserve existing category associations. Names and icons must be editable without changing category IDs.

Category changes must refresh record rows and form choices while keeping financial calculations unchanged. Fresh installs receive the default category set once; reopening the app must not duplicate it.

Use a tested Room schema migration from version 1 to version 2. Preserve every account and record, opening balances, occurrence timestamps, descriptions, transfer links, and legacy data retained internally. Do not add destructive migration fallback. Keep both schema files in version control. See [Room migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions).

Keep Kotlin, native Views/XML, Room, on-device storage, integer-paise amounts, exact aggregates, and R8 release optimization. Use bundled vector icons and a small CSV writer without adding a large UI or export library.

## Work sequence

1. Resolve the category/filter choices and review a CSV preview. Save the first V2 wireframe as `wireframes/wallet-v2-wireframe-r01.html` for review.
2. After an explicit request to implement V2, add the category storage and migration, then category management and icons.
3. Connect managed categories to record editing and add category filtering with the agreed totals behavior.
4. Add CSV export, then verify an upgrade from V1 and the complete V2 flow.

## Acceptance checks

- A populated V1 database upgrades without losing records or changing balances and totals.
- Category creation, renaming, icon changes, archiving, and restoration persist after restarting the app; shared income/expense use works.
- Archived category links remain readable and editable. Category filtering returns the correct records and handles empty results, transfers, and rotation.
- The Records summary uses the current day and the agreed filter scope; past record dates do not become summary controls.
- CSV export handles all/filtered scope, transfers, an empty wallet, exact amounts, Unicode, commas, quotes, line breaks, formula-like text, cancellation, and write failures.
- V1 regression checks and the new migration/UI/export checks pass. Lint and debug/R8 release builds complete successfully.
