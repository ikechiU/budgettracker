package com.iykeafrica.budgettracker;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO for the expenses table.
 *
 * Changes from v1:
 *  - Every SELECT now filters by userId
 *  - Added getExpensesByMonth() for the grouped list
 *  - Added getTotalForMonth() for monthly summary
 */
@Dao
public interface ExpenseDao {

    // ── Write ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(Expense expense);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<Expense> expenses);

    @Query("DELETE FROM expenses WHERE id = :id")
    void deleteById(String id);

    @Query("UPDATE expenses SET isSynced = 1 WHERE id = :id")
    void markAsSynced(String id);

    // ── Read — filtered by userId ──────────────────────────────────────────

    /**
     * Live list of ALL expenses for this device, newest first.
     * The UI observes this — updates automatically when Room changes.
     */
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<Expense>> getAllExpenses(String userId);

    /**
     * One-shot fetch — used during background sync.
     */
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    List<Expense> getAllExpensesSync(String userId);

    /**
     * Rows not yet pushed to Google Sheets for this device.
     */
    @Query("SELECT * FROM expenses WHERE userId = :userId AND isSynced = 0")
    List<Expense> getUnsyncedExpenses(String userId);

    /**
     * All expenses for a specific month for this device.
     * monthPrefix format: "2026-05" (first 7 chars of the date column).
     */
    @Query("SELECT * FROM expenses WHERE userId = :userId " +
            "AND substr(date, 1, 7) = :monthPrefix " +
            "ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesByMonth(String userId, String monthPrefix);

    /**
     * Sum of all expenses for a specific month for this device.
     * Returns 0.0 if no rows match.
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses " +
            "WHERE userId = :userId AND substr(date, 1, 7) = :monthPrefix")
    double getTotalForMonth(String userId, String monthPrefix);

    /**
     * Returns all distinct YYYY-MM month strings this device has data for.
     * Used to build the month filter spinner.
     */
    @Query("SELECT DISTINCT substr(date, 1, 7) FROM expenses " +
            "WHERE userId = :userId ORDER BY date DESC")
    List<String> getAvailableMonths(String userId);

    /**
     * Sum of ALL expenses ever for this device.
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId")
    double getGrandTotal(String userId);
}