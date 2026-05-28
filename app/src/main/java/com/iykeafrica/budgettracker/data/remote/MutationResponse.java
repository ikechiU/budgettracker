package com.iykeafrica.budgettracker.data.remote;

import com.google.gson.annotations.SerializedName;

/** JSON response from a POST (add or delete): {"status":"ok"} or {"status":"error","message":"..."} */
public class MutationResponse {

    @SerializedName("status")  private String status;
    @SerializedName("message") private String message;

    public String getStatus()  { return status; }
    public String getMessage() { return message; }
}
