# WalletApp design consistency review

Date: 12 November 2026.

Scope: aesthetic consistency, padding, spacing, alignment, typography, icons, and control treatment in the current working-tree UI. Recommendations only: no application code was changed. This is separate from `UI-BUGS.md`.

Method: source review of all screen layouts, shared styles, row layouts, and programmatically built screens and charts against ROADMAP.md. These are source-backed inconsistencies and design recommendations, not screenshot-verified defects. Exact visual impact should be checked on a device before implementation, in both themes and with larger text. Current working-tree changes were considered; earlier fixed search dimensions and monetary layout issues are not repeated here.

## Preserve the existing direction

Keep the simple native layout, themed paper/ink surfaces, thin square outlines, line icons, compact lists, and blue Add / green Save / red Delete actions. Avoid extra cards, shadows, rounded containers, or decorative elements. Padding should improve grouping and alignment without making every screen taller.

Suggested spacing baseline for review: 16 dp page gutters; 8 dp within related groups; 16 dp between groups; 4 dp for tight icon/text or label/field relationships; at least 48 dp interactive targets. These are proposed conventions, not existing product requirements. Purposeful exceptions, including compact category rows and borderless Additional Icons choices, should remain.

## Headers and page edges

### D01 — Settings header and divider are inset differently

- Location: `repository/app/src/main/java/com/aravind/wallet/ui/SettingsActivity.kt`, `onCreate()`.
- Observation: Header and divider live inside a container padded 16 dp horizontally and 8 dp at the top. Other secondary screens use an 8 dp header gutter and a divider spanning the screen.
- Recommendation: Match the shared secondary header geometry and full-width divider; keep the Settings controls inside their 16 dp content gutter.

### D02 — Settings header scrolls away

- Location: `SettingsActivity.kt`, header inside the root ScrollView; compare `AdditionalIconsActivity.kt` and XML editor screens.
- Observation: Settings scrolls its Back/title/divider with its options, whereas the other secondary screens keep these above the scrolling content.
- Recommendation: Keep the header outside the scrolling options for consistent screen structure, especially at larger font sizes.

### D03 — Back icon/text spacing differs on generated screens

- Location: `SettingsActivity.kt`, `AdditionalIconsActivity.kt`; compare `activity_account_editor.xml`, `activity_record_editor.xml`, `activity_category_editor.xml`, `activity_export.xml`, `activity_date_range.xml`, `activity_category_picker.xml`, and both history layouts.
- Observation: Generated Back buttons inherit 8 dp drawable spacing; XML Back buttons explicitly use 4 dp.
- Recommendation: Use the same 4 dp gap and button insets on every secondary header.

## Lists and rows

### D04 — Date headings retain an extra horizontal inset

- Location: `repository/app/src/main/res/layout/item_date.xml`; Records and history list containers.
- Observation: Date headings add 12 dp horizontal padding inside lists already inset 16 dp. Row badges and metadata start at the list edge, so headings start on a separate vertical line left over from their former boxed treatment.
- Recommendation: Align date text to the list gutter, or intentionally to the row title column. Use one consistent alignment across Records and both history screens.

### D05 — First date heading creates an oversized opening gap

- Location: `item_date.xml`; `activity_main.xml` (`record_count`); both history layouts.
- Observation: Every date heading has a 12 dp top margin and a minimum 48 dp height with vertical padding. In Records this follows an All records label already padded 12 dp above and below; the first heading receives the same separation as later groups.
- Recommendation: Reduce only the first group's leading gap; retain clearer separation between later dates. Do not reduce text readability to compress the heading.

### D06 — Record metadata starts on a different text column

- Location: `repository/app/src/main/res/layout/item_record.xml`.
- Observation: Title text starts after a 28 dp badge plus 8 dp gap; time/account/payee metadata starts at the far left beneath the badge.
- Recommendation: Consider a 36 dp start inset on metadata so transaction text forms one column. Verify long account/payee strings before adopting it; preserve the compact row height.

### D07 — Category Edit is styled differently from account Edit

- Location: `item_category.xml` versus `item_account.xml`.
- Observation: Categories use an unpadded Edit TextView at the row's trailing edge; Accounts use an Edit button with icon, horizontal padding, and an explicit touch target.
- Recommendation: Give both a consistent trailing action treatment and inset. Keep the category row itself tappable and compact; avoid adding a second independent action unless required.

### D08 — Category picker rows have a different surface and inset

