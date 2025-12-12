package com.example.appqlct.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.fragment.user.ReportFragment;

import java.util.List;
import java.util.Locale;

/**
 * Adapter để hiển thị danh sách chi tiết chi tiêu theo danh mục trong báo cáo
 */
public class CategoryReportAdapter extends RecyclerView.Adapter<CategoryReportAdapter.ViewHolder> {
    private List<ReportFragment.CategoryReportItem> items;

    public CategoryReportAdapter(List<ReportFragment.CategoryReportItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportFragment.CategoryReportItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryName, tvAmount, tvCount, tvPercentage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCount = itemView.findViewById(R.id.tvCount);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
        }

        void bind(ReportFragment.CategoryReportItem item) {
            tvCategoryName.setText(item.getCategoryName());
            tvAmount.setText(formatAmount(item.getAmount()));
            tvCount.setText(itemView.getContext().getString(R.string.transaction_count_format, item.getCount()));
            tvPercentage.setText(String.format("%.1f%%", item.getPercentage()));
        }

        private String formatAmount(double amount) {
            return String.format(Locale.getDefault(), "%,.0f VND", amount);
        }
    }
}

