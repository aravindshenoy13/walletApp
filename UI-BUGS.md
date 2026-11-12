# WalletApp UI bug review

## Resolution update — 12 November 2026

The findings have been addressed in the current source. Original review text is retained below as historical evidence.

| Finding | Resolution |
| --- | --- |
| UI-01 | Category editor hides origin labels and shows only Archived when relevant. |
| UI-02 | Editor and record rows share device-preference-aware time formatting. |
| UI-03 | Review correction: repository guards already prevent archiving the last active category, and restore rejects backups with no active categories. A defensive empty-category explanation and route to Add category are now also provided. |
| UI-04 | Picker receives the current unsaved category ID and marks the selected row visually and for accessibility. |
| UI-05 | Clearing the wheel restores its explicit no-values accessibility description. |
| UI-06 | Movement beyond touch slop clears the held percentage and releases parent scrolling. |
| UI-07 | Flow graph exposes the active mode total and exact daily values through named Previous day / Next day accessibility actions, including zero days, without allocating an entire long range. |
| UI-08 | Search results remain stable through the full touch gesture; list-changing dismissal is posted after dispatching the completed gesture. Keyboard dismissal also waits for the gesture. |
| UI-09 | Calendar text uses content-based heights and readable scaled amounts instead of fixed-height slots and 7 sp shrinking. |
| UI-10 | Search uses a compact accessible icon when collapsed. Expanded search uses the title space, a fixed-size accessible Clear icon, and a content-height input. |
| UI-11 | Summary and record rows reserve space for both labels and exact values; amounts can wrap. Oversized Records summaries scroll within a bounded area so record actions remain available. Graph axes use fitted compact labels with font-aware vertical spacing. |

Validation: 25 JVM tests and all 61 Android cases passed across the regression run and isolated reruns. Additional checks passed at 320 dp width and 200% text in both themes; screenshots were inspected for search, Records totals, calendar, and graph layout. Debug/test APK builds passed. Lint: zero errors, 54 warnings.

Validation limits: export recovery was tested with restored SavedStateHandle parameters before account loading, rather than an OS-kill test inside the document picker. Accessibility descriptions and day-navigation actions were exercised programmatically; a manual TalkBack session was not run.

## Original source review


Reviewed: 12 November 2026. Baseline: current V4.4 source and ROADMAP.md.

Report only; no application files were changed. This was a source review of the UI activities, adapters, custom charts, layouts, styles, and theme resources. No new emulator session, physical-device inspection, or TalkBack session was run. Findings below distinguish direct code defects from interaction/layout risks requiring runtime confirmation. This is not a guarantee that every UI bug has been found. Previously resolved V4.4 crashes are not repeated here.

Severity: P2 = normal-use problem; P3 = smaller presentation/accessibility problem.

## Findings supported directly by the source

### UI-01 — Category editor still displays Built-in / Custom labels (P3)

- Steps: Open Categories and edit an existing built-in or custom category.
- Actual: The editor sets the origin text to Built-in or Custom and makes it visible for every existing category. Archived categories also include that origin text.
- Expected: Preserve archived-state indication without displaying the removed origin labels, consistent with V4.3.1/V4.3.2 refinements.
- Evidence: `repository/app/src/main/java/com/aravind/wallet/ui/CategoryEditorActivity.kt:99–101`; `repository/app/src/main/res/layout/activity_category_editor.xml` (`origin`). The category list hides these labels, but the editor does not.

### UI-02 — Time fields ignore the device's 12-hour display preference (P3)

- Steps: Set Android to 12-hour time. Open a record occurring at 15:00, then open its time picker.
- Actual: Record rows and the editor show 15:00 while the picker uses 3:00 PM. The display and picker use different time conventions.
- Expected: Use the user's time-format preference consistently.
- Evidence: `RecordEditorActivity.kt:167` and `LedgerAdapters.kt:95` hard-code `HH:mm`; the picker in `RecordEditorActivity.kt` uses `DateFormat.is24HourFormat(this)`. All files are under `repository/app/src/main/java/com/aravind/wallet/ui/`.

### UI-03 — Saving is silently unavailable when all categories are archived (P2)

- Steps: Archive every category, then open Add record with at least one account and select Expense or Income.
- Actual: The category control says Choose category, its picker has no available categories, and Save is disabled. There is no explanation on the form or direct route to create an available category.
- Expected: Explain why the transaction cannot be saved and how to recover.
- Evidence: `RecordEditorActivity.kt`, `refreshCategories()` and `updateEnabled()`; `CategoryPickerActivity.kt` filters archived categories except an edited record's original category. Category management permits archiving all categories.

### UI-04 — Category picker does not identify the current selection (P3)

- Steps: Edit a categorized record and open its category selector.
- Actual: All category rows look the same. The editor passes only recordId; the picker does not receive the current unsaved selected category or mark any item selected.
- Expected: Identify the currently selected category in the single-choice UI, including a category changed earlier in the same unsaved edit.
- Evidence: `RecordEditorActivity.kt` (`pickCategory.launch`), `CategoryPickerActivity.kt`, and `CategoryViews.kt` (`CategoryChoices.row`). None binds a selected state or selection indicator.

### UI-05 — Empty wheel accessibility description becomes blank after clearing data (P3)

