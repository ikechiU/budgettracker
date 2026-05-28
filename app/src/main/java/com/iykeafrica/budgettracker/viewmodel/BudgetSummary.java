package com.iykeafrica.budgettracker.viewmodel;

import java.util.Map;

/** Derived value object combining budget + actual spend for one month. */
public class BudgetSummary {

    public final double totalBudget;
    public final double totalSpent;
    public final double remaining;       // negative = over budget
    public final int    percentUsed;     // 0–∞
    public final Map<String, Double> spentByCategory;
    public final Map<String, Double> budgetByCategory;

    public BudgetSummary(double totalBudget, double totalSpent,
                         Map<String, Double> spentByCategory,
                         Map<String, Double> budgetByCategory) {
        this.totalBudget      = totalBudget;
        this.totalSpent       = totalSpent;
        this.remaining        = totalBudget - totalSpent;
        this.percentUsed      = totalBudget > 0
                ? (int) Math.round((totalSpent / totalBudget) * 100) : 0;
        this.spentByCategory  = spentByCategory;
        this.budgetByCategory = budgetByCategory;
    }

    public boolean hasBudget() { return totalBudget > 0; }
}
