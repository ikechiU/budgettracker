package com.iykeafrica.budgettracker.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class Expense {

    @PrimaryKey
    @NonNull
    private String id;

    private String userId;
    private String title;
    private double amount;
    private String date;        // "YYYY-MM-DD"
    private String category;
    @NonNull
    @ColumnInfo(defaultValue = "")
    private String notes;       // optional free-text note
    private boolean isSynced;

    public Expense(@NonNull String id, String userId, String title,
                   double amount, String date, String category,
                   String notes, boolean isSynced) {
        this.id       = id;
        this.userId   = userId;
        this.title    = title;
        this.amount   = amount;
        this.date     = date;
        this.category = category;
        this.notes    = notes != null ? notes : "";
        this.isSynced = isSynced;
    }

    @NonNull public String getId()        { return id; }
    public String getUserId()             { return userId; }
    public String getTitle()              { return title; }
    public double getAmount()             { return amount; }
    public String getDate()               { return date; }
    public String getCategory()           { return category; }
    public String getNotes()              { return notes != null ? notes : ""; }
    public boolean isSynced()             { return isSynced; }

    public void setId(@NonNull String id) { this.id = id; }
    public void setUserId(String userId)  { this.userId = userId; }
    public void setTitle(String title)    { this.title = title; }
    public void setAmount(double amount)  { this.amount = amount; }
    public void setDate(String date)      { this.date = date; }
    public void setCategory(String cat)   { this.category = cat; }
    public void setNotes(String notes)    { this.notes = notes != null ? notes : ""; }
    public void setSynced(boolean synced) { this.isSynced = synced; }
}
