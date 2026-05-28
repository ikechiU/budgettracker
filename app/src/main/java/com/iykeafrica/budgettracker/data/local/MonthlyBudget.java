package com.iykeafrica.budgettracker.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "monthly_budgets")
public class MonthlyBudget {

    @PrimaryKey
    @NonNull
    private String id;          // userId + "_" + monthYear

    private String userId;
    private String monthYear;   // "yyyy-MM"
    private double totalBudget;

    public MonthlyBudget(@NonNull String id, String userId, String monthYear, double totalBudget) {
        this.id          = id;
        this.userId      = userId;
        this.monthYear   = monthYear;
        this.totalBudget = totalBudget;
    }

    @NonNull public String getId()         { return id; }
    public String getUserId()              { return userId; }
    public String getMonthYear()           { return monthYear; }
    public double getTotalBudget()         { return totalBudget; }

    public void setId(@NonNull String id)  { this.id = id; }
    public void setUserId(String userId)   { this.userId = userId; }
    public void setMonthYear(String m)     { this.monthYear = m; }
    public void setTotalBudget(double b)   { this.totalBudget = b; }
}
