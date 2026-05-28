package com.iykeafrica.budgettracker.data.remote;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Full JSON response from a GET to Apps Script: {"status":"ok","expenses":[...]} */
public class FetchResponse {

    @SerializedName("status")   private String status;
    @SerializedName("expenses") private List<SheetExpense> expenses;
    @SerializedName("message")  private String message;

    public String getStatus()               { return status; }
    public List<SheetExpense> getExpenses() { return expenses; }
    public String getMessage()              { return message; }
}
