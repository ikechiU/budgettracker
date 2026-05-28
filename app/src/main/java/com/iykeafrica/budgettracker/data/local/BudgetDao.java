package com.iykeafrica.budgettracker.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BudgetDao {

    // ── Monthly budget ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertMonthlyBudget(MonthlyBudget budget);

    @Query("SELECT * FROM monthly_budgets WHERE userId = :userId AND monthYear = :monthYear LIMIT 1")
    LiveData<MonthlyBudget> getMonthlyBudget(String userId, String monthYear);

    @Query("SELECT * FROM monthly_budgets WHERE userId = :userId AND monthYear = :monthYear LIMIT 1")
    MonthlyBudget getMonthlyBudgetSync(String userId, String monthYear);

    // ── Category budgets ───────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertCategoryBudgets(List<CategoryBudget> budgets);

    @Query("SELECT * FROM category_budgets WHERE userId = :userId AND monthYear = :monthYear")
    LiveData<List<CategoryBudget>> getCategoryBudgets(String userId, String monthYear);

    @Query("DELETE FROM category_budgets WHERE userId = :userId AND monthYear = :monthYear")
    void deleteCategoryBudgetsForMonth(String userId, String monthYear);
}
