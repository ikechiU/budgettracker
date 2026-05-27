package com.iykeafrica.budgettracker;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Singleton that creates and holds the Retrofit instance.
 * Call SheetsApiClient.getService() anywhere you need to make a network call.
 */
public class SheetsApiClient {

    private static SheetsApiService SERVICE;

    public static SheetsApiService getService() {
        if (SERVICE == null) {

            // Logs every HTTP request and response to Logcat — very useful for debugging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build();

            // Base URL must end with "/" — the actual full URL is passed per-call via @Url
            SERVICE = new Retrofit.Builder()
                    .baseUrl("https://script.google.com/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(SheetsApiService.class);
        }
        return SERVICE;
    }
}