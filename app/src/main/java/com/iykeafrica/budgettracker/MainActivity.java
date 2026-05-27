package com.iykeafrica.budgettracker;

import android.app.DatePickerDialog;
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
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private TextView tvTotal;
    private TextView tvExpenseCount;
    private TextView tvTotalLabel; // Add this to change "Total Expenses" text
    private TextView tvEmpty;
    private TextView tvCurrencySymbol;
    private ImageButton btnRefresh;
    private ImageButton btnFilter;
    private ProgressBar progressBarSync;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;

    // ── Bottom sheet views ─────────────────────────────────────────────────
    private BottomSheetDialog bottomSheet;
    private TextInputEditText etTitle;
    private TextInputEditText etAmount;
    private TextInputEditText etDate;
    private AutoCompleteTextView spinnerCategory;

    // ── State ──────────────────────────────────────────────────────────────
    private BudgetViewModel viewModel;
    private ExpenseAdapter adapter;
    private String selectedDate = "";
    private String currentSymbol = "₦";
    private boolean currencyPickerShown = false;

    // ── Categories ─────────────────────────────────────────────────────────
    private static final String[] CATEGORIES = {
            "🍔 Food",
            "🚗 Transport",
            "🏠 Housing",
            "💊 Health",
            "🎮 Entertainment",
            "📦 Shopping",
            "💡 Utilities",
            "📝 Other"
    };

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupBottomSheet();
        setupViewModel();
        setupClickListeners();
    }

    // ── Setup ──────────────────────────────────────────────────────────────

    private void bindViews() {
        tvTotal          = findViewById(R.id.tvTotal);
        tvTotalLabel     = findViewById(R.id.tvTotalLabel); // You'll need to add this ID to XML if not present
        tvExpenseCount   = findViewById(R.id.tvExpenseCount);
        tvEmpty          = findViewById(R.id.tvEmpty);
        tvCurrencySymbol = findViewById(R.id.tvCurrencySymbol);
        btnRefresh       = findViewById(R.id.btnRefresh);
        btnFilter        = findViewById(R.id.btnFilter);
        progressBarSync  = findViewById(R.id.progressBarSync);
        recyclerView     = findViewById(R.id.recyclerView);
        fab              = findViewById(R.id.fab);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(
                expense -> {
                    viewModel.deleteExpense(expense);
                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                },
                () -> currentSymbol
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
    }

    private void setupBottomSheet() {
        bottomSheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_add_expense, null);
        bottomSheet.setContentView(sheetView);

        // Find views inside the bottom sheet
        etTitle         = sheetView.findViewById(R.id.etTitle);
        etAmount        = sheetView.findViewById(R.id.etAmount);
        etDate          = sheetView.findViewById(R.id.etDate);
        spinnerCategory = sheetView.findViewById(R.id.spinnerCategory);

        // Category dropdown
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                CATEGORIES
        );
        spinnerCategory.setAdapter(categoryAdapter);
        spinnerCategory.setText(CATEGORIES[0], false);

        // Date field opens picker
        etDate.setOnClickListener(v -> showDatePicker());
        etDate.setFocusable(false);

        // Set today as default date
        setTodayAsDefault();

        // Add button inside bottom sheet
        sheetView.findViewById(R.id.btnAdd).setOnClickListener(v -> {
            String title    = etTitle.getText() != null
                    ? etTitle.getText().toString() : "";
            String amount   = etAmount.getText() != null
                    ? etAmount.getText().toString() : "";
            String category = spinnerCategory.getText() != null
                    ? spinnerCategory.getText().toString() : "";

            String error = viewModel.addExpense(title, amount, selectedDate, category);

            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            } else {
                clearForm();
                hideKeyboard();
                bottomSheet.dismiss();
                Toast.makeText(this, "Expense added ✓", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        // Observe filtered expenses
        viewModel.getFilteredExpenses().observe(this, expenses -> {
            adapter.submitList(getDisplayList(expenses));
            updateTotal(expenses);
            updateExpenseCount(expenses.size());
            updateEmptyState(expenses.isEmpty());
        });

        // Observe selected month to update UI labels
        viewModel.getSelectedMonth().observe(this, month -> {
            if (month == null || month.isEmpty()) {
                if (tvTotalLabel != null) tvTotalLabel.setText("Total Expenses");
            } else {
                String label = formatMonthLabel(month);
                if (tvTotalLabel != null) tvTotalLabel.setText("Total for " + label);
            }
        });

        // Observe currency symbol
        viewModel.getCurrencySymbol().observe(this, symbol -> {
            currentSymbol = symbol;
            tvCurrencySymbol.setText(symbol);
            adapter.notifyDataSetChanged();
        });

        // Observe first launch
        viewModel.getIsSetupDone().observe(this, isDone -> {
            if (!isDone && !currencyPickerShown) {
                currencyPickerShown = true;
                getWindow().getDecorView().post(() -> showCurrencyPicker());
            }
        });

        // Observe sync spinner
        viewModel.getIsSyncing().observe(this, isSyncing -> {
            progressBarSync.setVisibility(isSyncing ? View.VISIBLE : View.GONE);
            btnRefresh.setVisibility(isSyncing ? View.GONE : View.VISIBLE);
        });

        // Observe toast messages
        viewModel.getStatusMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                viewModel.onStatusMessageShown();
            }
        });
    }

    private void setupClickListeners() {
        // FAB opens the bottom sheet
        fab.setOnClickListener(v -> {
            setTodayAsDefault();
            bottomSheet.show();
        });

        // Refresh button
        btnRefresh.setOnClickListener(v -> viewModel.onManualRefresh());

        // Filter button
        btnFilter.setOnClickListener(v -> showMonthPicker());
    }

    // ── Date picker ────────────────────────────────────────────────────────

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    String display = String.format(Locale.getDefault(),
                            "%02d %s %04d", dayOfMonth,
                            new SimpleDateFormat("MMM", Locale.getDefault())
                                    .format(new java.util.Date(year - 1900, month, dayOfMonth)),
                            year);
                    etDate.setText(display);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // ── Currency picker ────────────────────────────────────────────────────

    private void showCurrencyPicker() {
        CurrencyPickerDialog dialog = new CurrencyPickerDialog(
                this,
                (code, symbol, countryName) -> {
                    viewModel.saveCurrency(code, symbol, countryName);
                    Toast.makeText(this,
                            "Currency set to " + symbol + " (" + countryName + ")",
                            Toast.LENGTH_SHORT).show();
                }
        );
        dialog.show();
    }

    private String formatMonthLabel(String monthYear) {
        // monthYear is "yyyy-MM"
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            java.util.Date date = parser.parse(monthYear);
            return date != null ? formatter.format(date) : monthYear;
        } catch (Exception e) {
            return monthYear;
        }
    }

    private void showMonthPicker() {
        new Thread(() -> {
            List<String> rawMonths = new ArrayList<>(viewModel.getAvailableMonths());
            String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new java.util.Date());
            if (!rawMonths.contains(currentMonth)) rawMonths.add(0, currentMonth);
            rawMonths.add("Show All");

            runOnUiThread(() -> {
                BottomSheetDialog monthSheet = new BottomSheetDialog(this);
                View view = getLayoutInflater().inflate(R.layout.bottom_sheet_month_picker, null);
                monthSheet.setContentView(view);

                RecyclerView rvMonths = view.findViewById(R.id.rvMonths);
                rvMonths.setLayoutManager(new LinearLayoutManager(this));
                
                rvMonths.setAdapter(new RecyclerView.Adapter<MonthViewHolder>() {
                    @androidx.annotation.NonNull
                    @Override
                    public MonthViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
                        View v = getLayoutInflater().inflate(R.layout.item_month_selection, parent, false);
                        return new MonthViewHolder(v);
                    }

                    @Override
                    public void onBindViewHolder(@androidx.annotation.NonNull MonthViewHolder holder, int position) {
                        String m = rawMonths.get(position);
                        String display = "Show All".equals(m) ? m : formatMonthLabel(m);
                        holder.tv.setText(display);
                        holder.itemView.setOnClickListener(v -> {
                            viewModel.setSelectedMonth("Show All".equals(m) ? null : m);
                            monthSheet.dismiss();
                        });
                    }

                    @Override
                    public int getItemCount() { return rawMonths.size(); }
                });

                monthSheet.show();
            });
        }).start();
    }

    private static class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        MonthViewHolder(View v) { super(v); tv = v.findViewById(R.id.tvMonthOption); }
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

    private void updateEmptyState(boolean isEmpty) {
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        // Hide FAB only when the sheet is open — always show it otherwise
        fab.setVisibility(View.VISIBLE);
    }

    private List<Object> getDisplayList(List<Expense> expenses) {
        List<Object> displayList = new ArrayList<>();
        if (expenses == null || expenses.isEmpty()) return displayList;

        String lastMonth = "";
        for (Expense e : expenses) {
            String currentMonth = monthLabelOf(e);
            if (!currentMonth.equals(lastMonth)) {
                displayList.add(currentMonth);
                lastMonth = currentMonth;
            }
            displayList.add(e);
        }
        return displayList;
    }

    private String monthLabelOf(Expense e) {
        String date = e.getDate();
        String monthPart = date.length() >= 7 ? date.substring(0, 7) : date;
        if (monthPart.length() < 7) return monthPart;
        try {
            String[] months = {
                    "January", "February", "March", "April",
                    "May", "June", "July", "August",
                    "September", "October", "November", "December"
            };
            int m = Integer.parseInt(monthPart.substring(5, 7)) - 1;
            String y = monthPart.substring(0, 4);
            return months[m] + " " + y;
        } catch (Exception ex) {
            return monthPart;
        }
    }

    private void setTodayAsDefault() {
        Calendar cal = Calendar.getInstance();
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(cal.getTime());
        String display = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(cal.getTime());
        if (etDate != null) etDate.setText(display);
    }

    private void clearForm() {
        etTitle.setText("");
        etAmount.setText("");
        spinnerCategory.setText(CATEGORIES[0], false);
        setTodayAsDefault();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}