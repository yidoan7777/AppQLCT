package com.example.appqlct.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.model.User;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * UserReportAdapter - Adapter cho RecyclerView hiển thị danh sách users với thống kê (Admin Report)
 */
public class UserReportAdapter extends RecyclerView.Adapter<UserReportAdapter.ViewHolder> {
    private List<UserReportItem> reportItems;
    private OnUserReportClickListener listener;

    public interface OnUserReportClickListener {
        void onUserReportClick(UserReportItem item);
    }

    public UserReportAdapter(List<UserReportItem> reportItems, OnUserReportClickListener listener) {
        this.reportItems = reportItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserReportItem item = reportItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return reportItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUserName, tvUserEmail, tvUserRole;
        private TextView tvUserExpense, tvUserBudget, tvUserTransactionCount;
        private TextView tvViewDetails;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            tvUserExpense = itemView.findViewById(R.id.tvUserExpense);
            tvUserBudget = itemView.findViewById(R.id.tvUserBudget);
            tvUserTransactionCount = itemView.findViewById(R.id.tvUserTransactionCount);
            tvViewDetails = itemView.findViewById(R.id.tvViewDetails);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserReportClick(reportItems.get(getAdapterPosition()));
                }
            });

            tvViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserReportClick(reportItems.get(getAdapterPosition()));
                }
            });
        }

        void bind(UserReportItem item) {
            User user = item.getUser();
            tvUserName.setText(user.getName());
            tvUserEmail.setText(user.getEmail());
            tvUserRole.setText(user.getRole().equals("admin") ? "Admin" : "User");
            
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.getDefault());
            tvUserExpense.setText(formatAmount(item.getTotalExpense()));
            tvUserBudget.setText(formatAmount(item.getTotalBudget()));
            tvUserTransactionCount.setText(String.valueOf(item.getTransactionCount()));
        }

        private String formatAmount(double amount) {
            return String.format(Locale.getDefault(), "%,.0f VND", amount);
        }
    }

    /**
     * Class để lưu thông tin báo cáo của một user
     */
    public static class UserReportItem {
        private User user;
        private double totalExpense;
        private double totalBudget;
        private int transactionCount;

        public UserReportItem(User user, double totalExpense, double totalBudget, int transactionCount) {
            this.user = user;
            this.totalExpense = totalExpense;
            this.totalBudget = totalBudget;
            this.transactionCount = transactionCount;
        }

        public User getUser() {
            return user;
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

