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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TransactionSectionAdapter - Adapter với section headers để nhóm transactions theo tháng/năm
 */
public class TransactionSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TRANSACTION = 1;
    
    private List<Object> items; // Chứa SectionHeader hoặc Transaction
    private OnTransactionClickListener listener;
    private OnTransactionEditListener editListener;
    private OnTransactionDeleteListener deleteListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
    private Map<String, String> categoryIdToNameMap;
    private FirebaseHelper firebaseHelper;
    private boolean showEditDeleteButtons = true;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }
    
    public interface OnTransactionEditListener {
        void onTransactionEdit(Transaction transaction);
    }
    
    public interface OnTransactionDeleteListener {
        void onTransactionDelete(Transaction transaction);
    }
    
    // Class để đại diện cho section header
    public static class SectionHeader {
        private String monthYear;
        
        public SectionHeader(String monthYear) {
            this.monthYear = monthYear;
        }
        
        public String getMonthYear() {
            return monthYear;
        }
    }

    public TransactionSectionAdapter(OnTransactionClickListener listener) {
        this.listener = listener;
        this.items = new ArrayList<>();
        this.firebaseHelper = new FirebaseHelper();
        this.categoryIdToNameMap = new HashMap<>();
        loadCategories();
    }
    
    public void setOnTransactionEditListener(OnTransactionEditListener listener) {
        this.editListener = listener;
    }
    
    public void setOnTransactionDeleteListener(OnTransactionDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    public void setShowEditDeleteButtons(boolean show) {
        this.showEditDeleteButtons = show;
        notifyDataSetChanged();
    }
    
    /**
     * Cập nhật danh sách transactions và tự động nhóm theo tháng/năm
     */
    public void setTransactions(List<Transaction> transactions) {
        items.clear();
        
        if (transactions == null || transactions.isEmpty()) {
            notifyDataSetChanged();
            return;
        }
        
        // Nhóm transactions theo tháng/năm
        Map<String, List<Transaction>> groupedTransactions = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        
        for (Transaction transaction : transactions) {
            calendar.setTime(transaction.getDate());
            String monthYear = monthYearFormat.format(transaction.getDate());
            
            if (!groupedTransactions.containsKey(monthYear)) {
                groupedTransactions.put(monthYear, new ArrayList<>());
            }
            groupedTransactions.get(monthYear).add(transaction);
        }
        
        // Sắp xếp các tháng theo thứ tự giảm dần (mới nhất trước)
        // Sử dụng Calendar để so sánh trực tiếp
        List<Map.Entry<String, List<Transaction>>> entries = new ArrayList<>(groupedTransactions.entrySet());
        entries.sort((entry1, entry2) -> {
            // Lấy transaction đầu tiên của mỗi tháng để so sánh
            List<Transaction> list1 = entry1.getValue();
            List<Transaction> list2 = entry2.getValue();
            if (list1.isEmpty() || list2.isEmpty()) {
                return 0;
            }
            Transaction t1 = list1.get(0);
            Transaction t2 = list2.get(0);
            return t2.getDate().compareTo(t1.getDate()); // Giảm dần (mới nhất trước)
        });
        
        // Thêm vào items: header rồi đến transactions của tháng đó
        for (Map.Entry<String, List<Transaction>> entry : entries) {
            String monthYear = entry.getKey();
            List<Transaction> monthTransactions = entry.getValue();
            
            items.add(new SectionHeader(monthYear));
            // Sắp xếp transactions trong tháng theo ngày giảm dần
            monthTransactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
            items.addAll(monthTransactions);
        }
        
        notifyDataSetChanged();
    }
    
    /**
     * Lọc transactions theo tháng/năm cụ thể
     */
    public void filterByMonthYear(int year, int month) {
        // Method này sẽ được gọi từ fragment với tháng/năm được chọn
        // Tạm thời để trống, sẽ được implement trong fragment
    }
    
    private void loadCategories() {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                updateCategoryMap(categories);
                notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                // Nếu không load được categories, vẫn hiển thị category như cũ
            }
        });
    }
    
    private void updateCategoryMap(List<Category> categories) {
        categoryIdToNameMap.clear();
        for (Category category : categories) {
            if (category.getId() != null && category.getName() != null) {
                categoryIdToNameMap.put(category.getId(), category.getName());
                categoryIdToNameMap.put(category.getName(), category.getName());
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof SectionHeader) {
            return TYPE_HEADER;
        } else {
            return TYPE_TRANSACTION;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            SectionHeader header = (SectionHeader) items.get(position);
            ((HeaderViewHolder) holder).bind(header);
        } else if (holder instanceof TransactionViewHolder) {
            Transaction transaction = (Transaction) items.get(position);
            ((TransactionViewHolder) holder).bind(transaction);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * ViewHolder cho section header
     */
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvSectionHeader;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSectionHeader = itemView.findViewById(R.id.tvSectionHeader);
        }

        void bind(SectionHeader header) {
            tvSectionHeader.setText(header.getMonthYear());
        }
    }

    /**
     * ViewHolder cho transaction item
     */
    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategory, tvNote, tvDate, tvAmount;
        private ImageButton btnEdit, btnDelete;

        TransactionViewHolder(@NonNull View itemView) {
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
                        Object item = items.get(position);
                        if (item instanceof Transaction) {
                            listener.onTransactionClick((Transaction) item);
                        }
                    }
                }
            });
            
            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && editListener != null) {
                    Object item = items.get(position);
                    if (item instanceof Transaction) {
                        editListener.onTransactionEdit((Transaction) item);
                    }
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && deleteListener != null) {
                    Object item = items.get(position);
                    if (item instanceof Transaction) {
                        deleteListener.onTransactionDelete((Transaction) item);
                    }
                }
            });
        }

        void bind(Transaction transaction) {
            String categoryDisplay = transaction.getCategory();
            
            if (categoryIdToNameMap.containsKey(categoryDisplay)) {
                categoryDisplay = categoryIdToNameMap.get(categoryDisplay);
            }
            
            tvCategory.setText(categoryDisplay != null ? categoryDisplay : itemView.getContext().getString(R.string.unknown));
            
            String note = transaction.getNote();
            if (note != null && !note.trim().isEmpty()) {
                tvNote.setText(note);
                tvNote.setVisibility(View.VISIBLE);
            } else {
                tvNote.setVisibility(View.GONE);
            }
            
            tvDate.setText(dateFormat.format(transaction.getDate()));
            
            if (transaction.isIncome()) {
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.income_color));
                tvAmount.setText("+" + formatAmount(transaction.getAmount()));
            } else {
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.expense_color));
                tvAmount.setText("-" + formatAmount(transaction.getAmount()));
            }
            
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