- Steps: Display a Category Expense Graph with negative categories, then choose a range with none.
- Actual: The wheel's accessibility description becomes the wheel prefix plus an empty joined list. It loses the explicit no-values description installed by its constructor. A wheel first opened empty has a different description from one that becomes empty.
- Expected: Announce the empty state consistently.
- Evidence: `repository/app/src/main/java/com/aravind/wallet/ui/CategoryWheelView.kt`, `init` and `show()`. `show()` formats `negative.joinToString` even when empty; the separate visible no-values TextView does not fix the wheel's own description.

### UI-06 — Dragging on the wheel prevents scrolling the Analysis page (P2)

- Steps: Open a populated Category Expense Graph and start a vertical swipe on a colored ring segment.
- Actual: ACTION_DOWN immediately disables parent touch interception. Moving off the segment clears only the pressed percentage, not the interception lock, so the surrounding ScrollView cannot take over until the gesture ends.
- Expected: Allow vertical page scrolling after a drag threshold while preserving stationary press-and-hold percentage display.
- Evidence: `repository/app/src/main/java/com/aravind/wallet/ui/CategoryWheelView.kt:72–86`. Interception is released only by `clearPress()` on up/cancel/detach/data changes, not on a scrolling drag.

### UI-07 — Flow graph exposes totals but no daily values to accessibility (P2)

- Steps: Use a screen reader to inspect a Flow Graph containing several different daily values.
- Actual: The graph is one accessibility node describing the range, income total, expense total, and mode. Individual dates and values are only drawn on Canvas; they cannot be focused or read. The Net view's description does not even include the numerical net total.
- Expected: Provide an accessible way to inspect the daily information represented by the graph and the active mode's total.
- Evidence: `repository/app/src/main/java/com/aravind/wallet/ui/DailyFlowView.kt`, `show()` and `onDraw()`; no virtual accessibility nodes or equivalent daily-value list are provided in the Flow Graph view.

## Interaction and layout risks requiring runtime confirmation

### UI-08 — Tapping a filtered record may clear or replace the target before the tap completes (P2)

- Steps: Search so that one older transaction remains, then tap that transaction directly while search is expanded.
- Risk: `MainActivity.dispatchTouchEvent()` dismisses search on ACTION_DOWN anywhere outside the search box, clears the query, and submits the unfiltered list before passing the event onward. Restoring the list and changing the keyboard inset can move or rebind the row between down and up. The tap may fail or act on a different record.
- Expected: Opening a visible filtered record should reliably open that record.
- Evidence: `repository/app/src/main/java/com/aravind/wallet/ui/MainActivity.kt:141–147`, `dismissSearch()`, and `render()`; `LedgerAdapters.kt` binds edit listeners to the current row. Needs runtime reproduction because list-diff and layout timing determine the outcome.

### UI-09 — Calendar day numbers can clip with large system fonts (P3)

- Steps: Increase Android font size substantially and open Daily Breakdown.
- Risk: Day-number text scales in sp but is confined to an 18 dp high TextView. The cell does not grow with the font. Amounts also remain in fixed 22 dp slots and can shrink as low as 7 sp, reducing readability.
- Expected: Keep day numbers and amounts readable without clipping at supported font settings.
- Evidence: `repository/app/src/main/java/com/aravind/wallet/ui/MonthlyCalendar.kt:73–85`. Exact font/device threshold needs visual confirmation.

### UI-10 — Main header search can become too narrow at large font sizes (P3)

- Steps: Use a narrow phone with large system text, expand Records search, and enter a query.
- Risk: The 48 dp search container must fit an icon, a weighted edit field, and a wrap-content Clear button. Clear's text scales while the height remains fixed; the container itself shares the header with Settings and the Wallet title. Text entry width can collapse or text can clip as font size increases.
- Expected: A usable search field and readable Clear action at supported font sizes.
- Evidence: `repository/app/src/main/res/layout/activity_main.xml` (`search_box`, `record_search`, `clear_search`). Verify beyond the previously documented 130% font check.

### UI-11 — Large monetary values can obscure labels and chart axes (P3)

- Steps: Enter a valid large amount near the supported money limit; inspect Records totals, Analysis totals, and Flow Graph on a narrow screen with larger text.
- Risk: Summary values use unrestricted wrap-content width beside labels weighted into the remaining space. Long values can consume the entire row. Graph axis amounts are drawn without fitting, wrapping, or clipping to a reserved label region, so they can extend beyond the view. Record-row amounts have a width cap but no explicit single-line fit policy and may wrap awkwardly.
- Expected: Keep the monetary value and its label readable within the viewport.
- Evidence: `repository/app/src/main/res/values/styles.xml` (`SummaryLabel`, `SummaryValue`), `repository/app/src/main/res/layout/item_record.xml`, and `repository/app/src/main/java/com/aravind/wallet/ui/DailyFlowView.kt` (`canvas.drawText` for axis amounts). Verify concrete clipping at supported amounts and screen sizes.

## Suggested verification order

1. Filtered-record tap behavior, wheel scrolling, and the all-categories-archived form.
2. Category selection/origin labels and 12-hour time consistency.
3. Calendar/search/money layout at narrow widths and 150–200% fonts, in both themes.
4. TalkBack review of both charts and the category picker.

No fixes, version changes, commits, or chat archival were performed for this review.
