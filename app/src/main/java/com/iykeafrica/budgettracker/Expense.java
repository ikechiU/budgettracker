package com.iykeafrica.budgettracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Represents one expense.
 *
 * Changes from v1:
 *  - Added userId  : separates each device's rows in Google Sheets
 *  - Added category: e.g. "Food", "Transport", "Housing"
 */
@Entity(tableName = "expenses")
public class Expense {

    @PrimaryKey
    @NonNull
    private String id;

    private String userId;      // device UUID — links row to this device
    private String title;
    private double amount;
    private String date;        // "YYYY-MM-DD"
    private String category;    // "Food", "Transport", etc.
    private boolean isSynced;

    // ── Constructor ────────────────────────────────────────────────────────

    public Expense(@NonNull String id, String userId, String title,
                   double amount, String date, String category, boolean isSynced) {
        this.id       = id;
        this.userId   = userId;
        this.title    = title;
        this.amount   = amount;
        this.date     = date;
        this.category = category;
        this.isSynced = isSynced;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    @NonNull
    public String getId()       { return id; }
    public String getUserId()   { return userId; }
    public String getTitle()    { return title; }
    public double getAmount()   { return amount; }
    public String getDate()     { return date; }
    public String getCategory() { return category; }
    public boolean isSynced()   { return isSynced; }

    // ── Setters ────────────────────────────────────────────────────────────

    public void setId(@NonNull String id)   { this.id = id; }
    public void setUserId(String userId)    { this.userId = userId; }
    public void setTitle(String title)      { this.title = title; }
    public void setAmount(double amount)    { this.amount = amount; }
    public void setDate(String date)        { this.date = date; }
    public void setCategory(String cat)     { this.category = cat; }
    public void setSynced(boolean synced)   { this.isSynced = synced; }
}