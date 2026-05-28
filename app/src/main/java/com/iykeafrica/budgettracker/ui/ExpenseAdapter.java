package com.iykeafrica.budgettracker.ui;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.iykeafrica.budgettracker.R;
import com.iykeafrica.budgettracker.data.local.Expense;
import com.iykeafrica.budgettracker.viewmodel.BudgetSummary;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Handles three view types:
 *   TYPE_MONTH_HEADER   — month divider (by-month mode)
 *   TYPE_CATEGORY_HEADER — category section header with progress bar (by-category mode)
 *   TYPE_EXPENSE        — individual expense row
 */
public class ExpenseAdapter extends ListAdapter<Object, RecyclerView.ViewHolder> {

    private static final int TYPE_MONTH_HEADER    = 0;
    private static final int TYPE_EXPENSE         = 1;
    private static final int TYPE_CATEGORY_HEADER = 2;

    public interface OnDeleteClickListener  { void onDeleteClick(Expense expense); }
    public interface OnEditClickListener    { void onEditClick(Expense expense); }
    public interface SymbolProvider         { String getSymbol(); }

    private final OnDeleteClickListener deleteListener;
    private final OnEditClickListener   editListener;
    private final SymbolProvider        symbolProvider;

    private BudgetSummary budgetSummary;

    // Categories whose rows are collapsed in BY_CATEGORY mode
    final Set<String> collapsedCategories = new HashSet<>();

    public ExpenseAdapter(OnDeleteClickListener deleteListener,
                          OnEditClickListener editListener,
                          SymbolProvider symbolProvider) {
        super(DIFF_CALLBACK);
        this.deleteListener = deleteListener;
        this.editListener   = editListener;
        this.symbolProvider = symbolProvider;
    }

