# Wallet V3

A personal, on-device INR transaction tracker for Android 8.0 (API 26) and later. Built with Kotlin, native Android Views/XML, and Room. The product scope is defined in [ROADMAP.md](../ROADMAP.md); the simple appearance follows [V3 wireframe revision 03](../wireframes/wallet-v3-wireframe-r03.html) and the subsequently confirmed decisions in the wireframe README.

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

V3 uses light mode even when the device uses dark mode. CSV import, account-specific CSV export, full backup/restore, settings, dark mode, and additional/app-wide icons remain V4. There is no category filter or past-date balance screen. No sample accounts or transactions are inserted at startup.

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


Final accepted V3 is preserved at tag `wallet-v3-2026-11-12-r01`, with a standalone source/history/APK archive under `archives/wallet-v3-2026-11-12-r01/`. See [final V3 notes](../V3-PLAN.md) for behavior and validation limits.


## V4 development checkpoint

V4 development adds Settings, persistent dark mode, compact expandable search, app icons, additional category icons, selected-account CSV exports, and full JSON backup/empty-wallet restore. CSV import awaits bank statement examples and is unavailable in this development build. See [V4 implementation notes](../V4-PLAN.md). The frozen V3 archive remains under `archives/wallet-v3-2026-11-12-r01/`.

V4 checkpoint validation: 23 JVM tests and 32 API 37 instrumentation tests passed; lint has zero errors. Installable development APK: `dist/wallet-v4-dev-debug.apk`. Optimized release APK: `dist/wallet-v4-dev-release-unsigned.apk` (requires signing). This is an unfinished V4 build while CSV import awaits bank examples.
