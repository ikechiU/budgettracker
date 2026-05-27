package com.iykeafrica.budgettracker;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

/**
 * RecyclerView adapter for the expense list.
 * Handles both Month Headers (Strings) and Expense items.
 */
public class ExpenseAdapter extends ListAdapter<Object, RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER  = 0;
    private static final int TYPE_EXPENSE = 1;

    public interface OnDeleteClickListener {
        void onDeleteClick(Expense expense);
    }

    public interface SymbolProvider {
        String getSymbol();
    }

    private final OnDeleteClickListener deleteListener;
    private final SymbolProvider symbolProvider;

    public ExpenseAdapter(OnDeleteClickListener deleteListener,
                          SymbolProvider symbolProvider) {
        super(DIFF_CALLBACK);
        this.deleteListener = deleteListener;
        this.symbolProvider = symbolProvider;
    }

    private static final DiffUtil.ItemCallback<Object> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Object>() {
                @Override
                public boolean areItemsTheSame(@NonNull Object o, @NonNull Object n) {
                    if (o instanceof Expense && n instanceof Expense) {
                        return ((Expense) o).getId().equals(((Expense) n).getId());
                    }
                    return o.equals(n);
                }

                @SuppressLint("DiffUtilEquals")
                @Override
                public boolean areContentsTheSame(@NonNull Object o, @NonNull Object n) {
                    return o.equals(n);
                }
            };

    @Override
    public int getItemViewType(int position) {
        if (getItem(position) instanceof String) {
            return TYPE_HEADER;
        }
        return TYPE_EXPENSE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_month_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_expense, parent, false);
            return new ExpenseViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = getItem(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) item);
        } else if (holder instanceof ExpenseViewHolder) {
            ((ExpenseViewHolder) holder).bind(
                    (Expense) item,
                    deleteListener,
                    symbolProvider.getSymbol()
            );
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMonthLabel;

        HeaderViewHolder(@NonNull View v) {
            super(v);
            tvMonthLabel = v.findViewById(R.id.tvMonthLabel);
        }

        void bind(String label) {
            tvMonthLabel.setText(label);
        }
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDate, tvAmount, tvCategory;
        private final ImageButton btnDelete;
        private final View viewSyncDot;

        ExpenseViewHolder(@NonNull View v) {
            super(v);
            tvTitle    = v.findViewById(R.id.tvTitle);
            tvDate     = v.findViewById(R.id.tvDate);
            tvAmount   = v.findViewById(R.id.tvAmount);
            tvCategory = v.findViewById(R.id.tvCategory);
            btnDelete  = v.findViewById(R.id.btnDelete);
            viewSyncDot = v.findViewById(R.id.viewSyncDot);
        }

        void bind(Expense expense, OnDeleteClickListener listener, String symbol) {
            tvTitle.setText(expense.getTitle());
            tvDate.setText(expense.getDate());
            tvCategory.setText(expense.getCategory());
            tvAmount.setText(symbol + String.format(Locale.getDefault(), "%,.2f", expense.getAmount()));
            viewSyncDot.setVisibility(expense.isSynced() ? View.GONE : View.VISIBLE);
            btnDelete.setOnClickListener(v -> listener.onDeleteClick(expense));
        }
    }
}
