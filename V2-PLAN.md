# Wallet V2 Implementation Plan

This plan translates [ROADMAP.md](ROADMAP.md) and the completed [V2 wireframe revision 03](wireframes/wallet-v2-wireframe-r03.html) into implementation checkpoints. Revision 03 supersedes the earlier visual drafts. All six checkpoints are implemented and validated in `repository/` as of 11 November 2026.

The accepted V1 remains available at Git tag `wallet-v1-2026-11-11-r01` and in [the local V1 backup](backups/wallet-v1-2026-11-11-r01/README.md). All application source, resources, tests, build commands, and APKs belong inside `repository/`.

## Confirmed V2 scope

| Deliverable | Required behavior |
| --- | --- |
| Category management | One shared list for income and expenses. Add custom categories; rename, change appearance, and archive built-in and custom categories. |
| Category appearance | Six line-art icons and 16 background colors from revision 03. Icon and color are independent selections. |
| Records integration | Show category badges and names while preserving today's totals and the date-grouped record list. |
| CSV export | Export all transactions or only income, expenses, or transfers through the Android save-file picker. CSV is the fixed format; omit the format row. |
| Light mode | Use light mode throughout V2, with black category line art on the selected backgrounds. |

Category filtering and historical analysis stay in V3. CSV import, full backup/restore, dark mode, app-wide icons, and additional category icons stay in V4. Keep the accepted V1 account behavior, transaction types, form arrangement, description/payee field, and income/expense colors. Records has no date navigation, and the form has no separate note field.

## Checkpoint 1 Data model and migration

**Files:** `repository/app/src/main/java/com/aravind/wallet/data/`, `domain/Ledger.kt`, and `repository/app/schemas/`.

Replace the fixed `Category` enum with a persisted category model. Implemented fields are:

| Field | Purpose |
| --- | --- |
| `id: String` | Immutable identifier. Retain V1 keys such as `FOOD`; assign UUIDs to custom categories. |
| `name: String` | Editable display name. |
| `iconKey: String` | One of the six bundled icon keys. |
| `backgroundColorKey: String` | One of the 16 approved palette keys. |
| `isBuiltIn: Boolean` | Supports the Built-in/Custom origin labels without restricting editing. |
| `isArchived: Boolean` | Removes a category from choices for new transactions while preserving existing links. |

Use `WalletRecord.categoryId: String?` mapped to the existing physical `category` column. Remove the enum converter so custom IDs can be read. Add a category foreign key and index; transfers retain a null category. Keep money storage and calculations unchanged.

Implement an explicit Room migration from schema 1 to schema 2:

1. Create the category table and seed the 12 existing categories with their original IDs and names.
2. Rebuild the records table as needed for the category foreign key. Copy every column, including the legacy `note` column retained internally, all record IDs, account references, and occurrence timestamps.
3. Preserve the record AUTOINCREMENT sequence, existing account foreign keys, and indexes. Category deletion must not cascade into records.
4. Register the migration without destructive fallback and check in schema 2 alongside schema 1. Fresh installs seed the same defaults once, transactionally, before presenting entry choices.

Extend the observed read model to include category changes. Renames and appearance changes must refresh visible records and pickers even when no transaction changes. Export reads must obtain accounts, categories, and records from one consistent snapshot. Keep the current Room 2.8.5 dependency. The migration test constructs its fixture from the saved V1 schema and opens the actual V2 Room database, exercising Room's generated schema validation. Follow [Room migration and schema-testing guidance](https://developer.android.com/training/data-storage/room/migrating-db-versions).

**Checkpoint complete when:** a populated V1 database upgrades with identical balances, daily totals, record IDs, timestamps, and legacy data; a fresh install has exactly 12 defaults; reopening duplicates nothing.

## Checkpoint 2 Icon catalog and light mode

**Files:** new category catalog/renderer under `repository/app/src/main/java/com/aravind/wallet/`, vector/background resources under `repository/app/src/main/res/`, and `WalletApplication.kt`.

