# Wallet V3 implementation

Scope follows [ROADMAP.md](ROADMAP.md), [wireframe revision 03](wireframes/wallet-v3-wireframe-r03.html), and the confirmed decisions in [wireframes/README.md](wireframes/README.md). The later confirmed choices take precedence over illustrative expense-only sketches.

The accepted V2 is frozen at tag `wallet-v2-2026-11-11-r01` (`8cc58c7`) and archived in [backups/wallet-v2-2026-11-11-r01](backups/wallet-v2-2026-11-11-r01/README.md), including source, complete Git history, debug/unsigned release APKs, and checksums. V1 remains separately preserved.

## Implementation

- Records: always-visible compact header search with a magnifying glass and clear action. Case-insensitive description/payee substring or amount matching; comma-formatted amounts are supported. Results remain grouped newest first. Today's totals, all-account balances, and CSV export are independent of search. No category-name search, category filter, or Records date control.
- Analysis: opening the tab resets the period to the first of the current month through today, with All accounts, both graph series, and net category values. One account can be selected. Custom inclusive start/end dates apply to totals, graph, and categories together; reversed ranges are rejected. Rotation retains the active period, account ID, and view modes.
- Daily flow: green income and red expense lines together by default, with independent income/expense graph controls. Zero days remain part of the timeline. Sparse points and neighboring zero days support long ranges without allocating every calendar day. The graph uses normalized coordinates; underlying totals remain exact integer paise/BigInteger.
- Categories: net income minus expense by default, plus income-only and expense-only modes. Transfers and opening balances are excluded. Reimbursements count as income in their category. Zero values in the active view are hidden; archived categories with values remain included. Rows sort by absolute value, then name.
- Accounts: row opens dated history and current balance; Edit is a separate row button. History includes both transfer directions with signed blue amounts, and a separate undated opening-balance row. Records can be edited from history, with live balance refresh. No past-date balance screen.
- Native Views/XML, Room schema 2 retained, no new dependencies, application version 3.0/code 3. V4 import, account-specific export, settings, dark mode, and full backup/restore remain excluded.

## Initial V3 validation

Completed on 11 November 2026:

| Check | Result |
| --- | --- |
| JVM | All 18 tests passed, including six new tests for range/account consistency, reimbursements, exact large aggregates, empty/long periods, search, and transfer-inclusive history. |
| Android | 25/26 passed in the full API 37 suite. The remaining legacy account-entry test passed on rerun together with all four V3 flows and the record-draft regression (6/6). The final search test additionally verified rendered RecyclerView children after layout/animations, rotation, no matches, amount matching, and clearing. |
| Build/lint | Debug, Android test APK, and R8/resource-shrunk release built successfully; lint reported 0 errors and 22 warnings. No dependency changes. |
| Installed upgrade | Installed V3 over V2 without clearing data; exact database comparison preserved 2 accounts, 1 record, all 12 categories, and both ID sequences. Room schema remains 2. |
| Optimized release | Temporary debug-key-signed release launched with saved data; Analysis totals/chart and account history opened successfully with no crash output. Distribution release remains unsigned. |
| Visuals | Reviewed fully filtered Records, Analysis graph/breakdown, and account history/opening balance against the approved simple layouts. |
| Archives | V2 tag still points to `8cc58c7324f579c3cc028dfa875aa97ea9e4bd41`; all 6 archive-file checksums match. V1 and V2 APKs remain intact. |

Device tests used API 37 with system night mode enabled and the app remaining light. API 26 compatibility was checked by lint, not a separate device run. Local logs/screenshots are in ignored `repository/.tools/v3-qa/`. Final debug and unsigned release APKs are `repository/dist/wallet-v3-debug.apk` and `wallet-v3-release-unsigned.apk`, with hashes in `wallet-v3-SHA256SUMS.txt`.

## V3 refinements

- Replaced the outlined/underlined Records search field with a rounded filled field and an icon clear action.
- Made description/payee optional in both the form and repository, retaining its 120-character limit. Blank descriptions are stored as empty strings and export as empty CSV columns. Record metadata omits the trailing separator when the description is blank.
- Net amounts in Records and Analysis are green when positive, red when negative, and neutral at zero.
- Restyled Custom range with a separate padded body, rounded date cards with calendar icons, and a filled Apply action. Root system-bar insets no longer replace the content padding.
- Account balances occupy a padded right-hand section with Edit beneath them; names and opening balances remain on the left.
- Category-wise transaction history was explicitly deferred by the user pending revised requirements. Analysis category rows remain non-interactive; no drilldown screen was added.

Follow-up validation on 11 November 2026: all 19 JVM and 28 Android tests passed. Tests include saving/editing blank descriptions for all transaction types, blank-description CSV output, form saving without a description, and net colors in forced light mode while the device uses dark mode. Debug/test and optimized release builds passed; lint reported 0 errors and 25 warnings. Screenshots of search, date range, negative net, and account rows were visually reviewed. The refreshed optimized release opened Analysis and account history with saved data and no crash output. The V3 APKs and checksums in `repository/dist/` include these refinements.

## Accepted final V3 archive — 12 November 2026

The final V3 includes the changes approved after the initial validation:

- Records search and custom date controls use the simple square outlined wireframe styling, with a text Clear action.
- Analysis has separate monthly and detailed sections. Monthly totals and the smaller-cell calendar show the same full month across all accounts. Month arrows change them together. Transfers and opening balances are excluded; daily net amounts use compact signed notation. Calendar days are summary-only.
- Detailed analysis has its own account selection and inclusive range, exact compact income/expense/net totals, and a larger graph. One Net/Income/Expenses chooser drives both graph and category values. Net is income minus expenses and supports negative graph values.
- Category rows open dated history within the selected detailed range, account and mode, with transaction editing and an empty state. Rows use their actual content bounds for touch feedback.
- Account balances retain sign colors; the all-account total balance is bold and neutral. Transaction rows have tighter padding and smaller icon backgrounds.
- Rotation preserves monthly and detailed state separately. App version remains 3.0/code 3 and Room schema remains 2.

Final archive checks on 12 November 2026 passed all 22 unit tests, lint (0 errors, 34 warnings), the debug build and the R8-optimized unsigned release build. The later calendar/shared-mode changes have not been visually verified on a device, and their updated Android test flows have been compiled but not run. Earlier device results above apply to the earlier V3 builds only.

Archive location: `repository/archives/wallet-v3-2026-11-12-r01/`. Tag: `wallet-v3-2026-11-12-r01`. V4 draft work is excluded from this V3 source snapshot. Credentials, caches, signing keys and personal wallet data are excluded.
