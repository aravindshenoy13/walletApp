# Wallet V1

A personal, on-device INR transaction tracker for Android 8.0 (API 26) and later. Built with Kotlin, native Android Views/XML, and Room. The product scope is defined in [ROADMAP.md](ROADMAP.md); the simple appearance follows wireframe revision 02.

## Using the app

1. Open **Accounts** and add an account with a name and opening balance. All accounts behave identically, including accounts with negative balances.
2. Open **Records** and choose **+ Add record**. Enter an expense, income, or transfer with an amount, description/payee, occurrence date/time, and optional note. Income and expenses use the same built-in category list. Transfers require two different accounts.
3. Tap a record to edit it or delete it. Balances recalculate after every change. Tap an account to edit its name or opening balance.
4. Use the date picker, day arrows, **Today**, or a record's date heading to select the day for income, expense, and net totals. The list below always shows all records grouped by occurrence date, newest first. Transfers are excluded from those totals.

V1 has no analysis, budgets, custom categories, category filters, CSV, date-range totals, account history screens, or backup/restore controls. No sample accounts or transactions are inserted at startup.

## Build and install

Open this directory in Android Studio with Android SDK Platform 37 and Build Tools 36.0.0 installed. Use JDK 17 or a compatible newer JDK. The checked-in wrapper downloads Gradle 9.7.1 and verifies its SHA-256; AGP 9.5.1 supplies built-in Kotlin support. Dependencies are pinned. A first build needs internet access to download build tools and libraries.

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

Run connected tests on a disposable emulator or test device: the UI test fixture clears the installed debug app's wallet data. Tests cover exact money parsing, day boundaries, negative/large balances, transfer/edit/delete accounting, validation, simultaneous duplicate account creation, local persistence, occurrence sorting, the complete entry flow, and form recreation. Reports are under `app/build/reports/`. UI tests capture screenshots in the app's external-files `qa/` directory; collect them before uninstalling the test app (Gradle may uninstall it during teardown).

Verified on 11 November 2026: all 6 JVM tests and 9 instrumented tests passed on Android 17 / API 37. Debug lint completed with no errors. The R8 optimized release built successfully and launched with saved data after being signed with the standard debug key for a local smoke test; the distribution release artifact remains unsigned. The minimum supported Android version is checked by lint; device testing was performed on API 37.

## Data and implementation

- Room stores accounts and records in the app-private `wallet.db`; there are no network permissions. Android cloud backup and device transfer are disabled in the manifest and extraction rules.
- Amounts and opening balances use signed 64-bit integer paise. Transactions require positive amounts. Aggregates use `BigInteger` so totals cannot wrap around; formatting uses exact decimal INR amounts.
- A transfer is one database row referencing both accounts. Balances are derived from opening balances and records, so edits/deletions cannot leave a separate balance cache out of sync. Writes and observable ledger snapshots are transactional.
- Occurrence timestamps preserve the entered local date and time, including after a device time-zone change. No opening-balance effective date is modeled.
- Database schema version 1 is checked in under `app/schemas/`. There is no destructive-migration fallback.
- Editors keep drafts across recreation and keep writes in a ViewModel to prevent double saves during rotation.

The implementation does not change the roadmap, agent requirements, or earlier wireframes.
