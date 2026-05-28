# BudgetTracker — Full Feature Spec
**Status:** Pre-implementation reference. No code has been changed yet.
**Date drafted:** 2026-05-27

---

## 1. Current State (v1.1 — what exists today)

### Tech stack
| Layer | Technology |
|---|---|
| Language | Java (Android) |
| UI | XML layouts, Material Components |
| Local DB | Room (SQLite), DB version 2 |
| Cloud sync | Google Sheets via Apps Script (Retrofit 2 + Gson) |
| Architecture | ViewModel + LiveData + Repository |
| Min SDK | 24 (Android 7.0) |

### Data model — single table
```
expenses
  id          TEXT  PRIMARY KEY   (UUID)
  userId      TEXT                (device UUID — isolates rows per device)
  title       TEXT
  amount      REAL
  date        TEXT                (YYYY-MM-DD)
  category    TEXT                (one of 8 fixed values below)
  isSynced    INTEGER             (0 = pending push to Sheets, 1 = synced)
```

### Fixed categories (hardcoded in MainActivity)
`🍔 Food`, `🚗 Transport`, `🏠 Housing`, `💊 Health`,
`🎮 Entertainment`, `📦 Shopping`, `💡 Utilities`, `📝 Other`

### What the app currently does
- Add an expense: title, amount, date (date-picker), category (dropdown)
- Delete an expense (tap the red bin icon on the row)
- Filter the list by month via a bottom-sheet picker
- View the running total for the selected month (or all months)
- Currency picker on first launch; symbol shown throughout
- Bi-directional sync with Google Sheets (on launch + manual refresh button)
- Sync-status dot on each expense row (orange = pending, hidden = synced)
- Groups the list by month with a divider header

### What is missing
- No budget concept anywhere (no target to track against)
- No per-category breakdown or grouping
- No way to edit an expense — only delete
- No overview of where money is going
- No progress toward a limit

---

## 2. Feature Additions (v2.0 scope)

### 2.1 Monthly budget
Set a single total-spend target for each calendar month.
The header card changes to show **Spent / Budget** and a horizontal progress bar.
Colour coding:
- **Green** — spent < 75 % of budget
- **Amber** — spent 75 %–99 %
- **Red** — spent ≥ 100 % (over budget)

If no budget is set for a month the header falls back to showing the plain total as today.

### 2.2 Per-category budget allocation
Within the same "Set Budget" flow, the user can optionally allocate portions of the monthly total to individual categories (e.g. Food ₦30 000, Transport ₦10 000).
Category allocations are optional — leaving one blank means "untracked for this category."
The sum of category allocations is shown against the monthly total as a sanity check but is **not enforced** (you can allocate more or less than the monthly total).

### 2.3 Category breakdown view
A toggle in the toolbar switches the main list between two views:

**By Month (existing)** — expenses grouped by calendar month, newest first, as today.

**By Category (new)** — one section per category that has at least one expense in the selected period.
Each section header shows:
- Category emoji + name
- Subtotal spent in that category
- Mini progress bar if a category budget allocation exists
- Tap to collapse / expand the expense rows inside

### 2.4 Budget set / edit sheet
Tapping a new "Set Budget" icon in the toolbar opens a bottom sheet:
- **Month** label at the top (read-only, shows the currently selected month)
- **Total budget** — a single number field
- **Category allocations** — a compact list: each category on one row with a number field beside it
- **Save** button — writes to Room; updates the header immediately
- If a budget already exists for that month it opens pre-filled (edit mode)

### 2.5 Edit expense
Long-pressing any expense row opens the existing "Add Expense" bottom sheet pre-filled with that row's data. The "Add Expense" button is relabelled "Save Changes". On save it updates the row in Room (and marks it unsynced for the next Sheets push).

### 2.6 Budget alert banner
When an addition causes spending to reach or exceed **80 %** of the monthly budget a non-blocking `Snackbar` appears:
- At 80 %: "Heads up — you've used 80 % of your budget for [Month]"
- At 100 %: "Over budget for [Month]!"
Shown once per threshold per month session (not stored persistently).

---

## 3. Data Model Changes

### New table — `monthly_budgets`
```
monthly_budgets
  id          TEXT  PRIMARY KEY   (userId + "_" + monthYear, e.g. "abc123_2026-05")
  userId      TEXT
  monthYear   TEXT                (YYYY-MM)
  totalBudget REAL                (0 = not set)
```

### New table — `category_budgets`
```
category_budgets
  id          TEXT  PRIMARY KEY   (userId + "_" + monthYear + "_" + category)
  userId      TEXT
  monthYear   TEXT
  category    TEXT
  budgetAmount REAL
```

### Existing table — `expenses` — change
Add one column:
```
  notes       TEXT  DEFAULT ''    (free-text note; blank = no note)
```
This supports the edit flow without a schema clash.

### DB version bump
`version 2` → `version 3`
Migration provided (not `fallbackToDestructiveMigration`) so existing expense rows are preserved:
```sql
ALTER TABLE expenses ADD COLUMN notes TEXT NOT NULL DEFAULT '';
CREATE TABLE monthly_budgets (...);
CREATE TABLE category_budgets (...);
```

