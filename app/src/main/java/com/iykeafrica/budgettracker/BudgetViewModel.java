package com.iykeafrica.budgettracker;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

/**
 * ViewModel for the main budget screen.
 *
 * Changes from v1:
 *  - addExpense() now accepts date and category
 *  - Exposes selectedMonth so the UI can filter by month
 *  - currencySymbol exposed so the UI always uses the right symbol
 *  - UserPreferences read here so the UI knows if setup is done
 */
public class BudgetViewModel extends AndroidViewModel {

    private final ExpenseRepository repository;
    private final UserPreferences userPreferences;

    // ── Exposed data ───────────────────────────────────────────────────────

    /**
     * Currently selected month for filtering, format "yyyy-MM".
     * If null, shows all expenses.
     */
    private final MutableLiveData<String> selectedMonth = new MutableLiveData<>();
    public LiveData<String> getSelectedMonth() { return selectedMonth; }

    /** Filtered list of expenses for the selected month or all. */
    private final LiveData<List<Expense>> filteredExpenses;
    public LiveData<List<Expense>> getFilteredExpenses() { return filteredExpenses; }

    /** Full list of all expenses for calculations if needed. */
    public final LiveData<List<Expense>> allExpenses;

    /** One-shot toast messages. */
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    public LiveData<String> getStatusMessage() { return statusMessage; }

    /** True while a manual sync is running. */
    private final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsSyncing() { return isSyncing; }

    /**
     * The currency symbol for the current user e.g. "₦", "$", "£".
     * The UI reads this whenever it formats an amount.
     */
    private final MutableLiveData<String> currencySymbol = new MutableLiveData<>();
    public LiveData<String> getCurrencySymbol() { return currencySymbol; }

    /**
     * True if the user has already picked a currency.
     * False on first launch — triggers the currency picker dialog.
     */
    private final MutableLiveData<Boolean> isSetupDone = new MutableLiveData<>();
    public LiveData<Boolean> getIsSetupDone() { return isSetupDone; }

    // ── Constructor ────────────────────────────────────────────────────────

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        repository      = new ExpenseRepository(application);
        userPreferences = new UserPreferences(application);

        // Set default month to current month "yyyy-MM"
        String currentMonth = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                .format(new java.util.Date());
        selectedMonth.setValue(currentMonth);

        // Wire all expenses
        allExpenses = repository.getAllExpenses();

        // Wire filtered expenses based on selectedMonth
        filteredExpenses = androidx.lifecycle.Transformations.switchMap(selectedMonth, month -> {
            if (month == null || month.isEmpty()) {
                return repository.getAllExpenses();
            } else {
                return repository.getExpensesByMonth(month);
            }
        });

        // Load saved currency symbol
        currencySymbol.setValue(userPreferences.getCurrencySymbol());

        // Check if first launch
        isSetupDone.setValue(userPreferences.isSetupDone());

        // Pull latest data from Sheets on launch
        syncFromSheets();
    }

    // ── Actions called by the UI ───────────────────────────────────────────

    public void setSelectedMonth(String month) {
        selectedMonth.setValue(month);
    }

    public List<String> getAvailableMonths() {
        return repository.getAvailableMonths();
    }

    /**
     * Validates input and adds the expense.
     * Now accepts date (from date picker) and category (from dropdown).
     * Returns an error string if validation fails, null if successful.
     */
    public String addExpense(String title, String amountText,
                             String date, String category) {
        if (title == null || title.trim().isEmpty()) {
            return "Please enter a title";
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText.trim());
        } catch (NumberFormatException e) {
            return "Please enter a valid amount";
        }

        if (amount <= 0) {
            return "Amount must be greater than zero";
        }

        if (date == null || date.trim().isEmpty()) {
            return "Please select a date";
        }

        if (category == null || category.trim().isEmpty()) {
            return "Please select a category";
        }

        repository.addExpense(title.trim(), amount, date, category);
        return null; // null = no error
    }

    /**
     * Deletes an expense.
     */
    public void deleteExpense(Expense expense) {
        repository.deleteExpense(expense);
    }

    /**
     * Called when the user saves their currency selection.
     * Updates SharedPreferences and refreshes the symbol LiveData.
     */
    public void saveCurrency(String code, String symbol, String countryName) {
        userPreferences.saveCurrency(code, symbol, countryName);
        currencySymbol.setValue(symbol);
        isSetupDone.setValue(true);
    }

    /**
     * Manual refresh — push pending rows then pull from Sheets.
     */
    public void onManualRefresh() {
        isSyncing.setValue(true);
        repository.syncPendingExpenses();
        repository.syncFromSheets();

        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> {
                    isSyncing.setValue(false);
                    statusMessage.setValue("Synced with Google Sheets ✓");
                }, 2000);
    }

    /**
     * Called after the UI has shown the toast so it does not repeat on rotation.
     */
    public void onStatusMessageShown() {
        statusMessage.setValue(null);
    }

    /**
     * Returns the saved currency symbol — used by the adapter for formatting.
     */
    public String getCurrentSymbol() {
        return userPreferences.getCurrencySymbol();
    }

    /**
     * Returns the device ID — useful for debugging in Logcat.
     */
    public String getDeviceId() {
        return userPreferences.getDeviceId();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private void syncFromSheets() {
        repository.syncFromSheets();
    }
}