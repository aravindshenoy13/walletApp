# WalletApp version roadmap

### V1 — Core transaction tracking

- Add multiple accounts. All accounts have the same behavior; do not model special account types for cash, banks, or cards.
- Each account has a name and an opening balance. The opening balance is the starting point when tracking that account in the app; no separate historical effective date is required.
- Accounts may have negative balances.
- Manually create, edit, and delete transactions.
- Every transaction records its occurrence date and time.
- Support income, expense, and transfer transaction types.
- Income and expense transactions have a category. A category can be used for either income or expense; it is not restricted to one direction.
- Include an optional transaction description/payee. A separate note field is not included.
- A transfer moves value between two accounts and is excluded from income and expense totals. A card payment is represented as a transfer between accounts.
- Transactions change the relevant account balances.
- Show transactions grouped by date.
- On the Records tab, always show income, expense, and net totals for the current day. Do not provide date navigation or date selection on this tab.
- Store data locally on the device.
- V1 has a built-in category set. Users cannot add categories in V1.
- Do not include analysis/overview, budgets, or account-specific transaction/history views in V1.

### V2 — Categories and CSV export

- Add, edit, and manage custom categories.
- Allow renaming, changing icons, and archiving all categories, including the built-in categories.
- Add category icons alongside category management.
- Each category icon can use any of 16 background colors: 8 normal (dark) colors and 8 pastel colors. Choose the icon and its background color independently.
- Present all 16 colors in one selector labeled "Background color", without separate color-group headings.
- Use simple line-art category icons. In light mode the line art is black; when dark mode is added in V4, the line art is white. V2 assumes light mode.
- Export transactions as CSV.

### V3 — Analysis, search, and account history

- Keep a compact Records search field visible in the top-right header with a magnifying-glass icon. Filter the transaction list on the same page as the user types; match description/payee or amount only. Include a clear action. Do not search category names or add a category filter. Today's summary totals remain independent of search results.
- Add an Analysis tab with two separate sections. Monthly overview shows income, expense, and net totals plus a daily net calendar for the displayed full month across all accounts. Month arrows change both together; default to the current month on opening. Calendar cells are summary-only, with compact signed amounts, and exclude transfers.
- Detailed analysis defaults to the first day of the current month through today on opening. Its account selector and custom range control the compact range totals, graph and category breakdown below the calendar.
- Add a custom start/end date range. Applying it updates range totals, daily flow graph and category breakdown together, independently of the monthly overview. Keep historical date selection and analysis controls out of Records.
- Use one Net/Income/Expenses chooser for the detailed graph and category breakdown, defaulting to Net. Plot daily net (income minus expenses) with positive portions green and negative portions red; income-only is green and expense-only is red. Use the existing muted semantic colors.
- Show a per-category overview directly below the graph. Its default value is net cash flow per category (income minus expenses). Allow switching the breakdown to income-only or expense-only. Transfers are excluded. Reimbursements remain income in the same category and therefore increase that category's net value. Hide categories with no value in the active view and range.
- Add an Analysis account selector for All accounts or one selected account at a time. The selection applies only to detailed range totals, graph and category breakdown; monthly totals and calendar always include all accounts.
- Keep the Accounts list and current total balance. Tapping an account opens its dated transaction history and current balance; show an Edit action on the account row. Account history includes transfers in the appropriate direction and the opening balance. Do not add a past-date balance screen.

#### V3 implementation order

1. Add the compact header search and live same-page filtering without changing today's summary.
2. Add the Analysis navigation and month-to-date period state, custom range picker, and account scope.
3. Add period totals, the switchable daily flow graph, and the net/income/expense category breakdown.
4. Change account-row navigation to history, add the separate Edit action, and build the account detail list.

### V4 — Full backup, restore, dark mode, and app-wide icons

- Add account selection to CSV export so users can export transactions for particular accounts.
- Back up and restore accounts, opening balances, categories, and transactions.
- Keep this distinct from transaction CSV export.
- Add dark mode.
- Place the Settings button at the far left of the header, immediately left of the "Wallet" title and vertically aligned with it. Settings contains dark mode, CSV export, backup, restore, and other options to be decided later.
- Add icons throughout the app, including navigation, actions, accounts, and transaction types, building on V2's category icons.
- Add more icons to the category icon selection.
- Keep the Records search compact at the top right until tapped, then expand it for text entry. Continue filtering descriptions/payees and amounts as the user types.
- Keep icons on the bottom navigation, and use simple line-art icons throughout the app.
- In the category editor, add an "Additional Icons" button opening a separate page with the additional icons arranged in a borderless row-and-column grid. Preserve independent icon and 16-color background selection.
- Use the square checkbox style for selection controls throughout the app, replacing radio-button appearances. Preserve each control's selection rules: mutually exclusive choices still allow only one selection, while multi-account CSV export allows multiple accounts.

### V4.1 — Analysis views and category wheel

- Add a Net-only wheel chart showing only categories with negative net cash flow. Size slices by the absolute negative net amounts; exclude zero and positive categories.
- Add a drop-down in the Analysis tab to switch between Calendar, Category Breakdown (wheel chart), and Flow Graph views.

### V4.2 — Visual refinements and account icons

