package com.iykeafrica.budgettracker.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

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

    // ── Read ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<Expense>> getAllExpenses(String userId);

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    List<Expense> getAllExpensesSync(String userId);

    @Query("SELECT * FROM expenses WHERE userId = :userId AND isSynced = 0")
    List<Expense> getUnsyncedExpenses(String userId);

    @Query("SELECT * FROM expenses WHERE userId = :userId " +
            "AND substr(date, 1, 7) = :monthPrefix ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesByMonth(String userId, String monthPrefix);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses " +
            "WHERE userId = :userId AND substr(date, 1, 7) = :monthPrefix")
    double getTotalForMonth(String userId, String monthPrefix);

    @Query("SELECT DISTINCT substr(date, 1, 7) FROM expenses " +
            "WHERE userId = :userId ORDER BY date DESC")
    List<String> getAvailableMonths(String userId);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId")
    double getGrandTotal(String userId);

    /** Returns spend per category for a given month — used to build BudgetSummary. */
    @Query("SELECT category, SUM(amount) AS total FROM expenses " +
            "WHERE userId = :userId AND substr(date, 1, 7) = :monthYear " +
            "GROUP BY category")
    List<CategorySpending> getTotalSpentByCategory(String userId, String monthYear);
}
