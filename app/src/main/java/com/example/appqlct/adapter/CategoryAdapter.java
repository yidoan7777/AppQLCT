package com.example.appqlct.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.model.Category;

import java.util.List;

/**
 * CategoryAdapter - Adapter cho RecyclerView hiển thị danh sách categories (Admin)
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<Category> categories;
    private OnCategoryClickListener clickListener;
    private OnCategoryDeleteListener deleteListener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public interface OnCategoryDeleteListener {
        void onCategoryDelete(Category category);
    }

    public CategoryAdapter(List<Category> categories, 
                          OnCategoryClickListener clickListener,
                          OnCategoryDeleteListener deleteListener) {
        this.categories = categories;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategoryName;
        private ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onCategoryClick(categories.get(getAdapterPosition()));
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onCategoryDelete(categories.get(getAdapterPosition()));
                }
            });
        }

        void bind(Category category) {
            tvCategoryName.setText(category.getName());
        }
    }
}

