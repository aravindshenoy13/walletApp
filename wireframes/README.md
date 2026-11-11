# WalletApp wireframes

## V4.1 confirmed implementation decision

- The user chose a Net-only wheel containing negative-net categories only. Slices use absolute negative values; zero and positive categories are omitted. This supersedes the all-category Expenses example and unresolved mixed-sign proposal in revision 01. Flow Graph keeps Net/Income/Expenses.

## V4.1 revision 01

- `wallet-v4.1-wireframe-r01.html` shows the Calendar, Category Breakdown, and Flow Graph views and an open view selector. These are static sketches.
- Calendar retains the V3 full-month, all-account summary and daily net calendar. The detailed views illustrate the V3 range, account selector, totals, and Net/Income/Expenses control; their range remains independent of the calendar month.
- The wheel example uses Expenses, with nonnegative category slices. All categories appear in the accompanying list, including a zero-value example, while zero has no visible slice. Exact treatment of zero and mixed positive/negative Net values in the wheel is a proposal that needs a product decision before implementation.
- The Flow Graph panel is a structural sketch illustrating V3's green/red net rendering; transfer exclusion remains in the roadmap. Amounts and dates are illustrative.
- No Android implementation or changes to AGENTS.md or ROADMAP.md are included.

## V4 revision 02

- `wallet-v4-wireframe-r02.html` places Settings left of Wallet with aligned centers, shows compact and expanded top-right search, retains bottom navigation icons in light and dark examples, and adds an Additional Icons page with a borderless grid.
- Square checkbox appearances replace radio-button appearances throughout V4. Single-choice groups remain mutually exclusive; the CSV export account list supports multiple selections. Account selection and category icon selection illustrate single-choice behavior.
- The additional icon names shown are examples, not a final inventory. Icon/color selection remains independent and the existing 16 colors remain in one selector.
- The sketches are static; the compact search expands for input in the intended app. CSV import and restore behavior still need product decisions before implementation.

## V4 revision 01

- `wallet-v4-wireframe-r01.html` sketches V4 settings, selected-account CSV export, CSV import, full backup and restore entry points, dark mode, app-wide icons, and more category icon examples.
- These are static sketches using sample data. CSV import columns, validation, mapping, and duplicate handling remain undecided as specified in the roadmap. The icon examples and restore review step are proposals, not finalized behavior.
- Backup includes accounts, opening balances, categories, and transactions and is distinct from CSV transaction export. Restore conflict/replacement behavior is deliberately undecided and needs product decisions before implementation.
- Records and Analysis retain the current V3 roadmap behavior. The displayed icon line art is black in light mode and white in dark mode. Category icon choices and their 16 independently selectable background colors carry forward from V2.
- No Android implementation or changes to AGENTS.md or ROADMAP.md are included.

Latest V3 draft: `wallet-v3-wireframe-r03.html`. Revisions 02 and 03 supersede the V3 revision 01 proposals below.

## V3 revision 03

- Moves the Records search bar to the top-right header, beside the Wallet title, with a magnifying-glass icon. Removes the search label and field from the body. The displayed query is sample text in a static wireframe.
- Preserves revision 02 and its remaining screen layouts. ROADMAP.md now explicitly requires the top-right header placement.

## V3 revision 02

- Preserves revision 01 and updates ROADMAP.md to reflect the user's revised V3 scope. AGENTS.md and the Android application are unchanged.
- Records has a single search bar: typed letters/numbers match description/payee or amount. Category-name search and category filtering are removed. Today's header totals remain independent of the search results.
- Analysis defaults to the first of the current month through today every time it is opened. The sample uses 1–11 November 2026.
- A "Custom range" button opens start/end date selection. Applying the range updates the totals, daily flow graph, and per-category expense list together.
- Category expenses appear directly below the graph on the Analysis screen, rather than on a separate screen. Categories with no spending in the displayed period/account scope are omitted.
- The account list and per-account current balance/history remain. Historical account balances and the past-date screen are removed.
- These five panels are static layout references. Follow the confirmed V3 behavior below and in [ROADMAP.md](../ROADMAP.md) where it is more specific than the sketches.

## V3 revision 01

