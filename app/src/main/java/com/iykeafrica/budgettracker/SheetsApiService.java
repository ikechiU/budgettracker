package com.iykeafrica.budgettracker;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Retrofit interface for the Apps Script Web App.
 *
 * Changes from v1:
 *  - fetchExpenses now passes userId as a query parameter
 *    so the script only returns this device's rows
 */
public interface SheetsApiService {

    /**
     * Fetch expenses filtered by userId.
     * Apps Script reads e.parameter.userId in doGet().
     */
    @GET
    Call<FetchResponse> fetchExpenses(
            @Url String url,
            @Query("userId") String userId
    );

    @POST
    Call<MutationResponse> mutateExpense(
            @Url String url,
            @Body Map<String, Object> body
    );
}