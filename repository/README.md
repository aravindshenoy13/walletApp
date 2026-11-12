# Wallet V4.4

A personal, on-device INR transaction tracker for Android 8.0 (API 26) and later. Built with Kotlin, native Android Views/XML, and Room. The product scope is defined in [ROADMAP.md](../ROADMAP.md); the simple appearance follows [V3 wireframe revision 03](../wireframes/wallet-v3-wireframe-r03.html) and the subsequently confirmed decisions in the wireframe README.

## Current V4.4 behavior

- Fixes the dark-mode category color exception affecting Add record, Categories, and other category badges. Blended display colors remain opaque; saved category choices are unchanged.
- Records search collapses when the keyboard is hidden as well as on Back, Done, outside tap, or leaving. Dismissal clears focus and restores the explicit themed screen background. Clear keeps search open for another query.
- Bottom navigation has matched 24 dp line icons, stroke weights, centered artwork, and equal-height touch targets. Secondary titles share the same typography and theme color; Settings spacing and date-range button sizing are consistent with the other screens.

Version code 10. Room schema 3, backup format 2 (including format-1 restore support), exact money calculations, and CSV behavior are unchanged. See [V4.4 implementation and validation](../V4.4-PLAN.md).

Verification: 25 JVM tests and all 46 Android cases passed across the regression run and corrected legacy-test reruns. Six selected UI cases passed at 130% text in both themes. Debug/release builds passed; lint reported zero errors and 50 warnings. The original brightness symptom was not consistently reproduced; final emulator screenshots retain the same background after Clear and dismissal.

Artifacts: [installable V4.4 debug APK](dist/wallet-v4.4-debug.apk), [unsigned optimized release](dist/wallet-v4.4-release-unsigned.apk), and [SHA-256 checksums](dist/wallet-v4.4-SHA256SUMS.txt). Release distribution requires signing.

## V4.3.2 behavior

Action outlines and alignment are refined, dark-mode pastel category colors are brighter, transfer amounts use a lighter blue in dark mode, category rows omit the Custom label, and Analysis summary boxes use square corners.

## V4.3.1 behavior

Analysis totals share a compact outlined box. Records dates are unboxed, the Today/month selection uses a switch, calendar borders are thinner, and category rows are more compact. Dark-mode category backgrounds are brighter while retaining white icon contrast. Add, Save, and Delete use blue, green, and red outlines respectively. Transfer colors and stored data remain unchanged.

## V4.3 behavior

- Tap the outlined **Today** summary on Records to switch to the full current calendar month, including future-dated transactions in that month. Tap again for Today. Rotation retains the selection; date changes refresh the totals. Search and the dated list remain independent of the summary. Transfers are excluded.
- Accounts show their name, icon, current balance, and separate Edit action. Opening balance remains editable and included in balances/backups, but is hidden from the list and transaction history.
- Analysis offers **Daily Breakdown**, **Category Expense Graph**, and **Flow Graph**. Daily Breakdown shows a full month with month arrows. The other views share a custom range, initially month-to-date, across all accounts. The active dates appear above Custom range; each summary group has an outline.
- Flow Graph retains Net/Income/Expenses and has no category list. Category Expense Graph shows only categories with negative net, proportional to their absolute values, with its own list and all-account category history. Hold a wheel sector to see its percentage; release or cancel to dismiss. The hole and outside do not select sectors. Screen readers retain exact category values and percentages.
- Compact INR labels use K (thousand), L (lakh), M, B, and T. L applies at 100,000 through below 1,000,000 after the existing three-significant-digit rounding. Accounting amounts remain exact.
- Category icon choices show icons without visible names; accessibility labels and independent color selection remain. Settings includes a Dark mode icon. Buttons, secondary headers, day headings, calendar borders, and wheel outlines follow both themes.

No database or backup format change is required: Room schema 3 and full-backup format 2 remain, including format-1 restore support. All data remains on-device. See [V4.3 plan and validation](../V4.3-PLAN.md).

