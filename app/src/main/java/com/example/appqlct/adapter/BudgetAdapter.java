package com.example.appqlct.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.model.Budget;

import java.util.List;
import java.util.Locale;

/**
 * BudgetAdapter - Adapter cho RecyclerView hiển thị danh sách ngân sách
 */
public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.ViewHolder> {
    private List<Budget> budgets;
    private List<BudgetSpendingInfo> spendingInfoList; // Thông tin chi tiêu cho mỗi ngân sách
    private OnBudgetClickListener listener;

    public interface OnBudgetClickListener {
        void onBudgetClick(Budget budget);
        void onBudgetDelete(Budget budget);
    }

    public BudgetAdapter(List<Budget> budgets, List<BudgetSpendingInfo> spendingInfoList, 
                        OnBudgetClickListener listener) {
        this.budgets = budgets;
        this.spendingInfoList = spendingInfoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Budget budget = budgets.get(position);
        BudgetSpendingInfo spendingInfo = findSpendingInfo(budget);
        holder.bind(budget, spendingInfo);
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    private BudgetSpendingInfo findSpendingInfo(Budget budget) {
        if (spendingInfoList == null) return null;
        for (BudgetSpendingInfo info : spendingInfoList) {
            if (info.categoryName.equals(budget.getCategoryName())) {
                return info;
            }
        }
        return null;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryName, tvBudgetAmount, tvSpentAmount, tvRemaining, tvProgress;
        private ProgressBar progressBar;
        private Button btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvBudgetAmount = itemView.findViewById(R.id.tvBudgetAmount);
            tvSpentAmount = itemView.findViewById(R.id.tvSpentAmount);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            progressBar = itemView.findViewById(R.id.progressBar);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBudgetClick(budgets.get(getAdapterPosition()));
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBudgetDelete(budgets.get(getAdapterPosition()));
                }
            });
        }

        void bind(Budget budget, BudgetSpendingInfo spendingInfo) {
            tvCategoryName.setText(budget.getCategoryName());
            tvBudgetAmount.setText(formatAmount(budget.getAmount()));

            if (spendingInfo != null) {
                double spent = spendingInfo.spentAmount;
                double remaining = budget.getAmount() - spent;
                double progress = budget.getAmount() > 0 ? (spent / budget.getAmount()) * 100 : 0;

                tvSpentAmount.setText(formatAmount(spent));
                tvRemaining.setText(formatAmount(remaining));
                tvProgress.setText(String.format(Locale.getDefault(), "%.0f%%", progress));

                // Cập nhật progress bar
                progressBar.setMax(100);
                progressBar.setProgress((int) Math.min(100, progress));

                // Đổi màu nếu vượt quá ngân sách
                if (remaining < 0) {
                    tvRemaining.setTextColor(itemView.getContext().getColor(R.color.expense_color));
                    progressBar.setProgressTintList(itemView.getContext().getColorStateList(R.color.expense_color));
                } else if (progress >= 80) {
                    tvRemaining.setTextColor(itemView.getContext().getColor(R.color.expense_color));
                    progressBar.setProgressTintList(itemView.getContext().getColorStateList(R.color.expense_color));
                } else {
                    tvRemaining.setTextColor(itemView.getContext().getColor(R.color.income_color));
                    progressBar.setProgressTintList(itemView.getContext().getColorStateList(R.color.income_color));
                }
            } else {
                tvSpentAmount.setText(formatAmount(0));
                tvRemaining.setText(formatAmount(budget.getAmount()));
                tvProgress.setText("0%");
                progressBar.setProgress(0);
                tvRemaining.setTextColor(itemView.getContext().getColor(R.color.income_color));
            }
        }

        private String formatAmount(double amount) {
            return String.format(Locale.getDefault(), "%,.0f VND", amount);
        }
    }

    /**
     * Class để lưu thông tin chi tiêu cho mỗi category
     */
    public static class BudgetSpendingInfo {
        public String categoryName;
        public double spentAmount;

        public BudgetSpendingInfo(String categoryName, double spentAmount) {
            this.categoryName = categoryName;
            this.spentAmount = spentAmount;
        }
    }
}