---

## 4. New and Changed Files

### Project package structure (reorganised — done)
```
com.iykeafrica.budgettracker/
  data/
    local/        BudgetDatabase, Expense, ExpenseDao
    remote/       SheetsApiClient, SheetsApiService, FetchResponse, MutationResponse, SheetExpense
    repository/   ExpenseRepository
  ui/             MainActivity, ExpenseAdapter, CurrencyPickerDialog
  viewmodel/      BudgetViewModel
  util/           UserPreferences, CurrencyConfig
```

### New Java files
| File | Purpose |
|---|---|
| `data/local/MonthlyBudget.java` | Room entity for the `monthly_budgets` table |
| `data/local/CategoryBudget.java` | Room entity for the `category_budgets` table |
| `data/local/BudgetDao.java` | DAO: CRUD for both budget tables; query to get budget + category allocations for a month |
| `viewmodel/BudgetSummary.java` | Plain Java value object: holds `totalBudget`, `totalSpent`, per-category breakdown; computed in ViewModel |

### Changed Java files
| File | What changes |
|---|---|
| `data/local/BudgetDatabase.java` | Add `MonthlyBudget` and `CategoryBudget` to `@Database`; version 2 → 3; add `BudgetDao` accessor; add explicit `Migration(2, 3)` |
| `data/local/ExpenseDao.java` | Add `getTotalSpentByCategory(userId, monthYear)` — returns a list of `(category, sum)` pairs for the breakdown |
| `data/repository/ExpenseRepository.java` | Expose `getTotalSpentByCategory()`; add `updateExpense()` for the edit flow; pass `notes` through `addExpense()` |
| `viewmodel/BudgetViewModel.java` | Add `LiveData<MonthlyBudget>` and `LiveData<List<CategoryBudget>>` (switch-mapped from selectedMonth); add `LiveData<BudgetSummary>`; add `setMonthlyBudget()`; add `updateExpense()`; expose `viewMode` LiveData (BY_MONTH / BY_CATEGORY) |
| `ui/MainActivity.java` | Wire budget summary to header card; add Set Budget toolbar icon; add view toggle icon; handle edit on long-press; show budget alert Snackbar |
| `ui/ExpenseAdapter.java` | Support BY_CATEGORY mode: new `TYPE_CATEGORY_HEADER` view type; collapse/expand state per category; pass `BudgetSummary` in for mini progress bars on category headers |

### New XML layouts
| File | Purpose |
|---|---|
| `bottom_sheet_set_budget.xml` | "Set Budget" sheet: month label, total budget field, list of category rows each with a number field |
| `item_category_header.xml` | Section header for the By-Category list: emoji + name, subtotal, mini progress bar, chevron for expand/collapse |

### Changed XML layouts
| File | What changes |
|---|---|
| `activity_main.xml` | Header card gets a budget progress bar row and a "remaining" / "over budget" label below the total; toolbar gets two new `ImageButton`s (Set Budget, Toggle View) |
| `bottom_sheet_add_expense.xml` | Add a "Notes (optional)" `TextInputLayout` at the bottom; title text made dynamic so it can read "Edit Expense" |
| `item_expense.xml` | Add a `TextView` for the notes field (hidden when blank) |

---

## 5. Screen-by-Screen UI Spec

### 5.1 Main screen — header card (budget set)
```
┌─────────────────────────────────────────┐
│  Total for May 2026                     │
│  ₦ 47,200.00          Budget: ₦80,000  │
│  [████████████░░░░░░░░░] 59%            │
│  ₦32,800.00 remaining                   │
└─────────────────────────────────────────┘
```

### 5.2 Main screen — header card (no budget set)
```
┌─────────────────────────────────────────┐
│  Total Expenses                         │
│  ₦ 47,200.00                            │
│  Tap 📅 to set a budget for this month  │
└─────────────────────────────────────────┘
```

### 5.3 Main screen — toolbar
```
[Budget Tracker]  [📅 Set Budget]  [☰ Toggle View]  [🔄 Sync]
```
(Filter-month icon replaced by the more capable Set Budget icon; month filter moves to a long-press or a separate chip below the toolbar)

Actually — to avoid removing the month filter, the toolbar has:
```
[Budget Tracker]  [📅 Set Budget]  [⊞ Category View]  [📆 Month Filter]  [🔄 Sync]
```

### 5.4 By-Category list view
```
▼  🍔 Food          ₦18,400   [██████░░░░] 61% of ₦30,000
   Groceries              2026-05-20   ₦6,500
   Lunch @ work           2026-05-18   ₦2,800
   ...

▼  🚗 Transport     ₦9,200    [████████░░] 92% of ₦10,000  ← amber
   Uber                   2026-05-21   ₦3,100
   ...

▶  🎮 Entertainment  ₦5,100   [no allocation]
   (collapsed)
```

