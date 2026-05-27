package com.iykeafrica.budgettracker;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for one expense row arriving from Google Sheets.
 * Now includes userId and category.
 */
public class SheetExpense {

    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("title")
    private String title;

    @SerializedName("amount")
    private double amount;

    @SerializedName("date")
    private String date;

    @SerializedName("category")
    private String category;

    // Getters
    public String getId()       { return id; }
    public String getUserId()   { return userId; }
    public String getTitle()    { return title; }
    public double getAmount()   { return amount; }
    public String getDate()     { return date; }
    public String getCategory() { return category != null ? category : "Other"; }
}