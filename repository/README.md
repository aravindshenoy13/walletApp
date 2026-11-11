# Wallet V2

A personal, on-device INR transaction tracker for Android 8.0 (API 26) and later. Built with Kotlin, native Android Views/XML, and Room. The product scope is defined in [ROADMAP.md](../ROADMAP.md); the simple appearance follows [V2 wireframe revision 03](../wireframes/wallet-v2-wireframe-r03.html).

## Using the app

1. Open **Accounts** and add an account with a name and opening balance. All accounts behave identically, including accounts with negative balances.
2. Open **Records** and choose **+ Add record**. Enter an expense, income, or transfer with an account, category when applicable, amount, description/payee, and occurrence date/time. Tap the category field to open a separate list with icon badges and a case-insensitive name search. Tap a category to return to your draft; Back cancels without changing it. Income and expenses share the same category list. Transfers require two different accounts.
3. Tap a record to edit it or delete it. Balances recalculate after every change. Tap an account to edit its name or opening balance.
4. The Records summary always shows today's income, expense, and net totals. The list below shows all records grouped by occurrence date, newest first. Date headings do not change the summary. Income amounts are green, expenses muted red, and transfers blue with a blue two-arrow badge. Transfers are excluded from income and expense totals.

5. Open **Categories** to add a custom category or edit any category, including built-ins. Choose among six black line icons and 16 background colors independently. Names are trimmed, 1–80 characters, and unique without regard to case, including archived names. New categories start with Other/Sky.
6. Archive a category from its editor after confirming. It stays visible and editable in Categories, and old records retain it; new assignments offer active categories only. An existing record can keep its archived category. At least one category must remain active. V2 has no restore action; an archived category can be renamed to free its old name.
7. On **Records**, choose **Export CSV**, select **All transactions**, **Income**, **Expenses**, or **Transfers**, then **Choose location & save**. Select a location in Android's file picker. The file is always CSV. Cancelling leaves wallet data unchanged; a failed write can be retried with the same selection.

V2 uses light mode even when the device uses dark mode. Category filters and analysis remain V3; CSV import, full backup/restore, dark mode, and additional/app-wide icons remain V4. No sample accounts or transactions are inserted at startup.

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

Verified on 11 November 2026: 11 JVM tests and 19 Android tests passed, lint reported 0 errors (15 warnings), and debug/R8 release builds passed. An installed V1→V2 upgrade preserved data and the record sequence. Actual system-picker CSV saving and an optimized-release launch with saved data passed. Device testing used API 37 with system dark mode enabled; API 26 compatibility was checked by lint. Details are recorded in [the V2 implementation plan](../V2-PLAN.md).

Verified artifacts: [installable debug APK](dist/wallet-v2-debug.apk), [unsigned release APK](dist/wallet-v2-release-unsigned.apk), and [SHA-256 checksums](dist/wallet-v2-SHA256SUMS.txt). The release APK requires signing before installation.

The subsequent transaction-type export update passed all 12 JVM tests and 3 focused Android export tests, including selection recreation and filtered-write retry. Debug/release assembly and lint passed again (0 errors, 15 warnings); the APKs above include this update.

The searchable category-page and blue transfer-badge update passed all 12 JVM tests and all 22 Android checks across the suite and a targeted rerun of one missed legacy time-picker interaction. Search/selection, cancellation, draft/query recreation, archive rules, and transfer color were verified. Debug/R8 builds passed and lint reported 0 errors (16 warnings). APKs and checksums include this latest update.

## Data and implementation

- Room stores accounts, records, and categories in the app-private `wallet.db`; there are no network permissions. Android cloud backup and device transfer are disabled in the manifest and extraction rules. CSV writes only to the location selected by the user.
- Amounts and opening balances use signed 64-bit integer paise. Transactions require positive amounts. Aggregates use `BigInteger` so totals cannot wrap around; formatting uses exact decimal INR amounts.
- A transfer is one database row referencing both accounts. Balances are derived from opening balances and records, so edits/deletions cannot leave a separate balance cache out of sync. Writes and observable ledger snapshots are transactional.
- Occurrence timestamps preserve the entered local date and time, including after a device time-zone change. No opening-balance effective date is modeled.
- Database schemas 1 and 2 are checked in under `app/schemas/`. The explicit 1→2 migration seeds 12 categories with the original IDs, preserves every record column and the record ID sequence, and adds a category foreign key. There is no destructive-migration fallback.
- Editors keep drafts across recreation and keep writes in a ViewModel to prevent double saves during rotation.
- Six Lucide icons are bundled as vectors, without a runtime icon dependency. Notices are in `third_party/LUCIDE-LICENSE.txt` and bundled in `assets/licenses/lucide.txt`.

The accepted V1 is preserved at tag `wallet-v1-2026-11-11-r01` and in [the V1 backup](../backups/wallet-v1-2026-11-11-r01/README.md). The roadmap and earlier wireframes remain in the parent WalletApp directory.
