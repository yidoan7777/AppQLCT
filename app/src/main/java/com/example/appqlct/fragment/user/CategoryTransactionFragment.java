package com.example.appqlct.fragment.user;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.appqlct.R;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.helper.NotificationHelper;
import com.example.appqlct.helper.SharedPreferencesHelper;
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
 * CategoryTransactionFragment - Hiển thị giao dịch của một danh mục cụ thể
 * Có thể lọc theo tháng/năm hoặc xem tất cả
 */
public class CategoryTransactionFragment extends Fragment {
    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";
    
    private TableLayout tableTransactions;
    private TextView tvEmpty;
    private TextView tvCategoryName;
    private TextView tvMonthYear;
    private ImageButton btnSelectMonthYear;
    private ImageButton btnViewAll; // Nút "Xem tất cả" để quay lại
    private List<Transaction> transactionList;
    private List<Transaction> allTransactions; // Lưu tất cả transactions để lọc
    private Map<String, String> categoryIdToNameMap; // Map category ID -> category name
    private FirebaseHelper firebaseHelper;
    private SharedPreferencesHelper prefsHelper;
    private Calendar selectedCalendar;
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private boolean isFilteringByMonth = false; // Flag để biết có đang lọc theo tháng không
    private String categoryId; // Category ID đang được xem
    private String categoryName; // Tên category đang được xem
    
    /**
     * Tạo instance mới của CategoryTransactionFragment
     * @param categoryId ID của category để xem
     * @param categoryName Tên của category để hiển thị
     * @return CategoryTransactionFragment instance
     */
    public static CategoryTransactionFragment newInstance(String categoryId, String categoryName) {
        CategoryTransactionFragment fragment = new CategoryTransactionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_ID, categoryId);
        args.putString(ARG_CATEGORY_NAME, categoryName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_transaction, container, false);

        // Đọc category ID và name từ Bundle
        if (getArguments() != null) {
            categoryId = getArguments().getString(ARG_CATEGORY_ID);
            categoryName = getArguments().getString(ARG_CATEGORY_NAME);
        }

        initViews(view);
        initHelpers();
        loadCategories();
        loadTransactions();

