package com.iykeafrica.budgettracker;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * The full JSON response from a GET request to Apps Script.
 * Example: {"status":"ok","expenses":[...]}
 */
public class FetchResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("expenses")
    private List<SheetExpense> expenses;

    @SerializedName("message")
    private String message;

    // Getters
    public String getStatus()                  { return status; }
    public List<SheetExpense> getExpenses()    { return expenses; }
    public String getMessage()                 { return message; }
}