Bundle only these six icon shapes as Android VectorDrawables, using the matching Lucide artwork and retaining [the upstream license notices](https://lucide.dev/license). No runtime icon download or large icon dependency is needed.

| Stored key | Visible choice |
| --- | --- |
| `utensils` | Food |
| `shopping-basket` | Basket |
| `house` | Home |
| `book-open` | Book |
| `briefcase` | Work |
| `tag` | Other |

Use the exact revision 03 palette, in this order, in a single selector labeled **Background color**:

| Color key | Hex | Color key | Hex |
| --- | --- | --- | --- |
| Red | `#D56565` | Rose | `#F2C7C7` |
| Orange | `#C88645` | Peach | `#F7D5BE` |
| Yellow | `#B7A13C` | Butter | `#F3E8B8` |
| Green | `#5C9B70` | Mint | `#CCE8D4` |
| Teal | `#479C98` | Seafoam | `#C5E6E2` |
| Blue | `#598FD0` | Sky | `#C9DEF5` |
| Purple | `#A177CA` | Lilac | `#DFD0EF` |
| Pink | `#C46F9D` | Blush | `#F2D0E2` |

The selector order is the eight normal colors followed by the eight pastels; the table pairs them only for reference. Do not add separate color-group headings to the UI.

Create a reusable category badge renderer for management rows, form choices, previews, and record rows. Changing an icon preserves the selected background; changing a background preserves the icon. Match the black strokes and small rounded backgrounds in the wireframe. Selection must also be identifiable by a border/check and an accessible name, with adequate touch targets.

Seed appearance defaults deterministically. Match the wireframe's Food/Peach, Groceries/Mint, and Salary/Butter examples; use semantic matches from the six available shapes for the remaining defaults. Record the complete seed mapping with the migration so later catalog changes cannot alter old migration behavior.

V1 currently uses a DayNight theme and night resources. Set the app's V2 display mode to `MODE_NIGHT_NO` before screens are created, and verify that system dark mode still produces the approved light UI. See [AppCompat night-mode behavior](https://developer.android.com/reference/androidx/appcompat/app/AppCompatDelegate#MODE_NIGHT_NO). Do not add a theme toggle or a larger icon catalog in V2.

**Checkpoint complete when:** all six vectors and 16 colors render offline, both selectors work independently, and the same appearance can be reproduced after reopening an editor.

## Checkpoint 3 Category screens and mutations

**Files:** category DAO/repository operations, category editor Activity/ViewModel, category list adapter, and XML layouts under `repository/app/src/main/`.

Map wireframe panels 02–05 to the native Categories tab, Add/Edit Category screen, and archive confirmation. Keep the shared list and Built-in/Custom labels. Built-in origin remains unchanged when a user renames a category.

The editor contains the name, six icon choices, the single 16-color selector, a live preview, and Save. Edit loads the stored choices and offers Archive. Preserve the name, icon, and color draft across recreation; disable repeated writes while saving. Keep archive confirmation and cancellation as shown in revision 03. Archive targets the saved category and must not implicitly save other unsaved edits.

Implemented validation rules are a trimmed name of 1–80 characters, case-insensitive name uniqueness across active and archived categories, and valid catalog keys. Check uniqueness in the same database transaction as the write so simultaneous requests cannot create duplicate categories. At least one category must remain active. Archived categories stay visible and editable in management; there is no Restore action in V2. An archived category can be renamed to release its old name.

Use screen-owned mutation state. The current `WalletViewModel.perform()` stops accepting writes after `Operation.Saved`; that terminal state suits an editor that closes, but must not prevent a second archive operation on the persistent Categories tab.

**Checkpoint complete when:** add, rename, appearance edits, and repeated archive actions work for built-in and custom categories; cancellation changes nothing; all changes persist without changing financial totals.

## Checkpoint 4 Records and transaction form integration

**Files:** `MainActivity.kt`, `LedgerAdapters.kt`, `RecordEditorActivity.kt`, relevant ViewModels, and record/category/account XML rows.

Add the text-only Categories navigation tab and replace the current two-tab Boolean with three-tab state that survives recreation. On Records, use the header's Export CSV action and the body Add record button shown in panel 01. Accounts keeps its existing add/edit flow.

Record rows follow revision 03: category badge/name and amount, followed by time, account, and description/payee. Transfers remain plain rows without a category badge. Preserve income/expense colors and today's totals across all categories; category edits and archiving never change balances or totals.

Replace enum-backed category selection in the transaction form with managed categories and their badges. Keep the V1 account/category placement, type choices, date/time pair, amount, and description arrangement. Transfer mode continues to substitute the destination account for category selection.

Restore category selection by stable ID rather than enum name, ordinal, or adapter position. Refresh category choices without overwriting unrelated unsaved form fields. An existing record may keep its archived category; new assignments must use active categories. Saving as a transfer stores a null category; a saved transfer converted back to income/expense offers active categories.

The existing account and record adapters share `item_record.xml`. Separate the account row layout if needed so the new category presentation does not leak into Accounts.

**Checkpoint complete when:** category changes appear immediately on linked rows, edited records keep the correct category after list reordering/recreation, transfers remain valid, and account presentation and money behavior pass V1 regression checks.

## Checkpoint 5 Export transactions as CSV

**Files:** new export Activity/ViewModel, an export snapshot query, a small CSV writer, and the export layout under `repository/app/src/main/`.

Follow panel 06 with the user's subsequent export refinement: offer All transactions (default), Income, Expenses, or Transfers as a single selection. The format is always CSV, so omit the format row. Choose location & save opens `ActivityResultContracts.CreateDocument` with MIME type `text/csv`. Preserve the type selection across recreation, picker cancellation, and retries, and lock it while choosing/writing. Use the returned document URI with the content resolver. The existing Activity dependency already supplies this contract. See [CreateDocument](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument).

After the user chooses a location, obtain a consistent read-only snapshot and write it off the main thread. Do not hold a database transaction open during the file-picker interaction or file writing. Keep choosing, writing, saved, and error states distinct; prevent duplicate export launches during a write and preserve state across rotation. Picker cancellation returns to the export screen without changing data. Report success only after the stream closes; report write failures and remove a newly created partial document when supported.

### Implemented CSV contract

The implementation uses this contract for all-transactions and single-type exports:

```csv
record_id,date,time,type,amount,currency,account,to_account,category,description
17,2026-11-11,13:30:00,expense,250.50,INR,Cash,,Food,Lunch
18,2026-11-11,14:00:00,transfer,2000.00,INR,Bank,Cash,,ATM withdrawal
```

Export one row per matching transaction, oldest first, with ID as the tie-breaker. An empty selection produces only the header. Include records linked to archived categories and use the current category/account display names. Preserve local occurrence date/time, positive two-decimal amounts without grouping, and `income`, `expense`, or `transfer` type values. Use `Money.input()` or an equivalent exact conversion from integer paise. Transfers have a destination and blank category; other types have a blank destination. IDs are local to this wallet.

Encoding is UTF-8 with BOM and CRLF line endings. Apply [RFC 4180 quoting](https://www.rfc-editor.org/info/rfc4180/) to commas, quotes, and line breaks. Prefix formula-like names/descriptions with an apostrophe when the first non-whitespace character is `=`, `+`, `-`, or `@`, or the text starts with a tab/newline. This addresses [CSV formula interpretation](https://cwe.mitre.org/data/definitions/1236.html) without modifying stored wallet data. Filename: `wallet-transactions-YYYY-MM-DD.csv`, using the export date.

Do not export opening balances, category appearance settings, or the hidden legacy note column. This transaction file is distinct from V4 full backup/restore. V2 has no category-filtered or date-range export options.

**Checkpoint complete when:** the approved contract produces a readable all-transactions CSV, includes each transfer once, preserves exact amounts and Unicode, and handles empty data, cancellation, rotation, and failed writes.

## Checkpoint 6 Upgrade and release validation

| Test area | Required evidence |
| --- | --- |
| Migration | Populate a schema-1 fixture with positive/negative opening balances, both category directions, transfers, backdated entries, legacy notes, and a deleted highest record ID. Verify every retained value, category link, sequence, and balance after migration. |
| Category operations | Seed once; reject blank/duplicate/invalid-key writes; allow editing built-ins; refresh subscribed lists after metadata-only edits; preserve records through archiving. |
| Editor and visuals | Exercise independent icon/color selection, saved drafts, reordered category choices, selected archived categories, and adequate touch/accessible selection states. Compare against revision 03. |
| Export | Verify row counts/order, transfer blanks, exact money, Unicode, embedded commas/quotes/newlines, formula-like text, empty exports, stream closure, errors, and cancellation. |
| V1 regression | Verify record/account creation and edits, record deletion, negative balances, transfers, all-date grouping, and current-day-only totals. |
| Installed upgrade | On a disposable emulator, populate V1 and install V2 over it using the same application ID and signing key. Do not uninstall or clear data during the upgrade check. |

Run unit tests, connected Android tests, lint, debug assembly, and R8 release assembly from `repository/`. Keep the existing dependency versions. Expose `app/schemas/` as Android test assets so migration tests can load schema 1. Use the existing SQLite test APIs rather than adding Room's test-helper dependency, which encountered a serializer compatibility error in this environment. Validate the minimum supported API 26 and the current API 37 where emulator availability permits, including a device configured for system dark mode.

Set `versionCode = 2` and `versionName = 2.0` before the installed upgrade check. After validation, update the app README and copy verified APKs into `repository/dist/`. Leave the accepted V1 backup and tag intact. Release signing remains separate from the unsigned release build.

## Implemented defaults

1. **Archive recovery:** no Restore action was added. Archived categories remain visible and editable in Categories, preserving historical links and allowing names to be reused after renaming the archived category.
2. **Category edge rules:** keep at least one active category and enforce unique names across active and archived categories. New-category default appearance is Other/Sky. The 12 seed mappings are frozen in `Migration1To2.kt`.
3. **CSV contract:** use the ten columns, UTF-8 BOM, exact amounts, literal-text escaping, and dated filename documented above. CSV import decisions remain V4.

These defaults were stated when implementation began. The confirmed V2 scope, six icons, independent 16-color selector, light-mode appearance, and CSV export flow remain the implementation baseline, with single-type export added at the user's request.

## Completed validation — 11 November 2026

| Check | Result |
| --- | --- |
| JVM suite | 11 tests passed: money/accounting and CSV serialization. |
| Android suite | 19 tests passed on a dedicated API 37 emulator: migration, repository rules, categories, export, and V1 UI regression. |
| Lint | 0 errors, 15 warnings. |
| Build | Debug and R8/resource-shrunk release assembly passed; version 2.0, code 2. |
| Installed upgrade | Installed archived V1, populated two accounts and three records, deleted record 100, then installed V2 without clearing data. All retained values, legacy notes, 12 seeded categories, balances (6350 and -2500 paise), and sequence 100 were preserved. |
| CSV system save | Saved an actual transaction CSV through Android's document picker to Downloads and read it back successfully. Cancellation and failed-provider retry also passed. |
| Visuals | Reviewed category management/editor, all six vectors and 16 colors, record badges, and export screens. Independent choices and editor recreation passed. The app remained light with system night mode enabled. |
| Optimized release | A temporary copy signed with the local debug key installed over the debug app and opened Records/Categories with saved data. No crash output; distribution release remains unsigned. |
| V1 preservation | V1 tag still points to `3908f31e3e7c9291023cc0a035826b563a246005`; all six backup checksums match. Existing roadmap/wireframe file hashes are unchanged. |

API 26 compatibility was checked by lint; device execution was limited to available API 37. Local test logs and screenshots are under ignored `repository/.tools/v2-qa/`. Installable debug and unsigned release APKs are in `repository/dist/`, with hashes in `wallet-v2-SHA256SUMS.txt`. The app README documents usage, archive behavior, and the CSV contract.

### Transaction-type export refinement

Added All transactions, Income, Expenses, and Transfers choices and removed the fixed format row at the user's request. Follow-up validation: all 12 JVM tests and 3 focused export Android tests passed; debug/test APK assembly, lint (0 errors, 15 warnings), and R8 release assembly passed. Tests cover all three type filters, empty matches, unchanged source data, selection recreation, picker cancellation, and retaining the filter through failed-provider retry. Debug and unsigned release APKs and their checksums were refreshed. The earlier full-suite and installed-upgrade results above describe the original V2 validation.