- Six static light-mode screen sketches: Records search/category filtering, Analysis totals and daily flow, category spending overview, Accounts, account history, and historical balance.
- Records search is at the top right and expands on the same page. The displayed query matches description/payee, amount, or category; only one category may be selected. There are no historical date controls on Records.
- Proposed Records behavior: search and category selection combine; the header retains all of today's income, expense, and net totals independently of the filtered list.
- Analysis supports a single date or inclusive date range. Proposed graph mode shows separate daily income and expense lines; the illustrated range is 9–11 November 2026. The drawing and values are sample content, not app functionality.
- Proposed Analysis account selector supports all accounts or one account. Proposed category percentages divide each category's expenses by total expenses in the selected period/account scope, with a separate income view. Transfers are excluded from these totals.
- Account history includes incoming/outgoing transfers. Proposed past balance uses the end of the selected local day, including opening balance and all movements through that day.
- Graph modes, account filtering, category calculations, and historical cutoff semantics are design proposals to confirm before implementation. The roadmap and AGENTS.md are unchanged.
- Retains V2's category icon/background styling and its independent 16-color selector in the existing category editor; these panels focus on V3 additions. No V4 settings, import, backup, dark mode, or expanded icon set is added.

Name wireframes `wallet-v<app-version>-wireframe-r<revision>.html`.
The app version identifies product scope; the revision identifies a design iteration within that version. Use at least two digits for revisions, increment them for each saved iteration, and preserve prior revisions. For example, `wallet-v1-wireframe-r03.html` follows `wallet-v1-wireframe-r02.html`; V2 begins at `wallet-v2-wireframe-r01.html`.

- `wallet-v1-wireframe-r01.html`: original visual draft.
- `wallet-v1-wireframe-r02.html`: adds occurrence time to the record form and transaction rows.
- `wallet-v1.html`: original legacy filename, preserved for existing chat references.
- `wallet-v2-wireframe-r01.html`: V2 draft based on the updated roadmap: Records with today's totals and CSV export entry, a shared category list, adding custom categories, renaming and changing icons on built-in/custom categories, archiving confirmation, and CSV export.
- `wallet-v2-wireframe-r02.html`: light-mode revision with independent icon/background selection, 8 normal (dark) colors and 8 pastel colors, and black line-art icons. The Add and Edit Category icon/color controls are interactive previews.
- `wallet-v2-wireframe-r03.html`: combines all 16 background colors into one selector in each category editor, removing the separate color-group headings. Preserves independent icon/color selection and black line art.

These drafts establish the simple lines-and-text appearance. They are not a feature specification: the initial draft includes analysis, budgets, category management, and monthly totals that are outside the V1 scope in [ROADMAP.md](../ROADMAP.md). Their presence does not authorize implementing them. Follow [ROADMAP.md](../ROADMAP.md) when implementation is requested.

## V3 decisions confirmed after revision 03

- Keep the Records search compact field visible in the top-right header. It matches description/payee or amount, filters the same-page list, and does not change today's summary.
- Show income and expense graph lines together by default, with income-only and expense-only modes and distinct muted pastel colors.
- Show per-category net values (income minus expenses) by default, with income-only and expense-only views. Reimbursements remain income in the same category.
- The Analysis account selector supports All accounts or one account at a time.
- Tapping an account opens its history and current balance; keep account editing as a separate action on the account row.

## V2 revision 01

- Static screen sketches with sample values; the Android application is not changed.
- V2 adds category management and CSV export to the V1 baseline. These six panels focus on those changes; existing account and transaction forms remain part of the V1 baseline.
- Categories share one list for income and expenses. Built-in and custom categories support the same editing and archiving actions. Icon choices are illustrative monochrome placeholders for the category icon set.
- Proposed archive behavior: hide the category from new-transaction choices while preserving it on existing transactions. The confirmation shows this design assumption.
- Proposed export flow: export all transactions, including transfers, and choose the destination with the Android system file picker. CSV columns and serialization are not specified by this wireframe.
- No category filtering, historical date controls, analysis, budgets, CSV import, full backup, or app-wide icons are added. Export is a text action; only categories receive icons.

## V2 revision 02

- Preserves revision 01. Uses a fixed light-mode preview and black line art, with color applied to icon backgrounds rather than strokes or text.
- Each of the six example icons can be paired with all 16 colors. Icon and color selection are independent; changing one preserves the other. Add and Edit Category have separate local preview choices.
- Includes 8 normal (dark) colors and 8 matching pastel colors: red, orange, yellow, green, teal, blue, purple, and pink. Normal shades retain contrast with black line art. The initial selections and category-list examples use pastels.
- Only the icon/color selectors are interactive; saving categories, archiving, and exporting remain wireframe sketches.
- ROADMAP.md records the palette requirement in V2, white line art for V4 dark mode, and an expanded category icon selection in V4.

