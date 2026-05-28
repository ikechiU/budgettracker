package com.iykeafrica.budgettracker.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/** Singleton Retrofit instance. Call SheetsApiClient.getService() to make network calls. */
public class SheetsApiClient {

    private static SheetsApiService SERVICE;

    public static SheetsApiService getService() {
        if (SERVICE == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build();

            // Base URL must end with "/" — full URL passed per-call via @Url
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
