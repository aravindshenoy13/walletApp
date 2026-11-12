# Logical bug review

## Resolution update — 12 November 2026

The findings have been addressed in the current source. Original review text is retained below as historical evidence.

| Finding | Resolution |
| --- | --- |
| 1 | Export selections use SavedStateHandle and synchronous Activity restoration before result delivery. Uninitialized/empty account scopes cannot start export or silently become all accounts. |
| 2 | Backup creation checks encoded UTF-8 byte size against the same 20 MiB limit as restore before opening the output stream. Oversized backups fail clearly instead of reporting success. |
| 3 | Already fixed by e2b414b: BackupViewModel retains work and confirmation/results through Activity recreation. |
| 4 | Default Analysis dates refresh on resume, midnight, and date broadcasts. Custom ranges and manually navigated months remain unchanged. |
| 5 | Export catches loading failures, stays disabled, and offers Retry. |
| 6 | Reads retry twice with bounded delays, then show a persistent failure with tap-to-retry starting a fresh subscription. |
| 7 | Validation explicitly rejects fractional-second backup timestamps before any database replacement, matching whole-second storage. |
| 8 | Filtered results stay unchanged until the gesture and pending row click finish dispatching. |

Validation: 25 JVM tests and all 61 Android cases passed across the regression run and isolated reruns. Additional checks passed at 320 dp width and 200% text in both themes; screenshots were inspected for search, Records totals, calendar, and graph layout. Debug/test APK builds passed. Lint: zero errors, 54 warnings.

Validation limits: export recovery was tested with restored SavedStateHandle parameters before account loading, rather than an OS-kill test inside the document picker. Accessibility descriptions and day-navigation actions were exercised programmatically; a manual TalkBack session was not run.

## Original source review


Reviewed: 12 November 2026.

Scope: current Android implementation in `repository/app/src/main`, checked against `ROADMAP.md` and the applicable version plans. This is a source review, not a guarantee that every bug has been found. No app changes were made. Tests and emulator reproduction were not run for this review.

P1 = high impact; P2 = normal priority; P3 = lower impact or an edge case. References below are relative to the project root.

## 1. [P1] CSV export can lose its selected accounts after process death

- **Code:** `repository/app/src/main/java/com/aravind/wallet/ui/ExportActivity.kt:61`, `:95`, `:105–108`.
- **Trigger:** Select a subset of accounts, open the document picker, let Android kill the app process, then finish choosing the output file.
- **Cause:** On recreation, the selected account IDs are restored only after a suspending database snapshot. The pending activity result can arrive before that completes. `selected()` immediately copies the still-null `accountIds`; a null scope means all accounts to `CsvExporter`. Assigning the restored IDs later cannot update the captured scope.
- **Impact:** The CSV can contain transactions for accounts the user excluded.
- **Fix:** Restore saved export parameters synchronously before result delivery, and make result handling wait for initialization when needed. Preserve the distinction between an uninitialized scope and an intentional all-accounts scope.
- **Validation needed:** Device test with process death while the picker is open and a deliberately delayed initial snapshot.

## 2. [P1] Backup creation can report success for a file the app cannot restore

- **Code:** `repository/app/src/main/java/com/aravind/wallet/ui/SettingsActivity.kt:20–26`, `:37–38`.
- **Trigger:** Back up a wallet whose encoded JSON exceeds 20 MiB.
- **Cause:** Backup creation writes without a size check, but restore unconditionally rejects files larger than 20 MiB.
- **Impact:** The user receives “Full backup saved” for an unusable recovery file. There is no corresponding wallet-size limit preventing this state.
- **Fix:** Make creation and restoration limits consistent. Prefer a format/read strategy that can restore every supported wallet; at minimum, detect and clearly reject an oversized backup before reporting success.
- **Scope note:** The V4 plan explicitly documents the restore limit. The defect is the incompatible successful backup path, not the existence of that limit alone.

## 3. [P2] Rotating Settings during backup can cancel the operation and delete its output

- **Code:** `repository/app/src/main/java/com/aravind/wallet/ui/SettingsActivity.kt:20–30`, `:80–85`.
- **Trigger:** Start a sufficiently large backup and rotate the device or change the theme/configuration while it is writing.
- **Cause:** Work runs in the Activity's `lifecycleScope`, which is cancelled when the Activity is destroyed. The backup catch block also catches cancellation and attempts to delete the chosen document. `busy` and operation state exist only in the old Activity.
- **Impact:** A routine configuration change can abandon a backup and remove its file; the recreated screen has no retained outcome to display. If deletion fails, a partial file may remain.
- **Fix:** Retain backup work and its state in a ViewModel, as the CSV export already does. Distinguish user cancellation from configuration recreation.

## 4. [P2] Analysis defaults become stale across midnight and when returning the next day

