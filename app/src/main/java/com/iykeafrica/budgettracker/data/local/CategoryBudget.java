package com.iykeafrica.budgettracker.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category_budgets")
public class CategoryBudget {

    @PrimaryKey
    @NonNull
    private String id;          // userId + "_" + monthYear + "_" + category

    private String userId;
    private String monthYear;
    private String category;
    private double budgetAmount;

    public CategoryBudget(@NonNull String id, String userId, String monthYear,
                          String category, double budgetAmount) {
        this.id           = id;
        this.userId       = userId;
        this.monthYear    = monthYear;
        this.category     = category;
        this.budgetAmount = budgetAmount;
    }

    @NonNull public String getId()            { return id; }
    public String getUserId()                 { return userId; }
    public String getMonthYear()              { return monthYear; }
    public String getCategory()               { return category; }
    public double getBudgetAmount()           { return budgetAmount; }

    public void setId(@NonNull String id)     { this.id = id; }
    public void setUserId(String userId)      { this.userId = userId; }
    public void setMonthYear(String m)        { this.monthYear = m; }
    public void setCategory(String c)         { this.category = c; }
    public void setBudgetAmount(double b)     { this.budgetAmount = b; }
}
