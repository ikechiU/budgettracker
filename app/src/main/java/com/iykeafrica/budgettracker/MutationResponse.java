package com.iykeafrica.budgettracker;

import com.google.gson.annotations.SerializedName;

/**
 * The JSON response from a POST request (add or delete).
 * Example: {"status":"ok"} or {"status":"error","message":"..."}
 */
public class MutationResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    // Getters
    public String getStatus()  { return status; }
    public String getMessage() { return message; }
}