V4.3 verification: 25 JVM tests and all 42 Android cases passed across the regression run and targeted retries; all four new V4.3 cases also passed at 130% text in both themes. Debug and optimized release builds passed. Lint: zero errors, 47 warnings. See the plan for retry details and validation limits.

Artifacts: [installable V4.3 debug APK](dist/wallet-v4.3-debug.apk), [unsigned optimized release](dist/wallet-v4.3-release-unsigned.apk), and [SHA-256 checksums](dist/wallet-v4.3-SHA256SUMS.txt).

## Earlier version behavior and implementation history

The sections below describe earlier version checkpoints; current behavior above supersedes their Records summary, Analysis account filters/view names, and opening-balance visibility.

## Using the app

1. Open **Accounts** and add an account with a name and opening balance. All accounts behave identically, including accounts with negative balances.
2. Open **Records** and choose **+ Add record**. Enter an expense, income, or transfer with an account, category when applicable, amount, optional description/payee, and occurrence date/time. A blank description is allowed for every type and exports as an empty CSV column. Tap the category field to open a separate list with icon badges and a case-insensitive name search. Tap a category to return to your draft; Back cancels without changing it. Income and expenses share the same category list. Transfers require two different accounts.
3. Tap a record to edit it or delete it. Balances recalculate after every change. Tap an account to view its current balance and dated history, including signed transfers and opening balance. The balance sits in a padded right-hand section; **Edit** appears underneath it and opens the account name/opening-balance editor.
4. The Records summary always shows today's income, expense, and net totals. The list below shows all records grouped by occurrence date, newest first. Date headings do not change the summary. Income amounts are green, expenses muted red, and transfers blue with a blue two-arrow badge. Transfers are excluded from income and expense totals.

5. Open **Categories** to add a custom category or edit any category, including built-ins. Choose among six black line icons and 16 background colors independently. Names are trimmed, 1–80 characters, and unique without regard to case, including archived names. New categories start with Other/Sky.
6. Archive a category from its editor after confirming. It stays visible and editable in Categories, and old records retain it; new assignments offer active categories only. An existing record can keep its archived category. At least one category must remain active. V2 has no restore action; an archived category can be renamed to free its old name.
7. On **Records**, choose **Export CSV**, select **All transactions**, **Income**, **Expenses**, or **Transfers**, then **Choose location & save**. Select a location in Android's file picker. The file is always CSV. Cancelling leaves wallet data unchanged; a failed write can be retried with the same selection.

8. Type in the outlined search field at the top right of **Records** to match description/payee or amount, and use × to clear. Today's totals stay unchanged. Category/account names are not searched; CSV still exports all records or the selected transaction type. Net amounts in Records and Analysis are green when positive, red when negative, and neutral at zero.
9. Open **Analysis** for two separate sections. **Monthly overview · all accounts** shows income, expenses, net, and a daily net calendar for the displayed full month. Month arrows update the calendar and totals together. The calendar is summary-only; green/red signed amounts are rounded to fit (K/M/B/T), with exact net amounts available to accessibility. Transfers and opening balances are excluded. **Detailed analysis**, below a divider, defaults to the first of the current month through today. Its account selector and inclusive **Custom range** affect the compact range totals, larger daily graph and category breakdown. A single Net/Income/Expenses chooser controls both graph and categories, defaulting to Net. Daily net plots income minus expenses, green above zero and red below zero. Tap a category for dated transactions within that detailed selection. Reimbursements increase category net; zero-value categories are hidden. Reopening Analysis resets both sections; rotation retains their independent choices.

V3 uses light mode even when the device uses dark mode. Account-specific CSV export, full backup/restore, settings, dark mode, and additional/app-wide icons remain V4. There is no category filter or past-date balance screen. No sample accounts or transactions are inserted at startup.

## CSV format

The file is UTF-8 with a BOM and CRLF row endings, named `wallet-transactions-YYYY-MM-DD.csv` using the export date. It contains these columns:

