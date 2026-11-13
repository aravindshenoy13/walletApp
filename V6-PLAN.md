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


## Implementation notes

- Version 6.0 (17). Room schema 3 and backup format 2 remain unchanged. No new dependencies.
- Outgoing V5.4 source, Git history bundle, APKs and checksums: `backups/wallet-v5.4-2026-11-13-r02/`. ZIP integrity and bundle verification passed; credentials and build caches are excluded.
- Bulk rows reuse the existing form controls and exact AmountCalculator/Money arithmetic. A retained ViewModel guards saving; the repository wraps existing per-record validation in one Room transaction, annotating validation errors with the row number.
- Review keeps editable views intact; configuration recreation restores row values. The single-record editor stays underneath bulk entry. Successful confirmation returns to an unfiltered Records screen.
- Records search is always visible under the list heading. Day/category/account filters are combined with it; clearing filters retains search. Filter Back cancels edits and Apply commits them. Transfers match either account endpoint and are excluded from category filters.
- Calendar navigation replaces all filters and clears search. Today's/monthly summary still uses the full ledger. Transfer alignment updates layout parameters only when needed, avoiding repeated layout requests from hidden controls.

## Verification

- 29 JVM tests passed, including combined filters, Uncategorised handling, transfer endpoints and empty dates.
- 12 targeted emulator tests passed: repository balance/validation tests plus bulk validation, draft recreation, review/back, duplicate confirmation and calendar/search clearing. An obsolete pre-V5.2 test rejecting null categories now checks a nonexistent category instead.
- Both V6 UI tests passed at 320 dp width, 130% text and dark mode; captured bulk/Records screens were visually inspected. Emulator size and font settings were restored afterward.
- Emulator checks used a separate application ID through an untracked local Gradle init script because the existing emulator app has a different signing certificate. The installed original wallet and its data were retained. Delivery builds use the normal `com.aravind.wallet` ID.
- Debug, R8 release and lint checks are recorded in `repository/.tools/v6-delivery.txt`. Release APK is unsigned and requires signing for installation. APKs/checksums are in `repository/dist/`.
- Validation is targeted rather than a full legacy UI suite. Not every draft/control combination was visually checked in both themes, and release APK device execution was not performed.
