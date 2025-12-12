package com.example.appqlct.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * MonthReportAdapter - Adapter cho RecyclerView hiển thị danh sách các tháng với thống kê
 */
public class MonthReportAdapter extends RecyclerView.Adapter<MonthReportAdapter.ViewHolder> {
    private List<MonthReportItem> monthItems;
    private OnMonthReportClickListener listener;

    public interface OnMonthReportClickListener {
        void onMonthReportClick(MonthReportItem item);
    }

    public MonthReportAdapter(List<MonthReportItem> monthItems, OnMonthReportClickListener listener) {
        this.monthItems = monthItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_month_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonthReportItem item = monthItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return monthItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMonthYear;
        private TextView tvMonthExpense, tvMonthBudget, tvMonthRemaining, tvMonthTransactionCount;
        private TextView tvTopSpenderBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonthYear = itemView.findViewById(R.id.tvMonthYear);
            tvMonthExpense = itemView.findViewById(R.id.tvMonthExpense);
            tvMonthBudget = itemView.findViewById(R.id.tvMonthBudget);
            tvMonthRemaining = itemView.findViewById(R.id.tvMonthRemaining);
            tvMonthTransactionCount = itemView.findViewById(R.id.tvMonthTransactionCount);
            tvTopSpenderBadge = itemView.findViewById(R.id.tvTopSpenderBadge);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMonthReportClick(monthItems.get(getAdapterPosition()));
                }
            });
        }

        void bind(MonthReportItem item) {
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.ENGLISH);
            String monthYearText = monthFormat.format(item.getMonthCalendar().getTime());
            
            // DEBUG: Log giá trị Calendar khi bind
            Calendar cal = item.getMonthCalendar();
            android.util.Log.d("MonthReportAdapter", String.format("Bind item: Calendar year=%d, month=%d, formatted=%s", 
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), monthYearText));
            
            tvMonthYear.setText(monthYearText);
            
            tvMonthExpense.setText(formatAmount(item.getTotalExpense()));
            tvMonthBudget.setText(formatAmount(item.getTotalBudget()));
            
            double remaining = item.getTotalBudget() - item.getTotalExpense();
            tvMonthRemaining.setText(formatAmount(remaining));
            // Đổi màu: đỏ nếu âm, xanh nếu dương
            if (remaining < 0) {
                tvMonthRemaining.setTextColor(itemView.getContext().getResources().getColor(com.example.appqlct.R.color.expense_color));
            } else {
                tvMonthRemaining.setTextColor(itemView.getContext().getResources().getColor(com.example.appqlct.R.color.income_color));
            }
            
            tvMonthTransactionCount.setText(String.valueOf(item.getTransactionCount()));
            
            // Hiển thị badge cho tháng tiêu nhiều nhất (tháng đầu tiên sau khi sắp xếp)
            int position = getAdapterPosition();
            if (position == 0 && monthItems.size() > 0 && item.getTotalExpense() > 0) {
                // Kiểm tra xem có tháng nào khác có chi tiêu bằng nhau không
                boolean isTopSpender = true;
                if (monthItems.size() > 1) {
                    double topExpense = monthItems.get(0).getTotalExpense();
                    // Nếu có nhiều tháng có cùng chi tiêu cao nhất, chỉ hiển thị cho tháng đầu tiên
                    isTopSpender = item.getTotalExpense() == topExpense && position == 0;
                }
                tvTopSpenderBadge.setVisibility(isTopSpender ? View.VISIBLE : View.GONE);
            } else {
                tvTopSpenderBadge.setVisibility(View.GONE);
            }
        }

        private String formatAmount(double amount) {
            return String.format(Locale.getDefault(), "%,.0f VND", amount);
        }
    }

    /**
     * Class để lưu thông tin báo cáo của một tháng
     */
    public static class MonthReportItem {
        private Calendar monthCalendar;
        private double totalExpense;
        private double totalBudget;
        private int transactionCount;

        public MonthReportItem(Calendar monthCalendar, double totalExpense, double totalBudget, int transactionCount) {
            this.monthCalendar = monthCalendar;
            this.totalExpense = totalExpense;
            this.totalBudget = totalBudget;
            this.transactionCount = transactionCount;
        }

        public Calendar getMonthCalendar() {
            return monthCalendar;
        }

        public double getTotalExpense() {
            return totalExpense;
        }

        public double getTotalBudget() {
            return totalBudget;
        }

        public int getTransactionCount() {
            return transactionCount;
        }
    }
}