    public void setBudgetSummary(BudgetSummary summary) {
        this.budgetSummary = summary;
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<Object> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Object>() {
                @Override
                public boolean areItemsTheSame(@NonNull Object o, @NonNull Object n) {
                    if (o instanceof Expense && n instanceof Expense)
                        return ((Expense) o).getId().equals(((Expense) n).getId());
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
        Object item = getItem(position);
        if (item instanceof CategoryHeader) return TYPE_CATEGORY_HEADER;
        if (item instanceof String)         return TYPE_MONTH_HEADER;
        return TYPE_EXPENSE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_CATEGORY_HEADER:
                return new CategoryHeaderViewHolder(
                        inf.inflate(R.layout.item_category_header, parent, false));
            case TYPE_MONTH_HEADER:
                return new MonthHeaderViewHolder(
                        inf.inflate(R.layout.item_month_header, parent, false));
            default:
                return new ExpenseViewHolder(
                        inf.inflate(R.layout.item_expense, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = getItem(position);
        if (holder instanceof CategoryHeaderViewHolder) {
            ((CategoryHeaderViewHolder) holder).bind(
                    (CategoryHeader) item, symbolProvider.getSymbol(), budgetSummary,
                    collapsedCategories, this);
        } else if (holder instanceof MonthHeaderViewHolder) {
            ((MonthHeaderViewHolder) holder).bind((String) item);
        } else if (holder instanceof ExpenseViewHolder) {
            ((ExpenseViewHolder) holder).bind(
                    (Expense) item, deleteListener, editListener, symbolProvider.getSymbol());
        }
    }

    // ── CategoryHeader marker object ───────────────────────────────────────

    public static class CategoryHeader {
        public final String category;
        public CategoryHeader(String category) { this.category = category; }
        @Override public boolean equals(Object o) {
            return o instanceof CategoryHeader && ((CategoryHeader) o).category.equals(category);
        }
        @Override public int hashCode() { return category.hashCode(); }
    }

    // ── View holders ───────────────────────────────────────────────────────

    static class MonthHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMonthLabel;
        MonthHeaderViewHolder(@NonNull View v) {
            super(v);
            tvMonthLabel = v.findViewById(R.id.tvMonthLabel);
        }
        void bind(String label) { tvMonthLabel.setText(label); }
    }

    static class CategoryHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView     tvCategoryName, tvCategorySpent;
        private final TextView     tvCategoryBudgetLabel, tvCategoryPercent;
        private final ProgressBar  progressCategory;
        private final View         layoutCategoryBudget;
        private final ImageView    ivChevron;

        CategoryHeaderViewHolder(@NonNull View v) {
            super(v);
            tvCategoryName        = v.findViewById(R.id.tvCategoryName);
            tvCategorySpent       = v.findViewById(R.id.tvCategorySpent);
            tvCategoryBudgetLabel = v.findViewById(R.id.tvCategoryBudgetLabel);
            tvCategoryPercent     = v.findViewById(R.id.tvCategoryPercent);
            progressCategory      = v.findViewById(R.id.progressCategory);
            layoutCategoryBudget  = v.findViewById(R.id.layoutCategoryBudget);
            ivChevron             = v.findViewById(R.id.ivChevron);
        }

        void bind(CategoryHeader header, String symbol, BudgetSummary summary,
                  Set<String> collapsed, ExpenseAdapter adapter) {
            tvCategoryName.setText(header.category);

            double spent = summary != null && summary.spentByCategory.containsKey(header.category)
                    ? summary.spentByCategory.get(header.category) : 0;
            tvCategorySpent.setText(symbol + String.format(Locale.getDefault(), "%,.2f", spent));

            double catBudget = summary != null && summary.budgetByCategory.containsKey(header.category)
                    ? summary.budgetByCategory.get(header.category) : 0;

            if (catBudget > 0) {
                layoutCategoryBudget.setVisibility(View.VISIBLE);
                int pct = (int) Math.min(Math.round((spent / catBudget) * 100), 100);
                tvCategoryBudgetLabel.setText(
                        "Budget: " + symbol + String.format(Locale.getDefault(), "%,.0f", catBudget));
                tvCategoryPercent.setText(pct + "%");
                progressCategory.setProgress(pct);

                int tint = pct >= 100 ? 0xFFBA1A1A : pct >= 75 ? 0xFFE65100 : 0xFF1B6CA8;
                progressCategory.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(tint));
            } else {
                layoutCategoryBudget.setVisibility(View.GONE);
            }

            boolean isCollapsed = collapsed.contains(header.category);
            ivChevron.setRotation(isCollapsed ? -90f : 0f);

            itemView.setOnClickListener(v -> {
                if (isCollapsed) collapsed.remove(header.category);
                else             collapsed.add(header.category);
                adapter.notifyDataSetChanged();
            });
        }
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDate, tvAmount, tvCategory, tvNotes;
        private final ImageButton btnDelete;
        private final View viewSyncDot;

        ExpenseViewHolder(@NonNull View v) {
            super(v);
            tvTitle    = v.findViewById(R.id.tvTitle);
            tvDate     = v.findViewById(R.id.tvDate);
            tvAmount   = v.findViewById(R.id.tvAmount);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvNotes    = v.findViewById(R.id.tvNotes);
            btnDelete  = v.findViewById(R.id.btnDelete);
            viewSyncDot = v.findViewById(R.id.viewSyncDot);
        }

        void bind(Expense expense, OnDeleteClickListener deleteListener,
                  OnEditClickListener editListener, String symbol) {
            tvTitle.setText(expense.getTitle());
            tvDate.setText(expense.getDate());
            tvAmount.setText(symbol + String.format(Locale.getDefault(), "%,.2f", expense.getAmount()));
            viewSyncDot.setVisibility(expense.isSynced() ? View.GONE : View.VISIBLE);

            String cat = expense.getCategory();
            tvCategory.setText(cat);
            applyChipColor(tvCategory, categoryColor(cat));

            String notes = expense.getNotes();
            if (notes != null && !notes.isEmpty()) {
                tvNotes.setText(notes);
                tvNotes.setVisibility(View.VISIBLE);
            } else {
                tvNotes.setVisibility(View.GONE);
            }

            btnDelete.setOnClickListener(v -> deleteListener.onDeleteClick(expense));
            itemView.setOnLongClickListener(v -> {
                editListener.onEditClick(expense);
                return true;
            });
        }

        private static void applyChipColor(TextView chip, int color) {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(40f);
            bg.setColor(color);
            chip.setBackground(bg);
        }

        private static int categoryColor(String category) {
            if (category == null) return 0xFFA9A9A9;
            switch (category.toLowerCase(Locale.getDefault())) {
                case "food":          return 0xFFE53935;
                case "transport":     return 0xFF00897B;
                case "housing":       return 0xFF039BE5;
                case "health":        return 0xFF43A047;
                case "entertainment": return 0xFF8E24AA;
                case "shopping":      return 0xFFFB8C00;
                case "utilities":     return 0xFF1E88E5;
                default:              return 0xFF757575;
            }
        }
    }
}
