# WalletApp version roadmap

### V1 — Core transaction tracking

- Add multiple accounts. All accounts have the same behavior; do not model special account types for cash, banks, or cards.
- Each account has a name and an opening balance. The opening balance is the starting point when tracking that account in the app; no separate historical effective date is required.
- Accounts may have negative balances.
- Manually create, edit, and delete transactions.
- Every transaction records its occurrence date and time.
- Support income, expense, and transfer transaction types.
- Income and expense transactions have a category. A category can be used for either income or expense; it is not restricted to one direction.
- Include a transaction description/payee. A separate note field is not included.
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
- Export transactions as CSV.

### V3 — Analysis and overview

- Filter records by one category at a time.
- Add search at the top right of the Records page. Match transaction description/payee, amount, or category, and filter the records directly on the same page.
- Add day-by-day income and expense flow graphs.
- Add category analysis and a spending overview.
- Allow viewing one account's transactions and balance.
- Show an account balance for a past date.
- Add income, expense, and net totals for a selected date or date range in the Analysis tab.
- Keep historical date selection and analysis controls out of the Records tab.
- Confirm graph modes, account filters, and category total calculations with the user before implementing them.

### V4 — Full backup, restore, dark mode, and app-wide icons

- Import transactions from CSV.
- CSV column format and import details are intentionally undecided; confirm them with the user before implementing CSV import.
- Back up and restore accounts, opening balances, categories, and transactions.
- Keep this distinct from transaction CSV import/export unless the user chooses to combine them.
- Add dark mode.
- Add a Settings button next to the "Wallet" title for dark mode, CSV export, CSV import, and other options to be decided later.
- Add icons throughout the app, including navigation, actions, accounts, and transaction types, building on V2's category icons.

## Business rules

- Transaction direction and category are separate concepts: income and expense entries can share a category.
- Transfers are not income or expense and must affect the source and destination account balances consistently.
- Use exact decimal-safe money storage (for example, integer minor units) rather than binary floating-point for amounts and balance calculations.
- Preserve the user's statement-oriented intent: record the money movements that occurred, rather than introducing budgets or assumptions about intended spending.

## Scope reminders

- CSV export is for V2. CSV import handling (format, duplicate detection, row validation) is for V4 and requires later product decisions.
- Full backup/restore is V4; exporting transactions as CSV alone is not a full backup.
- Budgets and app lock are not in the roadmap.