- **Code:** `repository/app/src/main/java/com/aravind/wallet/ui/AnalysisPanel.kt:20–24`, `:48–61`; `repository/app/src/main/java/com/aravind/wallet/ui/MainActivity.kt:36–38`, `:118–125`, `:164`.
- **Trigger:** Leave Analysis open across midnight, or background it and return the next day without tapping the Analysis tab again.
- **Cause:** The default detailed range and calendar month are captured at construction or `open()`. MainActivity's midnight timer, date broadcasts, and resume handler only call MainActivity's `render()`; they do not update or render the analysis panel. A later ledger emission rerenders Analysis but still uses its old dates.
- **Impact:** Month-to-date analysis omits the new day. At a month boundary, the default calendar and range can remain in the previous month until the tab is explicitly reopened.
- **Fix:** Refresh default dates on date changes/resume and rerender Analysis. Preserve explicitly chosen custom ranges and historical month selections.

## 5. [P2] Failure loading CSV account choices can crash the export screen

- **Code:** `repository/app/src/main/java/com/aravind/wallet/ui/ExportActivity.kt:105–113`.
- **Trigger:** The initial repository snapshot throws, for example because the database cannot be opened or read.
- **Cause:** Account initialization uses a bare `lifecycleScope.launch` with no error handling. The error escapes the main-thread coroutine rather than being translated into the existing export error state. Other screens handle ledger read failures explicitly.
- **Impact:** Opening Export can terminate the app instead of showing a recoverable error.
- **Fix:** Catch initialization failures, show an error, and keep export disabled until account initialization succeeds. Provide a retry path.

## 6. [P2] A transient ledger read failure leaves subscribed screens permanently failed

- **Code:** `repository/app/src/main/java/com/aravind/wallet/ui/WalletViewModel.kt:36–40`.
- **Trigger:** A snapshot throws while a screen remains subscribed to `ledger`.
- **Cause:** `catch { emit(LedgerState.Failed) }` emits one failure and completes the upstream collection. It does not retry or resume listening to database invalidations. The sharing policy does not restart the upstream while the same subscription remains active.
- **Impact:** Later database changes cannot recover that screen. The UI offers no explicit reload, so recovery depends on subscription teardown/recreation rather than the underlying failure clearing.
- **Fix:** Add an explicit reload/retry mechanism or a bounded retry policy for recoverable failures, while continuing to expose permanent failures clearly.

## 7. [P3] Restore accepts timestamp precision that database persistence silently discards

- **Code:** `repository/app/src/main/java/com/aravind/wallet/data/WalletBackup.kt:35`; `repository/app/src/main/java/com/aravind/wallet/data/Entities.kt:63–65`.
- **Trigger:** Restore an otherwise valid backup containing a timestamp such as `2026-11-12T10:30:00.123456789`.
- **Cause:** Backup decoding accepts `LocalDateTime` with fractional seconds. Room serializes timestamps with a formatter ending at whole seconds.
- **Impact:** Successful restore changes the accepted occurrence time. Transactions distinguished by fractional seconds can change their relative order after persistence. The current editor normally creates minute-precision entries, so ordinary app-generated backups do not trigger this.
- **Fix:** Either preserve fractional precision consistently or reject/normalize unsupported precision explicitly at validation boundaries. Add a database-backed round-trip case with fractional seconds.

## 8. [P2, interaction risk] Tapping a search result clears and replaces the list before the tap is dispatched

- **Code:** `repository/app/src/main/java/com/aravind/wallet/ui/MainActivity.kt:130–147`; `repository/app/src/main/java/com/aravind/wallet/ui/LedgerAdapters.kt:39–53`.
- **Trigger:** Search for a transaction and tap a matching record outside the search box.
- **Cause:** `dispatchTouchEvent()` dismisses search on `ACTION_DOWN` before forwarding the event. Dismissal clears the query and submits the unfiltered list. RecyclerView's asynchronous diff can replace or move rows between `ACTION_DOWN` and `ACTION_UP`.
- **Potential impact:** The intended tap can be cancelled, or interact with a changed row as the filtered results disappear. The exact outcome depends on diff timing and RecyclerView touch handling; opening the wrong editor has not been reproduced.
- **Fix:** Keep result identity stable for the full gesture. Defer list-changing dismissal until the result click has resolved, or consume dismissal separately from record activation.
- **Validation needed:** Device tests tapping and long-pressing filtered results whose positions differ substantially from the unfiltered list.

## Review boundaries

- The transfer balance calculation correctly debits the source, credits the destination, and excludes transfers from income/expense totals in the reviewed code.
- Money calculations use integer minor units and BigInteger aggregates; no floating-point accounting defect was identified.
- Empty-wallet-only restore is an explicit V4 requirement and is not listed as a bug.
- Features deliberately removed or deferred by later roadmap versions are not counted as missing functionality.
- Existing tests were inspected in part, but were not executed. Lifecycle, process-death, and touch timing findings need targeted Android validation before their fixes are accepted.
