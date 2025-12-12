package com.example.appqlct.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.model.Category;
import com.example.appqlct.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TransactionAdapter - Adapter cho RecyclerView hiển thị danh sách transactions
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<Transaction> transactions;
    private OnTransactionClickListener listener;
    private OnTransactionEditListener editListener;
    private OnTransactionDeleteListener deleteListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private Map<String, String> categoryIdToNameMap; // Map category ID -> category name
    private FirebaseHelper firebaseHelper;
    private boolean showEditDeleteButtons = true; // Mặc định hiển thị nút Edit/Delete

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }
    
    public interface OnTransactionEditListener {
        void onTransactionEdit(Transaction transaction);
    }
    
    public interface OnTransactionDeleteListener {
        void onTransactionDelete(Transaction transaction);
    }
    
    public void setOnTransactionEditListener(OnTransactionEditListener listener) {
        this.editListener = listener;
    }
    
    public void setOnTransactionDeleteListener(OnTransactionDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    /**
     * Thiết lập có hiển thị nút Edit/Delete hay không
     * @param show true để hiển thị, false để ẩn
     */
    public void setShowEditDeleteButtons(boolean show) {
        this.showEditDeleteButtons = show;
        notifyDataSetChanged();
    }

    public TransactionAdapter(List<Transaction> transactions, OnTransactionClickListener listener) {
        this.transactions = transactions;
        this.listener = listener;
        this.firebaseHelper = new FirebaseHelper();
        this.categoryIdToNameMap = new HashMap<>();
        loadCategories();
    }
    
    /**
     * Load tất cả categories để map ID với tên (cho các transaction cũ)
     */
    private void loadCategories() {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                updateCategoryMap(categories);
                // Notify adapter để reload với category names đã được map
                notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                // Nếu không load được categories, vẫn hiển thị category như cũ
            }
        });
    }
    
    /**
     * Public method để reload categories từ bên ngoài
     */
    public void reloadCategories() {
        loadCategories();
    }
    
    /**
     * Cập nhật category map với danh sách categories mới
     */
    private void updateCategoryMap(List<Category> categories) {
        categoryIdToNameMap.clear();
        for (Category category : categories) {
            if (category.getId() != null && category.getName() != null) {
                // Map ID -> Name
                categoryIdToNameMap.put(category.getId(), category.getName());
                // Map Name -> Name (để normalize, tránh trường hợp có cả ID và Name)
                categoryIdToNameMap.put(category.getName(), category.getName());
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategory, tvNote, tvDate, tvAmount;
        private ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onTransactionClick(transactions.get(position));
                    }
                }
            });
            
            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && editListener != null) {
                    editListener.onTransactionEdit(transactions.get(position));
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && deleteListener != null) {
                    deleteListener.onTransactionDelete(transactions.get(position));
                }
            });
        }

        void bind(Transaction transaction) {
            // Hiển thị thông tin transaction
            String categoryDisplay = transaction.getCategory();
            
            // Nếu category là ID (transaction cũ), tìm tên từ map
            // Nếu không tìm thấy trong map, có thể là tên category (transaction mới) hoặc ID chưa được load
            if (categoryIdToNameMap.containsKey(categoryDisplay)) {
                categoryDisplay = categoryIdToNameMap.get(categoryDisplay);
            }
            
            tvCategory.setText(categoryDisplay != null ? categoryDisplay : itemView.getContext().getString(R.string.unknown));
            
            // Hiển thị ghi chú nếu có
            String note = transaction.getNote();
            if (note != null && !note.trim().isEmpty()) {
                tvNote.setText(note);
                tvNote.setVisibility(View.VISIBLE);
            } else {
                tvNote.setVisibility(View.GONE);
            }
            
            tvDate.setText(dateFormat.format(transaction.getDate()));
            
            // Hiển thị số tiền với màu phù hợp
            if (transaction.isIncome()) {
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.income_color));
                tvAmount.setText("+" + formatAmount(transaction.getAmount()));
            } else {
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.expense_color));
                tvAmount.setText("-" + formatAmount(transaction.getAmount()));
            }
            
            // Ẩn/hiện nút Edit và Delete dựa trên flag
            if (showEditDeleteButtons) {
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
            } else {
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            }
        }

        private String formatAmount(double amount) {
            return String.format(Locale.getDefault(), "%,.0f VND", amount);
        }
    }
}