### 5.5 Set Budget bottom sheet
```
Set Budget — May 2026
─────────────────────
Monthly total:   [₦ ________]

Category allocations (optional)
🍔 Food          [₦ ________]
🚗 Transport     [₦ ________]
🏠 Housing       [₦ ________]
💊 Health        [₦ ________]
🎮 Entertainment [₦ ________]
📦 Shopping      [₦ ________]
💡 Utilities     [₦ ________]
📝 Other         [₦ ________]

            [ Save Budget ]
```

### 5.6 Add / Edit Expense bottom sheet (updated)
```
Add Expense   (or "Edit Expense" when editing)
─────────────────────
Title         [___________]
Amount        [___________]
Date          [___________] 📅
Category      [▼ Food     ]
Notes (opt.)  [___________]

            [ Add Expense ] / [ Save Changes ]
```

---

## 6. ViewModel — new LiveData contract

```java
// Existing (unchanged)
LiveData<String>          getSelectedMonth()
LiveData<List<Expense>>   getFilteredExpenses()
LiveData<String>          getCurrencySymbol()
LiveData<Boolean>         getIsSetupDone()
LiveData<Boolean>         getIsSyncing()
LiveData<String>          getStatusMessage()

// New
LiveData<MonthlyBudget>        getMonthlyBudget()        // for current month; null = not set
LiveData<List<CategoryBudget>> getCategoryBudgets()      // for current month
LiveData<BudgetSummary>        getBudgetSummary()        // derived: spent, remaining, % per category
LiveData<ViewMode>             getViewMode()             // BY_MONTH or BY_CATEGORY

// New actions
void setMonthlyBudget(double total, Map<String,Double> categoryAllocations)
void updateExpense(Expense updated)
void toggleViewMode()
```

`BudgetSummary` value object:
```java
class BudgetSummary {
    double totalBudget;           // 0 if not set
    double totalSpent;
    double remaining;             // totalBudget - totalSpent (negative = over)
    int    percentUsed;           // 0–∞
    Map<String, Double> spentByCategory;
    Map<String, Double> budgetByCategory;
}
```

---

## 7. Database Migration (2 → 3)

```java
static final Migration MIGRATION_2_3 = new Migration(2, 3) {
    @Override
    public void migrate(SupportSQLiteDatabase db) {
        // Preserve all existing expense data
        db.execSQL("ALTER TABLE expenses ADD COLUMN notes TEXT NOT NULL DEFAULT ''");

        db.execSQL("CREATE TABLE IF NOT EXISTS monthly_budgets (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "userId TEXT NOT NULL, " +
                "monthYear TEXT NOT NULL, " +
                "totalBudget REAL NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS category_budgets (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "userId TEXT NOT NULL, " +
                "monthYear TEXT NOT NULL, " +
                "category TEXT NOT NULL, " +
                "budgetAmount REAL NOT NULL DEFAULT 0)");
    }
};
```

`BudgetDatabase` passes this to `.addMigrations(MIGRATION_2_3)` and removes `fallbackToDestructiveMigration()`.

---

## 8. Google Sheets sync — scope

Budget data (`monthly_budgets`, `category_budgets`) is **device-local only** in this version.
Only the `expenses` table syncs to Sheets. The `notes` column added to expenses **will** be included in push/pull payloads (new field in `SheetExpense` + Apps Script update needed — noted but not in this spec scope).

The edit-expense path marks the updated row as `isSynced = false` so it is pushed on the next sync.

---

## 9. What Does NOT Change

- `UserPreferences.java` — no changes
- `CurrencyPickerDialog.java` — no changes
- `SheetsApiClient.java` / `SheetsApiService.java` — no changes (notes field is additive)
- `FetchResponse.java` / `MutationResponse.java` / `SheetExpense.java` — no changes
- `ExpenseAdapter` item layout for expense rows — only adds the notes TextView (hidden when empty)
- All 8 categories remain fixed strings; no UI to add/remove them

---

## 10. Build / Dependency Changes

No new external dependencies required. All features use libraries already in `build.gradle.kts`:
- Room (already present) — two new entities + one new DAO
- Material Components (already present) — progress bar, Snackbar, text fields
- ViewModel / LiveData (already present) — new LiveData fields

---

## 11. Implementation Order (when approved)

1. `MonthlyBudget.java` + `CategoryBudget.java` + `BudgetSummary.java` (data layer, no UI)
2. `BudgetDao.java` + migration in `BudgetDatabase.java` (DB compiles + migrates)
3. `ExpenseDao.java` additions (`getTotalSpentByCategory`, `updateExpense` query)
4. `ExpenseRepository.java` — wire new DAO methods + `updateExpense()`
5. `BudgetViewModel.java` — new LiveData + `setMonthlyBudget()` + `toggleViewMode()`
6. `bottom_sheet_set_budget.xml` + `item_category_header.xml` (new layouts)
7. `activity_main.xml` — header card + toolbar icons (layout only)
8. `bottom_sheet_add_expense.xml` + `item_expense.xml` — add notes field
9. `ExpenseAdapter.java` — category view mode + collapse/expand + mini progress bars
10. `MainActivity.java` — wire everything: budget header, set-budget sheet, edit on long-press, view toggle, alert Snackbar

Each step compiles independently. Steps 1–5 are pure logic with no UI; steps 6–10 are UI.