- Location: `activity_category_picker.xml`, `item_category_choice.xml`; compare `item_category.xml`.
- Observation: Picker rows use outlined selection surfaces and another 8 dp inset inside a list with 16 dp gutters, while management rows are plain, separated rows. Each picker entry appears as a touching outlined block with no explicit inter-row gap.
- Recommendation: Use the same plain row rhythm, with the chosen item receiving the established selection treatment. If outlines are retained on every option, add a small deliberate gap so neighboring borders do not visually merge. Confirm the intended choice treatment before changing it.

### D09 — History lists have no final content breathing room

- Location: `activity_account_history.xml`, `activity_category_history.xml` (`history_list`).
- Observation: Both lists have horizontal padding and `clipToPadding=false`, but no bottom padding. Records has 16 dp bottom padding.
- Recommendation: Add matching bottom content padding so the last separator/record does not finish against the system inset boundary.

## Forms and selection controls

### D10 — First-field spacing differs across editors

- Location: `activity_account_editor.xml`, `activity_category_editor.xml`, `activity_record_editor.xml`, `activity_date_range.xml`; `styles.xml` (`FormLabel`).
- Observation: Account and date-range forms combine a 16 dp body inset with the first label's inherited 16 dp top margin. Record editor explicitly removes that first margin. Category editor can add an origin line ahead of the first field.
- Recommendation: Use a consistent first-field offset from the divider; reserve the 16 dp label margin for separation from a preceding group. Retain archived-state information where applicable.

### D11 — Label-to-control spacing is not consistent

- Location: Record editor account/category/date/time controls; date-range fields; account icon selector; category icon/color grids.
- Observation: Record controls explicitly use 4 dp above controls, range dates use 8 dp, icon grids use 8 dp, while some selectors sit immediately after their labels.
- Recommendation: Standardize label-to-field and label-to-selector spacing. Use 4–8 dp based on control type, consistently across screens, without adding margins on top of equivalent drawable padding.

### D12 — Adjacent selection tiles share touching outlines

- Location: `activity_record_editor.xml` (`type`), `activity_account_editor.xml` (`account_icon`), `panel_analysis.xml` (`breakdown_mode`), `activity_export.xml` (`export_type`).
- Observation: Independently outlined choices have no inter-item margins. Category icon and color choices, by contrast, have 3 dp margins on each side. Touching tile borders can appear heavier than outer borders.
- Recommendation: Use a small consistent gap, or a deliberately shared single divider for segmented groups. Do not accidentally create thick double borders. Keep single- and multi-select behavior unchanged.

### D13 — Icon-to-label spacing is implicit in icon selectors

- Location: `activity_account_editor.xml` bank/cash/card choices; `activity_record_editor.xml` transaction-type choices.
- Observation: Both use top compound icons without explicit drawable padding, unlike navigation and action buttons, which specify their gaps.
- Recommendation: Set a small consistent icon-to-label gap, such as 4 dp, after checking the icon artwork bounds. Preserve equal tile heights and centered contents.

### D14 — Export section labels use different typography

- Location: `activity_export.xml`, Select accounts label versus `export_label`.
- Observation: Select accounts is an unstyled TextView; the next section uses the muted 14 sp FormLabel treatment with a defined section margin.
- Recommendation: Give both section labels the same typography and control gap, overriding only the first label's leading margin.

### D15 — Export selection groups use different text alignment

- Location: `repository/app/src/main/java/com/aravind/wallet/ui/ExportActivity.kt`, dynamically added account CheckBoxes; `styles.xml` (`WireCheckbox`, `SquareRadio`).
- Observation: Account checkbox labels use their normal leading alignment and 12 dp padding, while transaction-type radio tiles inherit centered gravity and 8 dp padding. These are consecutive full-width outlined selection groups with different label columns.
- Recommendation: Align full-width export choices to the same leading text inset. Preserve centered labels for horizontal segmented groups where appropriate and retain multiple-account selection.

### D16 — Date fields use different content insets and text sizes

- Location: `activity_date_range.xml`, `activity_record_editor.xml`, `styles.xml`.
- Observation: Range date buttons explicitly use 8 dp horizontal padding and 16 sp text. Record date/time buttons inherit 12 dp padding and 14 sp text from PlainButton/OutlineButton. These are similar date-entry controls with different text density.
- Recommendation: Match field text size and inner padding across date/time controls, allowing the narrower two-column editor to wrap or grow as needed.

## Actions and icons

### D17 — Custom range button mixes centered text with a leading-edge icon

- Location: `panel_analysis.xml` (`custom_range`).
- Observation: Most full-width icon action buttons explicitly use `start|center_vertical`. Custom range inherits Button's default alignment while its calendar is a start compound drawable.
- Recommendation: Align its icon and label as a single leading action group using the same inset as other outlined actions.

### D18 — Use selected icon button differs from the other confirmation actions

