package com.iykeafrica.budgettracker;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen dialog that lets the user pick their country and currency.
 * Shown only once on first launch.
 * Has a search box so users can filter the list quickly.
 */
public class CurrencyPickerDialog extends Dialog {

    // ── Callback ───────────────────────────────────────────────────────────

    public interface OnCurrencySelectedListener {
        void onCurrencySelected(String code, String symbol, String countryName);
    }

    private final OnCurrencySelectedListener listener;

    // ── Constructor ────────────────────────────────────────────────────────

    public CurrencyPickerDialog(@NonNull Context context,
                                OnCurrencySelectedListener listener) {
        super(context);
        this.listener = listener;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_currency_picker);

        // Make dialog full screen
        if (getWindow() != null) {
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        }

        // Non-cancellable — user must pick a currency
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        setupSearch();
        setupList();
    }

    // ── Search ─────────────────────────────────────────────────────────────

    private CurrencyListAdapter adapter;

    private void setupSearch() {
        TextInputEditText etSearch = findViewById(R.id.etCurrencySearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
        });
    }

    private void setupList() {
        RecyclerView recyclerView = findViewById(R.id.rvCurrencies);
        adapter = new CurrencyListAdapter(CurrencyConfig.getAll(), currency -> {
            listener.onCurrencySelected(
                    currency.currencyCode,
                    currency.symbol,
                    currency.countryName
            );
            dismiss();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // ── Inner adapter ──────────────────────────────────────────────────────

    private static class CurrencyListAdapter
            extends RecyclerView.Adapter<CurrencyListAdapter.VH> {

        interface OnPickListener {
            void onPick(CurrencyConfig.Currency currency);
        }

        private final List<CurrencyConfig.Currency> fullList;
        private List<CurrencyConfig.Currency> filteredList;
        private final OnPickListener listener;

        CurrencyListAdapter(List<CurrencyConfig.Currency> list, OnPickListener listener) {
            this.fullList     = list;
            this.filteredList = new ArrayList<>(list);
            this.listener     = listener;
        }

        /** Filters the list by country name or currency code. */
        void filter(String query) {
            filteredList = new ArrayList<>();
            if (query.isEmpty()) {
                filteredList.addAll(fullList);
            } else {
                String lower = query.toLowerCase();
                for (CurrencyConfig.Currency c : fullList) {
                    if (c.countryName.toLowerCase().contains(lower)
                            || c.currencyCode.toLowerCase().contains(lower)
                            || c.symbol.contains(query)) {
                        filteredList.add(c);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_currency, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.bind(filteredList.get(position), listener);
        }

        @Override
        public int getItemCount() { return filteredList.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvFlag, tvCountry, tvCode, tvSymbol;

            VH(@NonNull View v) {
                super(v);
                tvFlag    = v.findViewById(R.id.tvCurrencyFlag);
                tvCountry = v.findViewById(R.id.tvCurrencyCountry);
                tvCode    = v.findViewById(R.id.tvCurrencyCode);
                tvSymbol  = v.findViewById(R.id.tvCurrencySymbol);
            }

            void bind(CurrencyConfig.Currency c, OnPickListener listener) {
                tvFlag.setText(c.flag);
                tvCountry.setText(c.countryName);
                tvCode.setText(c.currencyCode);
                tvSymbol.setText(c.symbol);
                itemView.setOnClickListener(v -> listener.onPick(c));
            }
        }
    }
}