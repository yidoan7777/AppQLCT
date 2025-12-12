package com.example.appqlct.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.model.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CategoryViewAdapter - Adapter cho RecyclerView hiển thị danh sách categories (User)
 * Chỉ hiển thị, không có chức năng xóa/sửa
 */
public class CategoryViewAdapter extends RecyclerView.Adapter<CategoryViewAdapter.ViewHolder> {
    private List<Category> categories;
    private OnCategoryClickListener listener;
    private OnCategoryLongClickListener longClickListener;
    // Map category name -> budget amount
    private Map<String, Double> categoryBudgets;
    // Map category name -> spent amount
    private Map<String, Double> categorySpent;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }
    
    public interface OnCategoryLongClickListener {
        boolean onCategoryLongClick(Category category, boolean hasBudget);
    }

    public CategoryViewAdapter(List<Category> categories) {
        this.categories = categories;
        this.categoryBudgets = new HashMap<>();
        this.categorySpent = new HashMap<>();
    }
    
    /**
     * Cập nhật thông tin ngân sách và chi tiêu cho các categories
     */
    public void updateBudgetData(Map<String, Double> budgets, Map<String, Double> spent) {
        this.categoryBudgets = budgets != null ? budgets : new HashMap<>();
        this.categorySpent = spent != null ? spent : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }
    
    public void setOnCategoryLongClickListener(OnCategoryLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        boolean hasBudget = holder.bind(category);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onCategoryLongClick(category, hasBudget);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryName;
        private TextView tvCategoryType;
        private TextView tvBudgetAmount;
        private TextView tvRemainingAmount;
        private CardView cardView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCategoryType = itemView.findViewById(R.id.tvCategoryType);
            tvBudgetAmount = itemView.findViewById(R.id.tvBudgetAmount);
            tvRemainingAmount = itemView.findViewById(R.id.tvRemainingAmount);
            cardView = itemView.findViewById(R.id.cardView);
        }

        boolean bind(Category category) {
            tvCategoryName.setText(category.getName());
            boolean hasBudget = false;

            // Display type with different colors
            if ("income".equals(category.getType())) {
                tvCategoryType.setText(itemView.getContext().getString(R.string.income));
                tvCategoryType.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_color));
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.income_bg));
                // Hide budget information for income categories
                tvBudgetAmount.setVisibility(View.GONE);
                tvRemainingAmount.setVisibility(View.GONE);
            } else {
                tvCategoryType.setText(itemView.getContext().getString(R.string.expense));
                tvCategoryType.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_color));
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_bg));
                
                // Display budget information for expense categories
                String categoryName = category.getName();
                Double budget = categoryBudgets.get(categoryName);
                Double spent = categorySpent.getOrDefault(categoryName, 0.0);
                
                if (budget != null && budget > 0) {
                    hasBudget = true;
                    // Has budget, display information
                    tvBudgetAmount.setVisibility(View.VISIBLE);
                    tvRemainingAmount.setVisibility(View.VISIBLE);
                    
                    tvBudgetAmount.setText(itemView.getContext().getString(R.string.budget_label, formatAmount(budget)));
                    
                    double remaining = budget - spent;
                    tvRemainingAmount.setText(itemView.getContext().getString(R.string.remaining_label, formatAmount(remaining)));
                    
                    // Đổi màu dựa trên số tiền còn lại
                    if (remaining < 0) {
                        // Vượt quá ngân sách
                        tvRemainingAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_color));
                    } else if (remaining < budget * 0.2) {
                        // Còn ít hơn 20% ngân sách
                        tvRemainingAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_orange_dark));
                    } else {
                        // Còn nhiều
                        tvRemainingAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_color));
                    }
                } else {
                    // Chưa có ngân sách
                    tvBudgetAmount.setVisibility(View.GONE);
                    tvRemainingAmount.setVisibility(View.GONE);
                }
            }
            return hasBudget;
        }
        
        private String formatAmount(double amount) {
            return String.format(Locale.getDefault(), "%,.0f VND", amount);
        }
    }
}

