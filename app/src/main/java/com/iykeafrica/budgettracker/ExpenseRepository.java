package com.iykeafrica.budgettracker;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

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

/**
 * Single source of truth for all expense data.
 *
 * Changes from v1:
 *  - Reads deviceId from UserPreferences and attaches it to every expense
 *  - All DAO calls now pass userId so queries are scoped to this device
 *  - addExpense() now accepts a category parameter
 *  - pushExpenseToSheets() now sends userId and category to Sheets
 *  - syncFromSheets() now filters pulled rows by userId
 */
public class ExpenseRepository {

    private static final String TAG = "ExpenseRepository";

    private final ExpenseDao dao;
    private final String appsScriptUrl;
    private final String userId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ExpenseRepository(Application application) {
        BudgetDatabase db = BudgetDatabase.getInstance(application);
        this.dao          = db.expenseDao();
        this.appsScriptUrl = BuildConfig.APPS_SCRIPT_URL;

        // Get or create the permanent device ID
        this.userId = new UserPreferences(application).getDeviceId();
        Log.d(TAG, "Repository initialised for userId: " + userId);
    }

    // ── Read ───────────────────────────────────────────────────────────────

    /**
     * Live list of all expenses for THIS device only, newest first.
     */
    public LiveData<List<Expense>> getAllExpenses() {
        return dao.getAllExpenses(userId);
    }

    /**
     * Live list filtered to one specific month.
     * monthPrefix format: "2026-05"
     */
    public LiveData<List<Expense>> getExpensesByMonth(String monthPrefix) {
        return dao.getExpensesByMonth(userId, monthPrefix);
    }

    /**
     * All distinct months this device has expenses in.
     * Runs on the executor — call from a background thread.
     */
    public List<String> getAvailableMonths() {
        return dao.getAvailableMonths(userId);
    }

    // ── Add ────────────────────────────────────────────────────────────────

    /**
     * Creates a new expense with the device's userId and chosen category,
     * saves to Room immediately, then pushes to Sheets in background.
     */
    public void addExpense(String title, double amount, String date, String category) {
        String id = UUID.randomUUID().toString();

        Expense expense = new Expense(
                id,
                userId,       // ← attach this device's ID
                title,
                amount,
                date,
                category,
                false         // not yet synced
        );

        executor.execute(() -> {
            // 1. Save locally — UI refreshes immediately via LiveData
            dao.upsert(expense);

            // 2. Push to Sheets in background
            pushExpenseToSheets(expense);
        });
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    public void deleteExpense(Expense expense) {
        executor.execute(() -> {
            dao.deleteById(expense.getId());
            deleteExpenseFromSheets(expense.getId());
        });
    }

    // ── Sync: pull from Sheets → Room ──────────────────────────────────────

    /**
     * Fetches ALL rows from Sheets, filters to only this device's rows,
     * then upserts them into Room.
     */
    public void syncFromSheets() {
        SheetsApiClient.getService()
                .fetchExpenses(appsScriptUrl, userId)
                .enqueue(new Callback<FetchResponse>() {

                    @Override
                    public void onResponse(Call<FetchResponse> call,
                                           Response<FetchResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Log.w(TAG, "syncFromSheets: bad response " + response.code());
                            return;
                        }

                        FetchResponse body = response.body();
                        if (!"ok".equals(body.getStatus()) || body.getExpenses() == null) {
                            Log.w(TAG, "syncFromSheets: " + body.getMessage());
                            return;
                        }

                        // Filter to only rows belonging to this device
                        List<Expense> myExpenses = new ArrayList<>();
                        for (SheetExpense se : body.getExpenses()) {
                            if (userId.equals(se.getUserId())) {
                                myExpenses.add(new Expense(
                                        se.getId(),
                                        se.getUserId(),
                                        se.getTitle(),
                                        se.getAmount(),
                                        se.getDate(),
                                        se.getCategory(),
                                        true  // came from Sheets — already synced
                                ));
                            }
                        }

                        executor.execute(() -> {
                            dao.upsertAll(myExpenses);
                            Log.d(TAG, "syncFromSheets: upserted "
                                    + myExpenses.size() + " rows for this device");
                        });
                    }

                    @Override
                    public void onFailure(Call<FetchResponse> call, Throwable t) {
                        Log.e(TAG, "syncFromSheets failed: " + t.getMessage());
                    }
                });
    }

    // ── Sync: push unsynced local rows → Sheets ────────────────────────────

    /**
     * Retries any locally saved rows that never made it to Sheets.
     * Called on manual refresh.
     */
    public void syncPendingExpenses() {
        executor.execute(() -> {
            List<Expense> unsynced = dao.getUnsyncedExpenses(userId);
            if (unsynced.isEmpty()) return;

            Log.d(TAG, "syncPendingExpenses: retrying " + unsynced.size() + " rows");
            for (Expense expense : unsynced) {
                pushExpenseToSheets(expense);
            }
        });
    }

    // ── Private: push one expense to Sheets ────────────────────────────────

    private void pushExpenseToSheets(Expense expense) {
        Map<String, Object> body = new HashMap<>();
        body.put("action",   "add");
        body.put("id",       expense.getId());
        body.put("userId",   expense.getUserId());   // ← new
        body.put("title",    expense.getTitle());
        body.put("amount",   expense.getAmount());
        body.put("date",     expense.getDate());
        body.put("category", expense.getCategory()); // ← new

        SheetsApiClient.getService()
                .mutateExpense(appsScriptUrl, body)
                .enqueue(new Callback<MutationResponse>() {

                    @Override
                    public void onResponse(Call<MutationResponse> call,
                                           Response<MutationResponse> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && "ok".equals(response.body().getStatus())) {

                            executor.execute(() -> dao.markAsSynced(expense.getId()));
                            Log.d(TAG, "pushed: " + expense.getId());

                        } else {
                            Log.w(TAG, "push failed for: " + expense.getId()
                                    + " — will retry on next sync");
                        }
                    }

                    @Override
                    public void onFailure(Call<MutationResponse> call, Throwable t) {
                        Log.e(TAG, "pushExpenseToSheets failed: " + t.getMessage());
                    }
                });
    }

    // ── Private: delete one expense from Sheets ────────────────────────────

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
                        if (response.isSuccessful()
                                && response.body() != null
                                && "ok".equals(response.body().getStatus())) {
                            Log.d(TAG, "deleted from Sheets: " + id);
                        } else {
                            Log.w(TAG, "remote delete failed for: " + id);
                        }
                    }

                    @Override
                    public void onFailure(Call<MutationResponse> call, Throwable t) {
                        Log.e(TAG, "deleteExpenseFromSheets failed: " + t.getMessage());
                    }
                });
    }
}