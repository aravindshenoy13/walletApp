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
- Add an Analysis tab. Each time it opens, default to the first day of the current month through today and show income, expense, and net totals for that range.
- Add a custom start/end date range. Applying it updates the totals, daily flow graph, and category breakdown together. Keep historical date selection and analysis controls out of Records.
- Show daily income and expense as two colored series together by default, with controls to switch to income-only or expense-only. Use the existing muted pastel green and red semantic colors.
- Show a per-category overview directly below the graph. Its default value is net cash flow per category (income minus expenses). Allow switching the breakdown to income-only or expense-only. Transfers are excluded. Reimbursements remain income in the same category and therefore increase that category's net value. Hide categories with no value in the active view and range.
- Add an Analysis account selector for All accounts or one selected account at a time. The selection applies to totals, graph, and category breakdown.
- Keep the Accounts list and current total balance. Tapping an account opens its dated transaction history and current balance; show an Edit action on the account row. Account history includes transfers in the appropriate direction and the opening balance. Do not add a past-date balance screen.

#### V3 implementation order

1. Add the compact header search and live same-page filtering without changing today's summary.
2. Add the Analysis navigation and month-to-date period state, custom range picker, and account scope.
3. Add period totals, the switchable daily flow graph, and the net/income/expense category breakdown.
4. Change account-row navigation to history, add the separate Edit action, and build the account detail list.

### V4 — Full backup, restore, dark mode, and app-wide icons

- Import transactions from CSV.
- Add account selection to CSV export so users can export transactions for particular accounts.
- CSV column format and import details are intentionally undecided; confirm them with the user before implementing CSV import.
- Back up and restore accounts, opening balances, categories, and transactions.
- Keep this distinct from transaction CSV import/export unless the user chooses to combine them.
- Add dark mode.
- Add a Settings button next to the "Wallet" title for dark mode, CSV export, CSV import, and other options to be decided later.
- Add icons throughout the app, including navigation, actions, accounts, and transaction types, building on V2's category icons.
- Add more icons to the category icon selection.

## Business rules

- Transaction direction and category are separate concepts: income and expense entries can share a category.
- Transfers are not income or expense and must affect the source and destination account balances consistently.
- Use exact decimal-safe money storage (for example, integer minor units) rather than binary floating-point for amounts and balance calculations.
- Preserve the user's statement-oriented intent: record the money movements that occurred, rather than introducing budgets or assumptions about intended spending.

## Scope reminders

- CSV export is for V2. CSV import handling (format, duplicate detection, row validation) is for V4 and requires later product decisions.
- Full backup/restore is V4; exporting transactions as CSV alone is not a full backup.
- Budgets and app lock are not in the roadmap.
