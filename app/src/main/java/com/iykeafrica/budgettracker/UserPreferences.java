package com.iykeafrica.budgettracker;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Manages persistent user settings using SharedPreferences.
 *
 * Stores:
 *  - deviceId   : a UUID generated once on first launch, never changes
 *  - currencyCode: e.g. "NGN", "USD", "GBP"
 *  - currencySymbol: e.g. "₦", "$", "£"
 *  - countryName : e.g. "Nigeria", "United States"
 *  - isSetupDone : false until user picks a currency
 */
public class UserPreferences {

    private static final String PREF_FILE       = "budget_tracker_prefs";
    private static final String KEY_DEVICE_ID   = "device_id";
    private static final String KEY_CURRENCY_CODE   = "currency_code";
    private static final String KEY_CURRENCY_SYMBOL = "currency_symbol";
    private static final String KEY_COUNTRY_NAME    = "country_name";
    private static final String KEY_SETUP_DONE      = "setup_done";

    private final SharedPreferences prefs;

    public UserPreferences(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    // ── Device ID ──────────────────────────────────────────────────────────

    /**
     * Returns the device's unique ID.
     * Creates and saves one on the very first call — never changes after that.
     * This is used to separate each user's rows in Google Sheets.
     */
    public String getDeviceId() {
        String id = prefs.getString(KEY_DEVICE_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    // ── Currency ───────────────────────────────────────────────────────────

    public void saveCurrency(String code, String symbol, String countryName) {
        prefs.edit()
                .putString(KEY_CURRENCY_CODE,   code)
                .putString(KEY_CURRENCY_SYMBOL, symbol)
                .putString(KEY_COUNTRY_NAME,    countryName)
                .putBoolean(KEY_SETUP_DONE,     true)
                .apply();
    }

    public String getCurrencyCode()   {
        return prefs.getString(KEY_CURRENCY_CODE, "NGN");
    }

    public String getCurrencySymbol() {
        return prefs.getString(KEY_CURRENCY_SYMBOL, "₦");
    }

    public String getCountryName() {
        return prefs.getString(KEY_COUNTRY_NAME, "Nigeria");
    }

    // ── Setup flag ─────────────────────────────────────────────────────────

    /**
     * Returns false on first launch — triggers the currency picker dialog.
     * Returns true after the user has made their selection.
     */
    public boolean isSetupDone() {
        return prefs.getBoolean(KEY_SETUP_DONE, false);
    }
}