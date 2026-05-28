package com.iykeafrica.budgettracker.ui;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.iykeafrica.budgettracker.R;
import com.iykeafrica.budgettracker.data.local.Expense;
import com.iykeafrica.budgettracker.viewmodel.BudgetSummary;
import com.iykeafrica.budgettracker.viewmodel.BudgetViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;

public class MainActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private TextView       tvTotal, tvExpenseCount, tvTotalLabel, tvEmpty;
    private TextView       tvCurrencySymbol, tvBudgetAmount, tvBudgetRemaining, tvSetBudgetPrompt;
    private ProgressBar    progressBarSync, progressBudget;
    private View           layoutBudgetTarget;
    private ImageButton    btnRefresh, btnFilter, btnSetBudget, btnToggleView;
    private RecyclerView   recyclerView;
    private FloatingActionButton fab;

    // ── Add/Edit sheet ─────────────────────────────────────────────────────
    private BottomSheetDialog expenseSheet;
    private TextView          tvSheetTitle;
    private TextInputEditText etTitle, etAmount, etDate, etNotes;
    private AutoCompleteTextView spinnerCategory;
    private Expense           editingExpense = null;  // null = add mode

    // ── State ──────────────────────────────────────────────────────────────
    private BudgetViewModel viewModel;
    private ExpenseAdapter  adapter;
    private String          selectedDate   = "";
    private String          currentSymbol  = "₦";
    private boolean         currencyPickerShown = false;

    // alert thresholds already shown this session
    private boolean alert80Shown  = false;
    private boolean alert100Shown = false;

    private static final String[] CATEGORIES = {
            "🍔 Food", "🚗 Transport", "🏠 Housing", "💊 Health",
            "🎮 Entertainment", "📦 Shopping", "💡 Utilities", "📝 Other"
    };

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        bindViews();
        applyWindowInsets();
        setupToolbar();
        setupRecyclerView();
        setupExpenseSheet();
        setupViewModel();
        setupClickListeners();
    }

    // ── Insets ─────────────────────────────────────────────────────────────

    private void applyWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.rootLayout), (view, insets) -> {
                    androidx.core.graphics.Insets sb =
                            insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                    view.setPadding(sb.left, sb.top, sb.right, sb.bottom);
                    return insets;
                });
    }

    // ── Setup ──────────────────────────────────────────────────────────────

    private void bindViews() {
        tvTotal             = findViewById(R.id.tvTotal);
        tvTotalLabel        = findViewById(R.id.tvTotalLabel);
        tvExpenseCount      = findViewById(R.id.tvExpenseCount);
        tvEmpty             = findViewById(R.id.tvEmpty);
        tvCurrencySymbol    = findViewById(R.id.tvCurrencySymbol);
        tvBudgetAmount      = findViewById(R.id.tvBudgetAmount);
        tvBudgetRemaining   = findViewById(R.id.tvBudgetRemaining);
        tvSetBudgetPrompt   = findViewById(R.id.tvSetBudgetPrompt);
        layoutBudgetTarget  = findViewById(R.id.layoutBudgetTarget);
        progressBudget      = findViewById(R.id.progressBudget);
        progressBarSync     = findViewById(R.id.progressBarSync);
        btnRefresh          = findViewById(R.id.btnRefresh);
        btnFilter           = findViewById(R.id.btnFilter);
        btnSetBudget        = findViewById(R.id.btnSetBudget);
        btnToggleView       = findViewById(R.id.btnToggleView);
        recyclerView        = findViewById(R.id.recyclerView);
        fab                 = findViewById(R.id.fab);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(
                expense -> {
                    viewModel.deleteExpense(expense);
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                },
                expense -> openExpenseSheet(expense),   // long-press → edit
                () -> currentSymbol
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
    }

    // ── Expense sheet (add / edit) ─────────────────────────────────────────

    private void setupExpenseSheet() {
        expenseSheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_expense, null);
        expenseSheet.setContentView(sheetView);

        tvSheetTitle    = sheetView.findViewById(R.id.tvSheetTitle);
        etTitle         = sheetView.findViewById(R.id.etTitle);
        etAmount        = sheetView.findViewById(R.id.etAmount);
        etDate          = sheetView.findViewById(R.id.etDate);
        etNotes         = sheetView.findViewById(R.id.etNotes);
        spinnerCategory = sheetView.findViewById(R.id.spinnerCategory);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        spinnerCategory.setAdapter(catAdapter);
        spinnerCategory.setText(CATEGORIES[0], false);

        etDate.setOnClickListener(v -> showDatePicker());
        etDate.setFocusable(false);
        setTodayAsDefault();

        sheetView.findViewById(R.id.btnAdd).setOnClickListener(v -> submitExpenseForm());
    }

    private void openExpenseSheet(Expense expense) {
        editingExpense = expense;
        tvSheetTitle.setText(expense == null ? "Add Expense" : "Edit Expense");
        ((com.google.android.material.button.MaterialButton)
                expenseSheet.findViewById(R.id.btnAdd))
                .setText(expense == null ? "Add Expense" : "Save Changes");

        if (expense != null) {
            etTitle.setText(expense.getTitle());
            etAmount.setText(String.valueOf(expense.getAmount()));
            selectedDate = expense.getDate();
            try {
                SimpleDateFormat p = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat f = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                etDate.setText(f.format(p.parse(expense.getDate())));
            } catch (Exception e) { etDate.setText(expense.getDate()); }
            // Find matching category
            for (String cat : CATEGORIES) {
                if (cat.equals(expense.getCategory())) {
                    spinnerCategory.setText(cat, false); break;
                }
            }
            String notes = expense.getNotes();
            etNotes.setText(notes != null ? notes : "");
        }
        expenseSheet.show();
    }

    private void submitExpenseForm() {
        String title    = etTitle.getText()    != null ? etTitle.getText().toString()    : "";
        String amount   = etAmount.getText()   != null ? etAmount.getText().toString()   : "";
        String category = spinnerCategory.getText() != null ? spinnerCategory.getText().toString() : "";
        String notes    = etNotes.getText()    != null ? etNotes.getText().toString()    : "";

        if (editingExpense != null) {
            // Edit mode — validate then update
            if (title.trim().isEmpty())   { Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show(); return; }
            double amt;
            try { amt = Double.parseDouble(amount.trim()); }
            catch (NumberFormatException e) { Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show(); return; }
            if (amt <= 0) { Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show(); return; }
            if (selectedDate.isEmpty()) { Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show(); return; }

            editingExpense.setTitle(title.trim());
            editingExpense.setAmount(amt);
            editingExpense.setDate(selectedDate);
            editingExpense.setCategory(category);
            editingExpense.setNotes(notes.trim());
            viewModel.updateExpense(editingExpense);
            Toast.makeText(this, "Expense updated ✓", Toast.LENGTH_SHORT).show();
        } else {
            // Add mode
            String error = viewModel.addExpense(title, amount, selectedDate, category, notes);
            if (error != null) { Toast.makeText(this, error, Toast.LENGTH_SHORT).show(); return; }
            Toast.makeText(this, "Expense added ✓", Toast.LENGTH_SHORT).show();
        }

        clearForm();
        hideKeyboard();
        expenseSheet.dismiss();
    }

    // ── ViewModel ──────────────────────────────────────────────────────────

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        viewModel.getFilteredExpenses().observe(this, expenses -> {
            rebuildList(expenses, viewModel.getViewMode().getValue());
            updateTotal(expenses);
            updateExpenseCount(expenses.size());
            Boolean syncing = viewModel.getIsSyncing().getValue();
            updateEmptyState(expenses.isEmpty(), Boolean.TRUE.equals(syncing));
        });

        viewModel.getViewMode().observe(this, mode -> {
            List<Expense> expenses = viewModel.getFilteredExpenses().getValue();
            if (expenses != null) rebuildList(expenses, mode);
        });

        viewModel.getSelectedMonth().observe(this, month -> {
            if (tvTotalLabel == null) return;
            tvTotalLabel.setText(month == null || month.isEmpty()
                    ? "Total Expenses" : "Total for " + formatMonthLabel(month));
            // Reset alert thresholds when month changes
            alert80Shown  = false;
            alert100Shown = false;
        });

        viewModel.getBudgetSummary().observe(this, this::updateBudgetCard);

        viewModel.getCurrencySymbol().observe(this, symbol -> {
            currentSymbol = symbol;
            tvCurrencySymbol.setText(symbol);
            adapter.notifyDataSetChanged();
        });

        viewModel.getIsSetupDone().observe(this, isDone -> {
            if (!isDone && !currencyPickerShown) {
                currencyPickerShown = true;
                getWindow().getDecorView().post(this::showCurrencyPicker);
            }
        });

        viewModel.getIsSyncing().observe(this, isSyncing -> {
            progressBarSync.setVisibility(isSyncing ? View.VISIBLE : View.GONE);
            btnRefresh.setVisibility(isSyncing ? View.GONE : View.VISIBLE);
            // Update empty state message while syncing
            List<Expense> current = viewModel.getFilteredExpenses().getValue();
            boolean isEmpty = current == null || current.isEmpty();
            if (isEmpty) updateEmptyState(true, isSyncing);
        });

        viewModel.getStatusMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                viewModel.onStatusMessageShown();
            }
        });
    }

    private void setupClickListeners() {
        fab.setOnClickListener(v -> {
            editingExpense = null;
            setTodayAsDefault();
            openExpenseSheet(null);
        });
        btnRefresh.setOnClickListener(v -> viewModel.onManualRefresh());
        btnFilter.setOnClickListener(v -> showMonthPicker());
        btnSetBudget.setOnClickListener(v -> showSetBudgetSheet());
        btnToggleView.setOnClickListener(v -> viewModel.toggleViewMode());
    }

    // ── List building ──────────────────────────────────────────────────────

    private void rebuildList(List<Expense> expenses, BudgetViewModel.ViewMode mode) {
        if (mode == BudgetViewModel.ViewMode.BY_CATEGORY) {
            adapter.submitList(buildCategoryList(expenses));
        } else {
            adapter.submitList(buildMonthList(expenses));
        }
    }

    private List<Object> buildMonthList(List<Expense> expenses) {
        List<Object> list = new ArrayList<>();
        if (expenses == null || expenses.isEmpty()) return list;
        String lastMonth = "";
        for (Expense e : expenses) {
            String month = monthLabelOf(e);
            if (!month.equals(lastMonth)) { list.add(month); lastMonth = month; }
            list.add(e);
        }
        return list;
    }

    private List<Object> buildCategoryList(List<Expense> expenses) {
        List<Object> list = new ArrayList<>();
        if (expenses == null || expenses.isEmpty()) return list;

        // Group expenses by category (preserve insertion order)
        Map<String, List<Expense>> grouped = new java.util.LinkedHashMap<>();
        for (Expense e : expenses) {
            grouped.computeIfAbsent(e.getCategory(), k -> new ArrayList<>()).add(e);
        }

        Set<String> collapsed = adapter.collapsedCategories;
        for (Map.Entry<String, List<Expense>> entry : grouped.entrySet()) {
            list.add(new ExpenseAdapter.CategoryHeader(entry.getKey()));
            if (!collapsed.contains(entry.getKey())) {
                list.addAll(entry.getValue());
            }
        }
        return list;
    }

    // ── Budget card ────────────────────────────────────────────────────────

    private void updateBudgetCard(BudgetSummary summary) {
        adapter.setBudgetSummary(summary);

        if (summary == null || !summary.hasBudget()) {
            layoutBudgetTarget.setVisibility(View.GONE);
            progressBudget.setVisibility(View.GONE);
            tvBudgetRemaining.setVisibility(View.GONE);
            tvSetBudgetPrompt.setVisibility(View.VISIBLE);
            return;
        }

        tvSetBudgetPrompt.setVisibility(View.GONE);
        layoutBudgetTarget.setVisibility(View.VISIBLE);
        progressBudget.setVisibility(View.VISIBLE);
        tvBudgetRemaining.setVisibility(View.VISIBLE);

        tvBudgetAmount.setText(currentSymbol + String.format(
                Locale.getDefault(), "%,.0f", summary.totalBudget));

        int pct = Math.min(summary.percentUsed, 100);
        progressBudget.setProgress(pct);

        int tint;
        if (summary.percentUsed >= 100)      tint = 0xFFBA1A1A;  // red
        else if (summary.percentUsed >= 75)  tint = 0xFFE65100;  // amber
        else                                 tint = 0xFFFFFFFF;  // white

        progressBudget.setProgressTintList(ColorStateList.valueOf(tint));

        if (summary.remaining >= 0) {
            tvBudgetRemaining.setText(currentSymbol + String.format(
                    Locale.getDefault(), "%,.2f", summary.remaining) + " remaining");
            tvBudgetRemaining.setTextColor(0xFFCCE4FF);
        } else {
            tvBudgetRemaining.setText(currentSymbol + String.format(
                    Locale.getDefault(), "%,.2f", Math.abs(summary.remaining)) + " over budget!");
            tvBudgetRemaining.setTextColor(0xFFFFB4AB);
        }

        // Budget alerts — show once per threshold per month session
        String month = viewModel.getSelectedMonth().getValue();
        String monthLabel = month != null ? formatMonthLabel(month) : "this month";
        if (!alert100Shown && summary.percentUsed >= 100) {
            alert100Shown = true;
            Snackbar.make(fab, "Over budget for " + monthLabel + "!", Snackbar.LENGTH_LONG).show();
        } else if (!alert80Shown && summary.percentUsed >= 80) {
            alert80Shown = true;
            Snackbar.make(fab, "Heads up — you've used 80% of your budget for " + monthLabel,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    // ── Set Budget sheet ───────────────────────────────────────────────────

    private void showSetBudgetSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_set_budget, null);
        sheet.setContentView(v);

        // Show the current month label
        String month = viewModel.getSelectedMonth().getValue();
        ((TextView) v.findViewById(R.id.tvBudgetMonth))
                .setText(month != null ? formatMonthLabel(month) : "");

        // Map from category key (e.g. "🍔 Food") to EditText id
        Map<String, TextInputEditText> catFields = new HashMap<>();
        catFields.put("🍔 Food",          v.findViewById(R.id.etBudgetFood));
        catFields.put("🚗 Transport",     v.findViewById(R.id.etBudgetTransport));
        catFields.put("🏠 Housing",       v.findViewById(R.id.etBudgetHousing));
        catFields.put("💊 Health",        v.findViewById(R.id.etBudgetHealth));
        catFields.put("🎮 Entertainment", v.findViewById(R.id.etBudgetEntertainment));
        catFields.put("📦 Shopping",      v.findViewById(R.id.etBudgetShopping));
        catFields.put("💡 Utilities",     v.findViewById(R.id.etBudgetUtilities));
        catFields.put("📝 Other",         v.findViewById(R.id.etBudgetOther));

        // Pre-fill from existing budget if set
        BudgetSummary existing = viewModel.getBudgetSummary().getValue();
        TextInputEditText etTotal = v.findViewById(R.id.etTotalBudget);
        if (existing != null && existing.hasBudget()) {
            etTotal.setText(String.valueOf((long) existing.totalBudget));
            for (Map.Entry<String, TextInputEditText> entry : catFields.entrySet()) {
                Double val = existing.budgetByCategory.get(entry.getKey());
                if (val != null && val > 0) {
                    entry.getValue().setText(String.valueOf(val.longValue()));
                }
            }
        }

        v.findViewById(R.id.btnSaveBudget).setOnClickListener(btn -> {
            String totalStr = etTotal.getText() != null ? etTotal.getText().toString().trim() : "";
            if (totalStr.isEmpty()) {
                Toast.makeText(this, "Please enter a monthly budget total", Toast.LENGTH_SHORT).show();
                return;
            }
            double total;
            try { total = Double.parseDouble(totalStr); }
            catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Double> allocations = new HashMap<>();
            for (Map.Entry<String, TextInputEditText> entry : catFields.entrySet()) {
                String val = entry.getValue().getText() != null
                        ? entry.getValue().getText().toString().trim() : "";
                if (!val.isEmpty()) {
                    try { allocations.put(entry.getKey(), Double.parseDouble(val)); }
                    catch (NumberFormatException ignored) {}
                }
            }

            viewModel.setMonthlyBudget(total, allocations);
            alert80Shown  = false;
            alert100Shown = false;
            sheet.dismiss();
            Toast.makeText(this, "Budget saved ✓", Toast.LENGTH_SHORT).show();
        });

        sheet.show();
    }

    // ── Currency picker ────────────────────────────────────────────────────

    private void showCurrencyPicker() {
        new CurrencyPickerDialog(this, (code, symbol, countryName) -> {
            viewModel.saveCurrency(code, symbol, countryName);
            Toast.makeText(this, "Currency set to " + symbol + " (" + countryName + ")",
                    Toast.LENGTH_SHORT).show();
        }).show();
    }

    // ── Month picker ───────────────────────────────────────────────────────

    private void showMonthPicker() {
        new Thread(() -> {
            List<String> rawMonths = new ArrayList<>(viewModel.getAvailableMonths());
            String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    .format(new java.util.Date());
            if (!rawMonths.contains(currentMonth)) rawMonths.add(0, currentMonth);
            rawMonths.add("Show All");

            runOnUiThread(() -> {
                BottomSheetDialog monthSheet = new BottomSheetDialog(this);
                View mv = getLayoutInflater().inflate(R.layout.bottom_sheet_month_picker, null);
                monthSheet.setContentView(mv);
                RecyclerView rvMonths = mv.findViewById(R.id.rvMonths);
                rvMonths.setLayoutManager(new LinearLayoutManager(this));
                rvMonths.setAdapter(new RecyclerView.Adapter<MonthVH>() {
                    @NonNull @Override
                    public MonthVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int type) {
                        return new MonthVH(getLayoutInflater()
                                .inflate(R.layout.item_month_selection, parent, false));
                    }
                    @Override
                    public void onBindViewHolder(@NonNull MonthVH holder, int position) {
                        String m = rawMonths.get(position);
                        holder.tv.setText("Show All".equals(m) ? m : formatMonthLabel(m));
                        holder.itemView.setOnClickListener(x -> {
                            viewModel.setSelectedMonth("Show All".equals(m) ? null : m);
                            monthSheet.dismiss();
                        });
                    }
                    @Override public int getItemCount() { return rawMonths.size(); }
                });
                monthSheet.show();
            });
        }).start();
    }

    private static class MonthVH extends RecyclerView.ViewHolder {
        TextView tv;
        MonthVH(View v) { super(v); tv = v.findViewById(R.id.tvMonthOption); }
    }

    // ── Date picker ────────────────────────────────────────────────────────

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (dpv, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            try {
                SimpleDateFormat p = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat f = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                etDate.setText(f.format(p.parse(selectedDate)));
            } catch (Exception e) { etDate.setText(selectedDate); }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ── UI helpers ─────────────────────────────────────────────────────────

    private void updateTotal(List<Expense> expenses) {
        double total = 0;
        for (Expense e : expenses) total += e.getAmount();
        tvTotal.setText(String.format(Locale.getDefault(), "%,.2f", total));
    }

    private void updateExpenseCount(int count) {
        tvExpenseCount.setText("Expenses (" + count + ")");
    }

    private void updateEmptyState(boolean isEmpty, boolean isSyncing) {
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
        if (isEmpty) {
            tvEmpty.setText(isSyncing
                    ? "Syncing your expenses…"
                    : "No expenses yet.\nTap + to add your first one!");
        }
    }

    private String monthLabelOf(Expense e) {
        String date = e.getDate();
        String mp = date.length() >= 7 ? date.substring(0, 7) : date;
        if (mp.length() < 7) return mp;
        try {
            String[] months = { "January","February","March","April","May","June",
                                 "July","August","September","October","November","December" };
            return months[Integer.parseInt(mp.substring(5, 7)) - 1] + " " + mp.substring(0, 4);
        } catch (Exception ex) { return mp; }
    }

    private String formatMonthLabel(String monthYear) {
        try {
            return new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .format(new SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthYear));
        } catch (Exception e) { return monthYear; }
    }

    private void setTodayAsDefault() {
        Calendar cal = Calendar.getInstance();
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        if (etDate != null)
            etDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime()));
    }

    private void clearForm() {
        etTitle.setText("");
        etAmount.setText("");
        if (etNotes != null) etNotes.setText("");
        spinnerCategory.setText(CATEGORIES[0], false);
        setTodayAsDefault();
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused != null)
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(focused.getWindowToken(), 0);
    }
}
