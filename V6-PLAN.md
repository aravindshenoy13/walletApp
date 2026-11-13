# V6 — Bulk entry and Records filters

## Scope and references

Build on V5.4. Follow the bulk-entry design in [wireframe revision 02](wireframes/wallet-v5.4-wireframe-r02.html), with the entry point shown in [revision 01](wireframes/wallet-v5.4-wireframe-r01.html), and the Records search and filter design in [revision 05](wireframes/wallet-v5.4-wireframe-r05.html). Preserve the existing simple design in light and dark modes. These wireframes illustrate layout; existing transaction rules remain authoritative.

## Bulk add records

- Add a Bulk add records entry to the normal Add record screen, as shown in revision 01. Preserve the existing single-record flow.
- Use revision 02's shared date and inline editable drafts. Each draft has its own time, type, account, amount and description; income/expense have a category, while transfers have a destination account. Allow Uncategorised wherever the current single-record flow does.
- Add another record appends a blank editable draft and focuses its amount. Allow individual drafts to be removed; all remaining drafts stay editable. Each draft has a collapsed calculator using the existing record calculator's arithmetic and money handling, rather than the simplified preview implementation.
- Review validates every draft using the existing record rules, identifies the invalid draft and keeps all entered values. Show each transaction and income, expense and net totals; exclude transfers from these totals. Back returns to editing with drafts intact.
- The final confirmation saves the complete batch atomically, updates balances through the existing transaction model, and prevents repeat submission. A failure saves none and leaves drafts available for retry. A successful save returns to Records and shows the saved records.
- Keep an existing unsaved single-record draft when entering and returning from bulk entry. Prompt before discarding a nonempty bulk draft; changing the shared date applies to all drafts. Preserve drafts across ordinary configuration changes. These are implementation defaults for review.

## Records search and filters

- Move search from the header to the left side of the row beneath “All records · newest first”; place Filter on the right, matching revision 05. Search stays live and matches description/payee or amount. Keep the summary totals independent of search and filtering.
- Open a separate filter screen with category, day, and account controls. Apply commits the selected filters and returns to Records; Back discards un-applied changes. Combine selected filters with search. The account filter includes transfers where the account is either source or destination.
- Show the clear-filter control only when a filter is active; it clears the applied filters while retaining the search text. Show a clear empty state when no records match. Preserve filter and search state through normal navigation and configuration changes.

## Calendar navigation

- Tapping a date in the Analysis monthly calendar opens Records with that exact day applied as a visible, clearable filter. Replace any previously applied day, category, and account filters and clear the search so the selected day's complete set of records is visible. This is a proposed default for review.
- Support days with no records, including future dates displayed by the calendar, and show the normal filtered empty state. The Records today/month summary retains its existing meaning.

## Transfer layout

- Investigate the Add record transfer fields and fix the mismatched box dimensions and alignment. Source and destination controls should match the corresponding account/category controls in size, spacing, and styling, including at larger font scales and in both themes. Preserve transfer behavior.

## Implementation and validation

1. Inspect the existing record editor, calculator, transaction repository, Records search, and calendar navigation; reuse their validation and exact minor-unit arithmetic. Keep dependencies minimal and preserve the existing schema and backup format unless a change is proven necessary.
2. Implement inline batch drafts, review and atomic saving; then implement the Records toolbar, filters, calendar handoff, and transfer layout correction.
3. Verify mixed income, expense and transfer batches, same-category reimbursements, validation failures, cancellation, configuration changes, atomic rollback and duplicate-submit prevention. Check balances and totals against equivalent individually entered records.
4. Verify search combined with each filter, clearing filters, transfer account matching, filter Back/Apply, calendar dates with and without records, and summary independence. Exercise light/dark mode, narrow screens and large text, including transfer field alignment.
5. Run appropriate automated checks, debug and release builds, and lint; perform device or emulator checks where available. Record any checks that could not be performed. Update version metadata and roadmap when implementation is authorized.