```csv
record_id,date,time,type,amount,currency,account,to_account,category,description
```

Rows include all transactions or only the selected type, from oldest occurrence to newest, then by local record ID. A selection with no matches exports the column header only. Dates/times use `YYYY-MM-DD` and `HH:mm:ss`; types are `income`, `expense`, or `transfer`. Positive amounts use exact two-decimal INR values without grouping. Transfers appear once, with a destination account and blank category. Other rows have a blank destination. Current account/category names are used, including archived categories.

Commas, quotes, and line breaks are CSV-escaped. Formula-like text (leading `=`, `+`, `-`, or `@` after whitespace, or an initial tab/newline) receives an export-only apostrophe so spreadsheet applications treat it as text. Stored data is unchanged. Opening balances, category appearance, and hidden legacy notes are excluded: this file is a transaction export, not a full wallet backup.

## Build and install

`WalletApp/repository/` is the Android project root. Open this folder in Android Studio and run all commands below from it. App source, Gradle configuration, caches, build output, and APKs belong here; planning documents and wireframes remain in the parent WalletApp directory.

Build with Android SDK Platform 37 and Build Tools 36.0.0 installed. Use JDK 17 or a compatible newer JDK. The checked-in wrapper downloads Gradle 9.7.1 and verifies its SHA-256; AGP 9.5.1 supplies built-in Kotlin support. Dependencies are pinned. A first build needs internet access to download build tools and libraries.

Configure the SDK location through Android Studio, `ANDROID_HOME`, or an untracked `local.properties` file. A Windows example is `sdk.dir=C\:/Users/your-name/AppData/Local/Android/Sdk`.

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On macOS/Linux use `./gradlew`. The debug APK is directly installable. `:app:assembleRelease` produces an **unsigned** APK with R8 optimization and resource shrinking; distribution signing is left to the owner and no signing keys are checked in.

