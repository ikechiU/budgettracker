package com.iykeafrica.budgettracker.util;

import java.util.Arrays;
import java.util.List;

/** Holds the list of all supported currencies. */
public class CurrencyConfig {

    public static class Currency {
        public final String countryName;
        public final String currencyCode;
        public final String symbol;
        public final String flag;

        public Currency(String flag, String countryName, String currencyCode, String symbol) {
            this.flag         = flag;
            this.countryName  = countryName;
            this.currencyCode = currencyCode;
            this.symbol       = symbol;
        }

        @Override
        public String toString() {
            return flag + "  " + countryName + "  (" + currencyCode + ")";
        }
    }

    public static List<Currency> getAll() {
        return Arrays.asList(
                // Africa
                new Currency("🇳🇬", "Nigeria",       "NGN", "₦"),
                new Currency("🇬🇭", "Ghana",          "GHS", "GH₵"),
                new Currency("🇰🇪", "Kenya",          "KES", "KSh"),
                new Currency("🇿🇦", "South Africa",   "ZAR", "R"),
                new Currency("🇪🇹", "Ethiopia",       "ETB", "Br"),
                new Currency("🇹🇿", "Tanzania",       "TZS", "TSh"),
                new Currency("🇺🇬", "Uganda",         "UGX", "USh"),
                new Currency("🇷🇼", "Rwanda",         "RWF", "FRw"),
                new Currency("🇸🇳", "Senegal",        "XOF", "CFA"),
                new Currency("🇨🇮", "Côte d'Ivoire",  "XOF", "CFA"),
                new Currency("🇨🇲", "Cameroon",       "XAF", "FCFA"),
                new Currency("🇿🇲", "Zambia",         "ZMW", "ZK"),
                new Currency("🇿🇼", "Zimbabwe",       "ZWL", "Z$"),
                new Currency("🇲🇦", "Morocco",        "MAD", "MAD"),
                new Currency("🇪🇬", "Egypt",          "EGP", "E£"),
                // Americas
                new Currency("🇺🇸", "United States",  "USD", "$"),
                new Currency("🇨🇦", "Canada",         "CAD", "CA$"),
                new Currency("🇧🇷", "Brazil",         "BRL", "R$"),
                new Currency("🇲🇽", "Mexico",         "MXN", "MX$"),
                new Currency("🇦🇷", "Argentina",      "ARS", "ARS$"),
                // Europe
                new Currency("🇬🇧", "United Kingdom", "GBP", "£"),
                new Currency("🇪🇺", "Euro Zone",      "EUR", "€"),
                new Currency("🇨🇭", "Switzerland",    "CHF", "CHF"),
                new Currency("🇸🇪", "Sweden",         "SEK", "kr"),
                new Currency("🇳🇴", "Norway",         "NOK", "kr"),
                // Asia & Pacific
                new Currency("🇮🇳", "India",          "INR", "₹"),
                new Currency("🇨🇳", "China",          "CNY", "¥"),
                new Currency("🇯🇵", "Japan",          "JPY", "¥"),
                new Currency("🇦🇺", "Australia",      "AUD", "A$"),
                new Currency("🇸🇬", "Singapore",      "SGD", "S$"),
                new Currency("🇦🇪", "UAE",            "AED", "AED"),
                new Currency("🇸🇦", "Saudi Arabia",   "SAR", "SAR")
        );
    }

    /** Finds a Currency by code; falls back to Nigeria if not found. */
    public static Currency findByCode(String code) {
        for (Currency c : getAll()) {
            if (c.currencyCode.equals(code)) return c;
        }
        return getAll().get(0);
    }
}
