package com.iykeafrica.budgettracker.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.iykeafrica.budgettracker.BuildConfig;
import com.iykeafrica.budgettracker.data.local.BudgetDao;
import com.iykeafrica.budgettracker.data.local.BudgetDatabase;
import com.iykeafrica.budgettracker.data.local.CategoryBudget;
import com.iykeafrica.budgettracker.data.local.CategorySpending;
import com.iykeafrica.budgettracker.data.local.Expense;
import com.iykeafrica.budgettracker.data.local.ExpenseDao;
import com.iykeafrica.budgettracker.data.local.MonthlyBudget;
import com.iykeafrica.budgettracker.data.remote.FetchResponse;
import com.iykeafrica.budgettracker.data.remote.MutationResponse;
import com.iykeafrica.budgettracker.data.remote.SheetExpense;
import com.iykeafrica.budgettracker.data.remote.SheetsApiClient;
import com.iykeafrica.budgettracker.util.UserPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExpenseRepository {

    private static final String TAG = "ExpenseRepository";

    private final ExpenseDao expenseDao;
    private final BudgetDao  budgetDao;
    private final String     appsScriptUrl;
    private final String     userId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ExpenseRepository(Application application) {
        BudgetDatabase db  = BudgetDatabase.getInstance(application);
        this.expenseDao    = db.expenseDao();
        this.budgetDao     = db.budgetDao();
        this.appsScriptUrl = BuildConfig.APPS_SCRIPT_URL;
        this.userId        = new UserPreferences(application).getDeviceId();
        Log.d(TAG, "Repository initialised for userId: " + userId);
    }

    // ── Expenses — read ────────────────────────────────────────────────────

    public LiveData<List<Expense>> getAllExpenses() {
        return expenseDao.getAllExpenses(userId);
    }

    public LiveData<List<Expense>> getExpensesByMonth(String monthPrefix) {
        return expenseDao.getExpensesByMonth(userId, monthPrefix);
    }

    public List<String> getAvailableMonths() {
        return expenseDao.getAvailableMonths(userId);
    }

    public List<CategorySpending> getTotalSpentByCategory(String monthYear) {
        return expenseDao.getTotalSpentByCategory(userId, monthYear);
    }

    // ── Expenses — write ───────────────────────────────────────────────────

    public void addExpense(String title, double amount, String date,
                           String category, String notes) {
        String id = UUID.randomUUID().toString();
        Expense expense = new Expense(id, userId, title, amount, date, category, notes, false);

        executor.execute(() -> {
            expenseDao.upsert(expense);
            pushExpenseToSheets(expense);
        });
    }

    public void updateExpense(Expense expense) {
        expense.setSynced(false);
        executor.execute(() -> {
            expenseDao.upsert(expense);
            pushExpenseToSheets(expense);
        });
    }

    public void deleteExpense(Expense expense) {
        executor.execute(() -> {
            expenseDao.deleteById(expense.getId());
            deleteExpenseFromSheets(expense.getId());
        });
    }

    // ── Budget ─────────────────────────────────────────────────────────────

    public LiveData<MonthlyBudget> getMonthlyBudget(String monthYear) {
        return budgetDao.getMonthlyBudget(userId, monthYear);
    }

    public LiveData<List<CategoryBudget>> getCategoryBudgets(String monthYear) {
        return budgetDao.getCategoryBudgets(userId, monthYear);
    }

    public void saveMonthlyBudget(double totalBudget, Map<String, Double> categoryAllocations,
                                  String monthYear) {
        executor.execute(() -> {
            String budgetId = userId + "_" + monthYear;
            budgetDao.upsertMonthlyBudget(
                    new MonthlyBudget(budgetId, userId, monthYear, totalBudget));

            budgetDao.deleteCategoryBudgetsForMonth(userId, monthYear);

            List<CategoryBudget> cats = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categoryAllocations.entrySet()) {
                if (entry.getValue() > 0) {
                    String catId = userId + "_" + monthYear + "_" + entry.getKey();
                    cats.add(new CategoryBudget(catId, userId, monthYear,
                                                entry.getKey(), entry.getValue()));
                }
            }
            if (!cats.isEmpty()) {
                budgetDao.upsertCategoryBudgets(cats);
            }
        });
    }

    // ── Sync ───────────────────────────────────────────────────────────────

    public void syncFromSheets(Runnable onComplete) {
        SheetsApiClient.getService()
                .fetchExpenses(appsScriptUrl, userId)
                .enqueue(new Callback<FetchResponse>() {
                    @Override
                    public void onResponse(Call<FetchResponse> call,
                                           Response<FetchResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Log.w(TAG, "syncFromSheets: bad response " + response.code());
                            notifyComplete(onComplete);
                            return;
                        }
                        FetchResponse body = response.body();
                        if (!"ok".equals(body.getStatus()) || body.getExpenses() == null) {
                            Log.w(TAG, "syncFromSheets: " + body.getMessage());
                            notifyComplete(onComplete);
                            return;
                        }
                        List<Expense> myExpenses = new ArrayList<>();
                        for (SheetExpense se : body.getExpenses()) {
                            if (userId.equals(se.getUserId())) {
                                myExpenses.add(new Expense(
                                        se.getId(), se.getUserId(), se.getTitle(),
                                        se.getAmount(), normalizeDate(se.getDate()),
                                        se.getCategory(), "", true));
                            }
                        }
                        executor.execute(() -> {
                            expenseDao.upsertAll(myExpenses);
                            Log.d(TAG, "syncFromSheets: upserted " + myExpenses.size() + " rows");
                            notifyComplete(onComplete);
                        });
                    }

                    @Override
                    public void onFailure(Call<FetchResponse> call, Throwable t) {
                        Log.e(TAG, "syncFromSheets failed: " + t.getMessage());
                        notifyComplete(onComplete);
                    }
                });
    }

    /**
     * Converts whatever date string Sheets returns into yyyy-MM-dd.
     * Sheets returns JS Date.toString() e.g. "Wed May 28 2026 00:00:00 GMT+0100 (West Africa Time)".
     */
    private static String normalizeDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        raw = raw.trim();
        // Already ISO
        if (raw.matches("\\d{4}-\\d{2}-\\d{2}.*")) return raw.substring(0, 10);
        // JS Date.toString(): "EEE MMM d yyyy HH:mm:ss ..."  e.g. "Wed May 28 2026 00:00:00 ..."
        try {
            String[] parts = raw.split("\\s+");
            if (parts.length >= 4) {
                String candidate = parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3];
                Date parsed = new SimpleDateFormat("EEE MMM d yyyy", Locale.ENGLISH).parse(candidate);
                if (parsed != null)
                    return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsed);
            }
        } catch (Exception ignored) {}
        return raw;
    }

    private void notifyComplete(Runnable onComplete) {
        if (onComplete != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
        }
    }

    public void syncPendingExpenses() {
        executor.execute(() -> {
            List<Expense> unsynced = expenseDao.getUnsyncedExpenses(userId);
            if (unsynced.isEmpty()) return;
            for (Expense expense : unsynced) pushExpenseToSheets(expense);
        });
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private void pushExpenseToSheets(Expense expense) {
        Map<String, Object> body = new HashMap<>();
        body.put("action",   "add");
        body.put("id",       expense.getId());
        body.put("userId",   expense.getUserId());
        body.put("title",    expense.getTitle());
        body.put("amount",   expense.getAmount());
        body.put("date",     expense.getDate());
        body.put("category", expense.getCategory());
        body.put("notes",    expense.getNotes());

        SheetsApiClient.getService()
                .mutateExpense(appsScriptUrl, body)
                .enqueue(new Callback<MutationResponse>() {
                    @Override
                    public void onResponse(Call<MutationResponse> call,
                                           Response<MutationResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && "ok".equals(response.body().getStatus())) {
                            executor.execute(() -> expenseDao.markAsSynced(expense.getId()));
                        } else {
                            Log.w(TAG, "push failed for: " + expense.getId());
                        }
                    }

                    @Override
                    public void onFailure(Call<MutationResponse> call, Throwable t) {
                        Log.e(TAG, "pushExpenseToSheets failed: " + t.getMessage());
                    }
                });
    }

    private void deleteExpenseFromSheets(String id) {
        Map<String, Object> body = new HashMap<>();
        body.put("action", "delete");
        body.put("id",     id);

        SheetsApiClient.getService()
                .mutateExpense(appsScriptUrl, body)
                .enqueue(new Callback<MutationResponse>() {
                    @Override
                    public void onResponse(Call<MutationResponse> call,
                                           Response<MutationResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && "ok".equals(response.body().getStatus())) {
                            Log.d(TAG, "deleted from Sheets: " + id);
                        }
                    }

                    @Override
                    public void onFailure(Call<MutationResponse> call, Throwable t) {
                        Log.e(TAG, "deleteExpenseFromSheets failed: " + t.getMessage());
                    }
                });
    }
}