## Verification

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
.\gradlew.bat :app:connectedDebugAndroidTest
```

Run connected tests on a disposable emulator or test device: the UI test fixture clears the installed debug app's wallet data. Tests cover money and accounting, migration from the checked-in V1 schema, category validation and metadata updates, archiving, independent appearance choices, draft recreation, CSV serialization, failed-write retry, picker cancellation, and the V1 entry flows. Reports are under `app/build/reports/`. UI tests capture screenshots in the app's external-files `qa/` directory; collect them before uninstalling the test app (Gradle may uninstall it during teardown).

Verified the refined V3 on 11 November 2026: all 19 JVM and 28 API 37 Android tests passed, including optional descriptions, CSV empty columns, net colors, search, range/account scope, and prior entry flows. Lint reported 0 errors (25 warnings); debug and R8 release builds passed. The optimized release opened Analysis and account history with saved data and no crash output. The original installed V2→V3 upgrade preserved every account, record, category, and ID sequence. API 26 compatibility was checked by lint. See [V3 validation details](../V3-PLAN.md).

Verified artifacts: [installable V3 debug APK](dist/wallet-v3-debug.apk), [unsigned V3 release APK](dist/wallet-v3-release-unsigned.apk), and [SHA-256 checksums](dist/wallet-v3-SHA256SUMS.txt). The release APK requires signing before installation.

Earlier V2 results and artifact details are recorded in [the V2 implementation plan](../V2-PLAN.md). Its standalone source/history/APK archive is preserved separately from V3.

## Data and implementation

- Room stores accounts, records, and categories in the app-private `wallet.db`; there are no network permissions. Android cloud backup and device transfer are disabled in the manifest and extraction rules. CSV writes only to the location selected by the user.
- Amounts and opening balances use signed 64-bit integer paise. Transactions require positive amounts. Aggregates use `BigInteger` so totals cannot wrap around; formatting uses exact decimal INR amounts.
- A transfer is one database row referencing both accounts. Balances are derived from opening balances and records, so edits/deletions cannot leave a separate balance cache out of sync. Writes and observable ledger snapshots are transactional.
- Occurrence timestamps preserve the entered local date and time, including after a device time-zone change. No opening-balance effective date is modeled.
- Database schemas 1 and 2 are checked in under `app/schemas/`. The explicit 1→2 migration seeds 12 categories with the original IDs, preserves every record column and the record ID sequence, and adds a category foreign key. There is no destructive-migration fallback.
- V3 keeps database schema 2 and adds read-only search/analysis/history calculations. Analysis excludes transfers and opening balances, applies inclusive local dates and one optional account ID, and aggregates with `BigInteger`. Graph coordinates alone use normalized drawing values; the graph allocates sparse daily points rather than a list of every date.
- Editors keep drafts across recreation and keep writes in a ViewModel to prevent double saves during rotation.
- Six Lucide icons are bundled as vectors, without a runtime icon dependency. Notices are in `third_party/LUCIDE-LICENSE.txt` and bundled in `assets/licenses/lucide.txt`.

Accepted versions are preserved at tags `wallet-v1-2026-11-11-r01` and `wallet-v2-2026-11-11-r01`, with standalone [V1](../backups/wallet-v1-2026-11-11-r01/README.md) and [V2](../backups/wallet-v2-2026-11-11-r01/README.md) archives. The roadmap and earlier wireframes remain in the parent WalletApp directory.


Final accepted V3 is preserved at tag `wallet-v3-2026-11-12-r01`, with a standalone source/history/APK archive under `../backups/wallet-v3-2026-11-12-r01/`. See [final V3 notes](../V3-PLAN.md) for behavior and validation limits.


## V4 development checkpoint

V4 development adds Settings, persistent dark mode, compact expandable search, app icons, additional category icons, selected-account CSV exports, and full JSON backup/empty-wallet restore. CSV import is removed from product scope. See [V4 implementation notes](../V4-PLAN.md). The frozen V3 archive remains under `../backups/wallet-v3-2026-11-12-r01/`.

V4 checkpoint validation: 23 JVM tests and 32 API 37 instrumentation tests passed; lint has zero errors. Installable development APK: `dist/wallet-v4-dev-debug.apk`. Optimized release APK: `dist/wallet-v4-dev-release-unsigned.apk` (requires signing). CSV export and full-backup restore remain supported; CSV import is not included.


## V4.1 — Analysis views

Analysis now uses a drop-down for Calendar, Category Breakdown, and Flow Graph. Calendar retains the full-month/all-account scope; detailed views share independent date-range and account controls. The category wheel shows only negative category net values, sized by magnitude, with signed amounts in the accompanying list. Transfers, zero values, and positive-net categories are excluded from the wheel. Flow Graph retains Net/Income/Expenses. State survives recreation. No database migration or new runtime dependency is needed.

V4.1 validation: 23 JVM tests and 33 API 37 Android tests passed; lint has zero errors. APKs: `dist/wallet-v4.1-debug.apk` and `dist/wallet-v4.1-release-unsigned.apk` (release requires signing). See [V4.1 notes](../V4.1-PLAN.md).

## V4.2 — visual refinements and account icons

V4.2 uses outlined selection states, a menu icon beside Wallet, themed Settings/dropdowns, readable dark-mode category badges and wheel slices, and neutral positive account balances. Records search clears and collapses on Back, Done, outside tap, or leaving; Clear keeps the empty search field open.

Accounts offer bank, cash and card icons as visual labels only. Existing accounts upgrade non-destructively to bank icons through Room schema 3. New full backups use format 2 and retain icons; V4 format-1 backups remain restorable with bank defaults. CSV is unchanged. See ../V4.2-PLAN.md for implementation and validation.

Validation: 23 JVM tests and all 38 API 37 Android tests passed; three V4.2 UI tests also passed at 360 dp with 130% text in light/dark themes. Debug and R8 release builds passed; lint reported zero errors and 54 warnings. Installable APK: dist/wallet-v4.2-debug.apk. Unsigned release and checksums are alongside it. The standalone archive is ../backups/wallet-v4.2-2026-11-12-r01/.