- Location: `AdditionalIconsActivity.kt`, final button.
- Observation: This full-width confirmation is a neutral, centered outlined button, while editor confirmation buttons are left-aligned green actions with a save icon.
- Recommendation: Match the alignment and content inset of confirmation actions. Decide whether it should share green confirmation styling; it selects an icon rather than persisting a category, so that color choice needs design judgment.

### D19 — Settings uses unrelated icons for export and restore

- Location: `SettingsActivity.kt`, `button()` icon mapping.
- Observation: Export CSV uses the expense icon, and Restore backup uses the Back navigation arrow. These reuse financial-direction/navigation meanings for file operations.
- Recommendation: Use simple line-art export and restore symbols that fit the existing icon weight. Keep Back reserved for navigation. This can be done with local vectors without a new dependency.

### D20 — Destructive archive action lacks the established action treatment

- Location: `activity_category_editor.xml` (`archive`); compare record Delete.
- Observation: Archive is a neutral text-only button, whereas Delete has a red outlined action and icon. Both remove an item from normal future use, but their visual hierarchy differs.
- Recommendation: Give Archive an intentional secondary destructive treatment, with consistent leading alignment and spacing. Confirm whether muted red is appropriate; archiving remains distinct from deletion.

## Analysis and balance presentation

### D21 — Net total emphasis differs between analysis views

- Location: `panel_analysis.xml` (`period_net`, `range_net`).
- Observation: Daily Breakdown's Net value is bold; the range Net value in graph views is normal weight, despite identical summary-box sizing and the same metric hierarchy.
- Recommendation: Use the same Net emphasis across all analysis summary boxes.

### D22 — Account balance heading has excess label-to-value space

- Location: `activity_account_history.xml`; compare Accounts panel in `activity_main.xml`.
- Observation: Current balance label has 16 dp padding on all sides, putting a full 16 dp below the label before the separate value. Accounts uses a compact label and 8 dp vertical padding around its value.
- Recommendation: Separate outer section padding from the internal label/value gap; use a tighter 4–8 dp internal gap and retain clear separation before history.

### D23 — Balance headline weight differs between list and detail

- Location: `activity_main.xml` (`total_balance`), `activity_account_history.xml` (`current_balance`), `activity_category_history.xml` (`total`).
- Observation: The Accounts total is bold 24 sp; equally prominent detail totals use normal-weight 24 sp.
- Recommendation: Choose one headline weight for comparable balance/total values, preserving semantic positive/negative colors and the current hierarchy.

### D24 — Calendar internal edges can look heavier than outer edges

- Location: `repository/app/src/main/java/com/aravind/wallet/ui/MonthlyCalendar.kt`, per-cell GradientDrawable strokes.
- Observation: Every adjacent cell draws its own 1 dp stroke, placing two outlines side by side internally. The perimeter has one stroke.
- Recommendation: Draw shared internal edges once, or otherwise preserve a uniform thin grid. Verify in both themes; do not make the calendar borders heavier, which would reverse the earlier refinement.

### D25 — Flow Graph caption is crowded against the drawn date labels

- Location: `panel_analysis.xml` (`flow_graph`, following legend); `DailyFlowView.kt`, final date-label drawing.
- Observation: Dates are drawn with their baseline only 4 dp above the bottom of the chart, and the legend follows with no explicit top margin or padding. Font descent and the next line can leave little visual breathing room.
- Recommendation: Reserve bottom space within the chart and add an 8 dp chart-to-caption gap. Verify actual text bounds at larger fonts before selecting the final spacing.

### D26 — Chart view uses a fixed height regardless of available width

- Location: `panel_analysis.xml` (280 dp Flow Graph); `AnalysisPanel.kt` (280 dp wheel); `CategoryWheelView.kt` (radius based on smaller dimension).
- Observation: The wheel remains the same height on wide screens, leaving broad unused horizontal space; the flow graph can become shallow in proportion to its width. Narrow or landscape screens have a different visual density from the rest of the responsive content.
- Recommendation: Check both orientations and wider devices, then use modest width-aware height limits if needed. Keep a bounded chart size and the existing simple scrolling structure.

## Recommended order

1. Align Settings/header geometry, list gutters, and trailing Edit actions.
2. Normalize first-field gaps, selector spacing, export-choice alignment, and date-field insets.
3. Match confirmation-action alignment, summary emphasis, and balance typography.
4. Verify calendar stroke weight and chart/caption spacing visually in both themes.

Review at normal text size and larger text, on a narrow phone and in landscape. Accept a recommendation only if it improves the existing design without unnecessary height or decoration. This report does not authorize implementation or change the roadmap/version scope.