        return view;
    }

    private void initViews(View view) {
        tableTransactions = view.findViewById(R.id.tableTransactions);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvCategoryName = view.findViewById(R.id.tvCategoryName);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        btnSelectMonthYear = view.findViewById(R.id.btnSelectMonthYear);
        btnViewAll = view.findViewById(R.id.btnViewAll);
        
        selectedCalendar = Calendar.getInstance();
        
        // Hiển thị tên category
        if (categoryName != null) {
            tvCategoryName.setText(categoryName);
        }
        
        // Thiết lập nút chọn tháng/năm
        // Nếu đang lọc theo tháng, nhấn vào sẽ reset về "Tất cả"
        // Nếu chưa lọc theo tháng, nhấn vào sẽ mở DatePicker
        btnSelectMonthYear.setOnClickListener(v -> {
            if (isFilteringByMonth) {
                // Reset về "Tất cả"
                isFilteringByMonth = false;
                applyFilter();
            } else {
                // Mở DatePicker
                showMonthYearPicker();
            }
        });
        
        // Thiết lập nút "Xem tất cả" để quay lại màn hình chính
        btnViewAll.setOnClickListener(v -> {
            // Quay lại fragment trước đó
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        prefsHelper = new SharedPreferencesHelper(requireContext());
        transactionList = new ArrayList<>();
        allTransactions = new ArrayList<>();
        categoryIdToNameMap = new HashMap<>();
    }

    /**
     * Load danh sách categories từ Firestore để map ID -> Name
     */
    private void loadCategories() {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                if (!isAdded() || getContext() == null) return;
                
                categoryIdToNameMap.clear();
                
                for (Category category : categories) {
                    if (category != null && category.getId() != null && category.getName() != null) {
                        categoryIdToNameMap.put(category.getId(), category.getName());
                        categoryIdToNameMap.put(category.getName(), category.getName());
                    }
                }
            }

            @Override
            public void onError(String error) {
                // Xử lý lỗi - không cần hiển thị Toast
            }
        });
    }

    /**
     * Load danh sách transactions từ Firestore
     */
    private void loadTransactions() {
        String userId = prefsHelper.getUserId();
        
        firebaseHelper.getUserTransactions(userId, new FirebaseHelper.OnTransactionsLoadedListener() {
            @Override
            public void onTransactionsLoaded(List<Transaction> transactions) {
                if (!isAdded() || getContext() == null) return;
                
                // Lưu tất cả transactions
                allTransactions.clear();
                allTransactions.addAll(transactions);
                
                // Áp dụng filter
                applyFilter();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                String userId = prefsHelper.getUserId();
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.error_loading_transactions, error));
            }
        });
    }
    
    /**
     * Áp dụng filter theo tháng/năm (luôn lọc theo category)
     */
    private void applyFilter() {
        transactionList.clear();
        
        // Bước 1: Lọc theo category (luôn luôn)
        List<Transaction> filteredByCategory = new ArrayList<>();
        String categoryNameToMatch = categoryIdToNameMap.get(categoryId);
        
        for (Transaction transaction : allTransactions) {
            if (transaction == null) continue;
            
            String transactionCategory = transaction.getCategory();
            
            // So sánh với category ID hoặc category name
            boolean matches = false;
            if (categoryId != null && categoryId.equals(transactionCategory)) {
                matches = true;
            } else if (categoryNameToMatch != null) {
                // So sánh với category name
                String transactionCategoryName = categoryIdToNameMap.get(transactionCategory);
                if (categoryNameToMatch.equals(transactionCategoryName) || 
                    categoryNameToMatch.equals(transactionCategory)) {
                    matches = true;
                }
            }
            
            // Chỉ lấy expense transactions
            if (matches && !transaction.isIncome()) {
                filteredByCategory.add(transaction);
            }
        }
        
        // Bước 2: Lọc theo tháng/năm nếu có
        if (isFilteringByMonth && selectedCalendar != null) {
            Calendar filterCalendar = (Calendar) selectedCalendar.clone();
            
            // Normalize về đầu tháng
            filterCalendar.set(Calendar.DAY_OF_MONTH, 1);
            filterCalendar.set(Calendar.HOUR_OF_DAY, 0);
            filterCalendar.set(Calendar.MINUTE, 0);
            filterCalendar.set(Calendar.SECOND, 0);
            filterCalendar.set(Calendar.MILLISECOND, 0);
            
            int filterYear = filterCalendar.get(Calendar.YEAR);
            int filterMonth = filterCalendar.get(Calendar.MONTH);
            
            for (Transaction transaction : filteredByCategory) {
                if (transaction.getDate() == null) continue;
                
                Calendar transactionCalendar = Calendar.getInstance();
                transactionCalendar.setTime(transaction.getDate());
                
                int transactionYear = transactionCalendar.get(Calendar.YEAR);
                int transactionMonth = transactionCalendar.get(Calendar.MONTH);
                
                // Chỉ thêm transaction nếu cùng năm và cùng tháng
                if (transactionYear == filterYear && transactionMonth == filterMonth) {
                    transactionList.add(transaction);
                }
            }
        } else {
            transactionList.addAll(filteredByCategory);
        }
        
        // Sắp xếp theo ngày giảm dần (mới nhất trước)
        transactionList.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
        
        // Cập nhật UI
        if (isFilteringByMonth && selectedCalendar != null) {
            String formattedText = monthYearFormat.format(selectedCalendar.getTime());
            tvMonthYear.setText(formattedText);
            // Đổi icon thành nút reset khi đang lọc theo tháng
            btnSelectMonthYear.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            tvMonthYear.setText(getString(R.string.all));
            // Đổi icon thành calendar khi không lọc theo tháng
            btnSelectMonthYear.setImageResource(android.R.drawable.ic_menu_my_calendar);
        }
        
        // Hiển thị bảng
        displayTransactions();
        
        // Hiển thị/ẩn empty state
        if (transactionList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tableTransactions.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            tableTransactions.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hiển thị transactions trong bảng
     */
    private void displayTransactions() {
        // Xóa tất cả các row cũ (trừ header row đầu tiên)
        int childCount = tableTransactions.getChildCount();
        if (childCount > 1) {
            tableTransactions.removeViews(1, childCount - 1);
        }
        
        // Thêm các row mới
        int index = 1;
        for (Transaction transaction : transactionList) {
            TableRow row = (TableRow) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_transaction_table_row, tableTransactions, false);
            
            TextView tvName = row.findViewById(R.id.tvName);
            TextView tvAmount = row.findViewById(R.id.tvAmount);
            TextView tvDate = row.findViewById(R.id.tvDate);
            TextView tvCategory = row.findViewById(R.id.tvCategory);
            
            // Tên chi tiêu (sử dụng note nếu có, nếu không thì dùng số thứ tự)
            String note = transaction.getNote();
            if (note != null && !note.trim().isEmpty()) {
                tvName.setText(note);
            } else {
                tvName.setText(String.valueOf(index));
            }
            index++;
            
            // Số tiền (màu đỏ cho chi tiêu)
            tvAmount.setTextColor(requireContext().getColor(R.color.expense_color));
            tvAmount.setText(formatAmount(transaction.getAmount()));
            
            // Ngày chi tiêu
            tvDate.setText(dateFormat.format(transaction.getDate()));
            
            // Loại chi tiêu (category name)
            String categoryDisplay = transaction.getCategory();
            if (categoryIdToNameMap.containsKey(categoryDisplay)) {
                categoryDisplay = categoryIdToNameMap.get(categoryDisplay);
            }
            tvCategory.setText(categoryDisplay != null ? categoryDisplay : getString(R.string.unknown));
            
            // Thêm click listener để có thể sửa/xóa
            row.setOnClickListener(v -> {
                showTransactionOptionsDialog(transaction);
            });
            
            tableTransactions.addView(row);
        }
    }

    /**
     * Format số tiền
     */
    private String formatAmount(double amount) {
        return String.format(Locale.getDefault(), "%,.0f", amount);
    }

    /**
     * Hiển thị dialog với các tùy chọn cho transaction (Sửa/Xóa)
     */
    private void showTransactionOptionsDialog(Transaction transaction) {
        String[] options = {getString(R.string.edit), getString(R.string.delete)};
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Edit
                        openEditTransactionDialog(transaction);
                    } else if (which == 1) {
                        // Delete
                        showDeleteConfirmationDialog(transaction);
                    }
                })
                .show();
    }
    
    /**
     * Hiển thị DatePicker để chọn tháng/năm
     */
    private void showMonthYearPicker() {
        int year = selectedCalendar.get(Calendar.YEAR);
        int month = selectedCalendar.get(Calendar.MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, selectedYear, selectedMonth, dayOfMonth) -> {
                // Normalize selectedCalendar về đầu tháng được chọn
                selectedCalendar.clear();
                selectedCalendar.set(Calendar.YEAR, selectedYear);
                selectedCalendar.set(Calendar.MONTH, selectedMonth);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, 1);
                selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                selectedCalendar.set(Calendar.MINUTE, 0);
                selectedCalendar.set(Calendar.SECOND, 0);
                selectedCalendar.set(Calendar.MILLISECOND, 0);
                
                isFilteringByMonth = true;
                applyFilter();
            },
            year,
            month,
            1 // Ngày mặc định là 1
        );
        
        datePickerDialog.show();
    }
    
    /**
     * Mở dialog để sửa giao dịch
     */
    private void openEditTransactionDialog(Transaction transaction) {
        AddTransactionFragment dialog = AddTransactionFragment.newInstance(transaction);
        dialog.setOnTransactionAddedListener(new AddTransactionFragment.OnTransactionAddedListener() {
            @Override
            public void onTransactionAdded() {
                // Fallback: reload từ Firestore nếu không có transaction object
                loadTransactions();
            }
            
            @Override
            public void onTransactionAdded(Transaction updatedTransaction) {
                // Đảm bảo refresh sau khi dialog dismiss hoàn toàn
                refreshAfterDialogDismiss(() -> {
                    if (isAdded() && isResumed() && getView() != null) {
                        updateTransactionLocally(updatedTransaction);
                    }
                });
            }
        });
        dialog.show(getParentFragmentManager(), "EditTransactionDialog");
    }
    
    /**
     * Helper method để refresh sau khi dialog dismiss
     * Sử dụng retry logic để đảm bảo fragment đã resume
     */
    private void refreshAfterDialogDismiss(Runnable refreshAction) {
        if (refreshAction == null) return;
        
        // Thử refresh ngay lập tức nếu fragment đã sẵn sàng
        if (isAdded() && isResumed() && getView() != null) {
            View view = getView();
            if (view != null) {
                view.post(() -> {
                    if (isAdded() && isResumed() && getView() != null) {
                        refreshAction.run();
                        return;
                    }
                });
            }
        }
        
        // Nếu chưa sẵn sàng, retry sau một khoảng thời gian
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && isResumed() && getView() != null) {
                View view = getView();
                if (view != null) {
                    view.post(refreshAction);
                } else {
                    refreshAction.run();
                }
            } else {
                // Retry lần cuối sau 300ms nữa
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded() && getView() != null) {
                        refreshAction.run();
                    }
                }, 300);
            }
        }, 100);
    }
    
    /**
     * Thêm transaction vào local list và refresh UI ngay lập tức
     */
    private void addTransactionLocally(Transaction transaction) {
        if (transaction == null || !isAdded() || getView() == null) return;
        
        // Thêm vào allTransactions
        allTransactions.add(transaction);
        
        // Áp dụng filter để cập nhật transactionList
        applyFilter();
        
        // Refresh UI
        displayTransactions();
    }
    
    /**
     * Cập nhật transaction trong local list và refresh UI ngay lập tức
     */
    private void updateTransactionLocally(Transaction transaction) {
        if (transaction == null || transaction.getId() == null || !isAdded() || getView() == null) return;
        
        // Tìm và cập nhật transaction trong allTransactions
        for (int i = 0; i < allTransactions.size(); i++) {
            Transaction t = allTransactions.get(i);
            if (t != null && transaction.getId().equals(t.getId())) {
                allTransactions.set(i, transaction);
                break;
            }
        }
        
        // Áp dụng filter để cập nhật transactionList
        applyFilter();
        
        // Refresh UI
        displayTransactions();
    }
    
    /**
     * Hiển thị dialog xác nhận xóa giao dịch
     */
    private void showDeleteConfirmationDialog(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm))
                .setMessage(getString(R.string.delete_transaction_confirm))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    deleteTransaction(transaction);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
    
    /**
     * Xóa giao dịch
     */
    private void deleteTransaction(Transaction transaction) {
        if (transaction.getId() == null) {
            String userId = prefsHelper.getUserId();
            NotificationHelper.addErrorNotification(requireContext(), userId, 
                    getString(R.string.cannot_delete_recurring_expense));
            return;
        }
        
        firebaseHelper.deleteTransaction(transaction.getId(), task -> {
            if (!isAdded() || getContext() == null) return;
            String userId = prefsHelper.getUserId();
            if (task.isSuccessful()) {
                NotificationHelper.addSuccessNotification(getContext(), userId, 
                        getString(R.string.delete_transaction_success));
                loadTransactions();
            } else {
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.delete_transaction_failed, getString(R.string.unknown)));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload categories và transactions khi quay lại fragment
        if (firebaseHelper != null) {
            loadCategories();
            loadTransactions();
        }
    }
}

