package com.iykeafrica.budgettracker.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.iykeafrica.budgettracker.data.local.CategoryBudget;
import com.iykeafrica.budgettracker.data.local.CategorySpending;
import com.iykeafrica.budgettracker.data.local.Expense;
import com.iykeafrica.budgettracker.data.local.MonthlyBudget;
import com.iykeafrica.budgettracker.data.repository.ExpenseRepository;
import com.iykeafrica.budgettracker.util.UserPreferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetViewModel extends AndroidViewModel {

    public enum ViewMode { BY_MONTH, BY_CATEGORY }

    private final ExpenseRepository repository;
    private final UserPreferences   userPreferences;

    // ── Selected month ─────────────────────────────────────────────────────
    private final MutableLiveData<String> selectedMonth = new MutableLiveData<>();
    public LiveData<String> getSelectedMonth() { return selectedMonth; }

    // ── Expenses ───────────────────────────────────────────────────────────
    private final LiveData<List<Expense>> filteredExpenses;
    public LiveData<List<Expense>> getFilteredExpenses() { return filteredExpenses; }
    public final LiveData<List<Expense>> allExpenses;

    // ── Budget for current month ───────────────────────────────────────────
    private final LiveData<MonthlyBudget> monthlyBudget;
    public LiveData<MonthlyBudget> getMonthlyBudget() { return monthlyBudget; }

    private final LiveData<List<CategoryBudget>> categoryBudgets;
    public LiveData<List<CategoryBudget>> getCategoryBudgets() { return categoryBudgets; }

    // ── Derived summary (budget + actual) ─────────────────────────────────
    private final MediatorLiveData<BudgetSummary> budgetSummary = new MediatorLiveData<>();
    public LiveData<BudgetSummary> getBudgetSummary() { return budgetSummary; }

    // ── View mode ──────────────────────────────────────────────────────────
    private final MutableLiveData<ViewMode> viewMode =
            new MutableLiveData<>(ViewMode.BY_MONTH);
    public LiveData<ViewMode> getViewMode() { return viewMode; }

    // ── Status / sync ──────────────────────────────────────────────────────
    private final MutableLiveData<String>  statusMessage = new MutableLiveData<>();
    public LiveData<String> getStatusMessage() { return statusMessage; }

    private final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsSyncing() { return isSyncing; }

    private final MutableLiveData<String>  currencySymbol = new MutableLiveData<>();
    public LiveData<String> getCurrencySymbol() { return currencySymbol; }

    private final MutableLiveData<Boolean> isSetupDone = new MutableLiveData<>();
    public LiveData<Boolean> getIsSetupDone() { return isSetupDone; }

    // ── Constructor ────────────────────────────────────────────────────────

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        repository      = new ExpenseRepository(application);
        userPreferences = new UserPreferences(application);

        String currentMonth = new java.text.SimpleDateFormat(
                "yyyy-MM", java.util.Locale.getDefault()).format(new java.util.Date());
        selectedMonth.setValue(currentMonth);

        allExpenses = repository.getAllExpenses();

        filteredExpenses = Transformations.switchMap(selectedMonth, month ->
                (month == null || month.isEmpty())
                        ? repository.getAllExpenses()
                        : repository.getExpensesByMonth(month));

        monthlyBudget = Transformations.switchMap(selectedMonth,
                month -> repository.getMonthlyBudget(
                        month != null ? month : currentMonth));

        categoryBudgets = Transformations.switchMap(selectedMonth,
                month -> repository.getCategoryBudgets(
                        month != null ? month : currentMonth));

        // BudgetSummary is derived from filteredExpenses + monthlyBudget + categoryBudgets
        budgetSummary.addSource(filteredExpenses,    expenses  -> recomputeSummary());
        budgetSummary.addSource(monthlyBudget,       budget    -> recomputeSummary());
        budgetSummary.addSource(categoryBudgets,     catBudgets -> recomputeSummary());

        currencySymbol.setValue(userPreferences.getCurrencySymbol());
        isSetupDone.setValue(userPreferences.isSetupDone());

        isSyncing.setValue(true);
        repository.syncFromSheets(() -> isSyncing.setValue(false));
    }

    // ── Actions ────────────────────────────────────────────────────────────

    public void setSelectedMonth(String month) { selectedMonth.setValue(month); }

    public List<String> getAvailableMonths() { return repository.getAvailableMonths(); }

    public String addExpense(String title, String amountText,
                             String date, String category, String notes) {
        if (title == null || title.trim().isEmpty())       return "Please enter a title";
        double amount;
        try { amount = Double.parseDouble(amountText.trim()); }
        catch (NumberFormatException e)                    { return "Please enter a valid amount"; }
        if (amount <= 0)                                   return "Amount must be greater than zero";
        if (date == null || date.trim().isEmpty())         return "Please select a date";
        if (category == null || category.trim().isEmpty()) return "Please select a category";

        repository.addExpense(title.trim(), amount, date, category,
                              notes != null ? notes.trim() : "");
        return null;
    }

    public void updateExpense(Expense expense) { repository.updateExpense(expense); }

    public void deleteExpense(Expense expense) { repository.deleteExpense(expense); }

    public void setMonthlyBudget(double total, Map<String, Double> categoryAllocations) {
        String month = selectedMonth.getValue();
        if (month == null) return;
        repository.saveMonthlyBudget(total, categoryAllocations, month);
    }

    public void toggleViewMode() {
        viewMode.setValue(viewMode.getValue() == ViewMode.BY_MONTH
                ? ViewMode.BY_CATEGORY : ViewMode.BY_MONTH);
    }

    public void saveCurrency(String code, String symbol, String countryName) {
        userPreferences.saveCurrency(code, symbol, countryName);
        currencySymbol.setValue(symbol);
        isSetupDone.setValue(true);
    }

    public void onManualRefresh() {
        isSyncing.setValue(true);
        repository.syncPendingExpenses();
        repository.syncFromSheets(() -> {
            isSyncing.setValue(false);
            statusMessage.setValue("Synced with Google Sheets ✓");
        });
    }

    public void onStatusMessageShown() { statusMessage.setValue(null); }

    public String getCurrentSymbol() { return userPreferences.getCurrencySymbol(); }
    public String getDeviceId()      { return userPreferences.getDeviceId(); }

    // ── Private ────────────────────────────────────────────────────────────

    private void recomputeSummary() {
        List<Expense>       expenses   = filteredExpenses.getValue();
        MonthlyBudget       budget     = monthlyBudget.getValue();
        List<CategoryBudget> catBudgets = categoryBudgets.getValue();

        // Compute totalSpent and spentByCategory from current expense list
        double totalSpent = 0;
        Map<String, Double> spentByCategory = new HashMap<>();
        if (expenses != null) {
            for (Expense e : expenses) {
                totalSpent += e.getAmount();
                spentByCategory.merge(e.getCategory(), e.getAmount(), Double::sum);
            }
        }

        double totalBudget = budget != null ? budget.getTotalBudget() : 0;

        Map<String, Double> budgetByCategory = new HashMap<>();
        if (catBudgets != null) {
            for (CategoryBudget cb : catBudgets) {
                budgetByCategory.put(cb.getCategory(), cb.getBudgetAmount());
            }
        }

        budgetSummary.setValue(new BudgetSummary(
                totalBudget, totalSpent, spentByCategory, budgetByCategory));
    }
}