- Replace default checkbox/tick appearances with clear wireframe-style selection states while preserving single- and multi-select behavior.
- Fit and align transaction-type icons; replace the Settings gear with a three-line menu icon; remove the duplicate plus on Add category.
- Improve category icon and wheel-chart contrast in dark mode and restyle the Settings screen to match the app.
- Add thin, theme-aware outlines to dropdowns and remove redundant explanatory text.
- Collapse Records search when it is dismissed and return it to its normal state.
- Show positive and zero account balances in the normal text color and negative balances in red.
- Add selectable bank, cash, and card account icons as visual labels, with persistent storage and backward-compatible backup/restore.
- See [V4.2-PLAN.md](V4.2-PLAN.md) for the detailed scope, implementation order, assumptions, and validation.

### V4.3 — Records and analysis refinements

- Improve search spacing, Records day headings, duplicate add icons, account-row layout, and consistent buttons and top bars.
- Let the Records summary toggle between Today and the current month. Keep opening balance visible only in account editing.
- Rename the Analysis views to Daily Breakdown, Category Expense Graph, and Flow Graph; remove the Detailed analysis heading, account filtering for the graph and wheel, and the category list beneath Flow Graph.
- Show the active range before a more prominent Custom range button, box Analysis summary groups, and improve calendar borders in both themes.
- Add lakh (L) compact formatting, clearer wheel-sector outlines, and press-and-hold percentage display.
- Remove visible labels beneath category icon choices and add an icon for the Dark mode setting.
- See [V4.3-PLAN.md](V4.3-PLAN.md) for the detailed scope, implementation order, assumptions, and validation.

### V4.3.1 — Visual refinements

- Group Analysis totals in one themed box and match Daily Breakdown summary sizing to the graph views.
- Remove the Records date-heading box, restore thinner calendar borders, brighten dark-mode category colors, and simplify the Today/month toggle.
- Use unfilled red Delete, green Save, and blue Add actions; use light-mode transfer colors in both themes.
- Remove the visible Built-in category label and make category rows more compact.
- See [V4.3.1-PLAN.md](V4.3.1-PLAN.md) for the implementation details.

### V4.3.2 — Button and color refinements

- Fix action-button outline edges, dimensions, and alignment while retaining the Add/Save/Delete color scheme.
- Brighten pastel category backgrounds while keeping icons readable and saved color choices intact.
- Restore the intended earlier dark-mode transfer colors.
- Remove the visible Custom label from category rows while retaining category data and archived-state indication.
- See [V4.3.2-PLAN.md](V4.3.2-PLAN.md) for implementation details and validation.

### V4.4 — Stability and design consistency

- Fix the reported Add record and Categories crashes.
- Fix the Records screen remaining brighter after searching with no records until Search is tapped again.
- Make the bottom navigation icons consistent in visible size and alignment.
- Review the app for controls and screens that miss the established design language and bring them into line.
- See [V4.4-PLAN.md](V4.4-PLAN.md) for the implementation order and validation.

### V5 — State, UI, and logical bug fixes

- Release the accepted state-machine, UI, and logical fixes from this chat as V5 (version name 5.0, version code 11).
- Include the fixes and corrections documented in [UI-BUGS.md](UI-BUGS.md) and [LOGICAL-BUGS.md](LOGICAL-BUGS.md), including search/gesture stability, saved export selection, backup validation, retry, Analysis date refresh, accessibility, and large-text layouts.
- Preserve Room schema 3 and backup format 2, including format-1 restore support.
- Keep the standalone V5 source/history/APK backup in the existing top-level `backups/` folder, as requested for this release.
- See [V5 release notes](V5-PLAN.md) for scope, verification, and backup details.

### V5.1 — UI design consistency

- Implement D01–D26 from [UI-DESIGN-REVIEW.md](UI-DESIGN-REVIEW.md): headers, list gutters, forms, selection spacing, actions, totals, and chart presentation.
- Rename the Analysis choices to Monthly Overview, Category Breakdown, and Flow Graph.
- Keep existing transaction behavior, Room schema 3, and backup format 2 unchanged.
- Version name 5.1, version code 12. See [V5.1-PLAN.md](V5.1-PLAN.md) for decisions and verification.

### V5.2 — Calculator and category management

- App name: Wallet Manager. Calculator and record-form rearrangement preserve existing icons and colors.
- Delete categories with confirmation and retain records as uncategorised; offer an Uncategorised selection.
- Position Add account and Add category at the bottom to match Add record.
- Version 5.2, code 13. See [V5.2 notes](V5.2-PLAN.md).

### V5.3 — Development baseline

- Add category icons, six distinct muted colors, immediate icon selection, an Uncategorised icon, and record keyboard fixes.
- Version 5.3, code 14. See [V5.3 notes](V5.3-PLAN.md).

### V5.3.1 � Save alignment and icon consistency

- Vertically center the record Save action with Back and the title.
- Remove paw and wrench artwork, redraw cash and airplane, and unify line-icon strokes.
- Version 5.3.1, code 15. See [V5.3.1 notes](V5.3.1-PLAN.md).

## Business rules

- Transaction direction and category are separate concepts: income and expense entries can share a category.
- Transfers are not income or expense and must affect the source and destination account balances consistently.
- Use exact decimal-safe money storage (for example, integer minor units) rather than binary floating-point for amounts and balance calculations.
- Preserve the user's statement-oriented intent: record the money movements that occurred, rather than introducing budgets or assumptions about intended spending.

## Scope reminders

- CSV export is supported. CSV import is removed from the product scope.
- Full backup/restore is V4; exporting transactions as CSV alone is not a full backup.
- Budgets and app lock are not in the roadmap.
