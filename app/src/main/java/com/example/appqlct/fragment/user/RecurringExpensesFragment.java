package com.example.appqlct.fragment.user;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appqlct.R;
import com.example.appqlct.fragment.user.AddTransactionFragment;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.helper.NotificationHelper;
import com.example.appqlct.helper.SharedPreferencesHelper;
import com.example.appqlct.model.Category;
import com.example.appqlct.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecurringExpensesFragment - Hiển thị danh sách chi tiêu định kỳ
 */
public class RecurringExpensesFragment extends Fragment {
    private TableLayout tableRecurringExpenses;
    private TextView tvEmpty;
    private List<Transaction> recurringExpensesList;
    private List<Category> allCategories;
    private Map<String, String> categoryIdToNameMap;
    private FirebaseHelper firebaseHelper;
    private SharedPreferencesHelper prefsHelper;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recurring_expenses, container, false);

        initViews(view);
        initHelpers();
        loadCategories();
        loadRecurringExpenses();

        return view;
    }

    private void initViews(View view) {
        tableRecurringExpenses = view.findViewById(R.id.tableRecurringExpenses);
        tvEmpty = view.findViewById(R.id.tvEmpty);
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        prefsHelper = new SharedPreferencesHelper(requireContext());
        recurringExpensesList = new ArrayList<>();
        allCategories = new ArrayList<>();
        categoryIdToNameMap = new HashMap<>();
    }

    /**
     * Load danh sách categories từ Firestore
     */
    private void loadCategories() {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                if (!isAdded() || getContext() == null) return;
                
                allCategories.clear();
                categoryIdToNameMap.clear();
                
                // Chỉ lấy expense categories
                for (Category category : categories) {
                    if (category != null && "expense".equals(category.getType())) {
                        allCategories.add(category);
                        if (category.getId() != null && category.getName() != null) {
                            categoryIdToNameMap.put(category.getId(), category.getName());
                            categoryIdToNameMap.put(category.getName(), category.getName());
                        }
                    }
                }
                
                // Reload danh sách chi tiêu định kỳ sau khi load xong categories
                loadRecurringExpenses();
            }

            @Override
            public void onError(String error) {
                // Xử lý lỗi - không cần hiển thị Toast
            }
        });
    }

    /**
     * Load danh sách chi tiêu định kỳ từ Firestore
     */
    private void loadRecurringExpenses() {
        String userId = prefsHelper.getUserId();
        
        if (userId == null || userId.isEmpty()) {
            if (isAdded() && getContext() != null) {
                NotificationHelper.addInfoNotification(getContext(), "", 
                        "Vui lòng đăng nhập lại");
            }
            return;
        }
        
        firebaseHelper.getUserRecurringTransactions(userId, new FirebaseHelper.OnTransactionsLoadedListener() {
            @Override
            public void onTransactionsLoaded(List<Transaction> transactions) {
                if (!isAdded() || getContext() == null) return;
                
                recurringExpensesList.clear();
                recurringExpensesList.addAll(transactions);
                
                // Sắp xếp theo ngày giảm dần (mới nhất trước)
                recurringExpensesList.sort((t1, t2) -> {
                    if (t1.getDate() == null && t2.getDate() == null) return 0;
                    if (t1.getDate() == null) return 1;
                    if (t2.getDate() == null) return -1;
                    return t2.getDate().compareTo(t1.getDate());
                });
                
                // Hiển thị danh sách
                displayRecurringExpenses();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Log chi tiết lỗi để debug
                android.util.Log.e("RecurringExpenses", "Error loading recurring expenses: " + error);
                String userId = prefsHelper.getUserId();
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.error_loading_recurring_expenses, error != null ? error : getString(R.string.unknown)));
            }
        });
    }

    /**
     * Hiển thị chi tiêu định kỳ trong bảng
     */
    private void displayRecurringExpenses() {
        // Xóa tất cả các row cũ (trừ header row đầu tiên)
        int childCount = tableRecurringExpenses.getChildCount();
        if (childCount > 1) {
            tableRecurringExpenses.removeViews(1, childCount - 1);
        }
        
        // Thêm các row mới
        int index = 1;
        for (Transaction transaction : recurringExpensesList) {
            TableRow row = (TableRow) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_transaction_table_row, tableRecurringExpenses, false);
            
            TextView tvName = row.findViewById(R.id.tvName);
            TextView tvAmount = row.findViewById(R.id.tvAmount);
            TextView tvDate = row.findViewById(R.id.tvDate);
            TextView tvCategory = row.findViewById(R.id.tvCategory);
            
            // Tên chi tiêu (sử dụng note nếu có, nếu không thì dùng số thứ tự)
            String note = transaction.getNote();
            if (note != null && !note.trim().isEmpty()) {
                tvName.setText(note);
            } else {
                // Nếu không có note, hiển thị số thứ tự
                tvName.setText(String.valueOf(index));
            }
            index++;
            
            // Số tiền (màu đỏ cho chi tiêu)
            tvAmount.setTextColor(requireContext().getColor(R.color.expense_color));
            tvAmount.setText(formatAmount(transaction.getAmount()));
            
            // Ngày chi tiêu
            if (transaction.getDate() != null) {
                tvDate.setText(dateFormat.format(transaction.getDate()));
            } else {
                tvDate.setText("N/A");
            }
            
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
            
            tableRecurringExpenses.addView(row);
        }
        
        // Hiển thị/ẩn empty state
        if (recurringExpensesList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tableRecurringExpenses.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            tableRecurringExpenses.setVisibility(View.VISIBLE);
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
     * Mở dialog để sửa giao dịch
     */
    private void openEditTransactionDialog(Transaction transaction) {
        AddTransactionFragment dialog = AddTransactionFragment.newInstance(transaction);
        dialog.setOnTransactionAddedListener(new AddTransactionFragment.OnTransactionAddedListener() {
            @Override
            public void onTransactionAdded() {
                // Fallback: reload từ Firestore nếu không có transaction object
                loadRecurringExpenses();
            }
            
            @Override
            public void onTransactionAdded(Transaction updatedTransaction) {
                // Đảm bảo refresh sau khi dialog dismiss hoàn toàn
                refreshAfterDialogDismiss(() -> {
                    if (isAdded() && isResumed() && getView() != null) {
                        updateRecurringTransactionLocally(updatedTransaction);
                    }
                });
            }
        });
        dialog.show(getParentFragmentManager(), "EditRecurringTransactionDialog");
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
     * Thêm recurring transaction vào local list và refresh UI ngay lập tức
     */
    private void addRecurringTransactionLocally(Transaction transaction) {
        if (transaction == null || !transaction.isRecurring() || !isAdded() || getView() == null) return;
        
        // Thêm vào danh sách
        recurringExpensesList.add(transaction);
        
        // Sắp xếp lại theo ngày giảm dần
        recurringExpensesList.sort((t1, t2) -> {
            if (t1.getDate() == null && t2.getDate() == null) return 0;
            if (t1.getDate() == null) return 1;
            if (t2.getDate() == null) return -1;
            return t2.getDate().compareTo(t1.getDate());
        });
        
        // Refresh UI (displayRecurringExpenses đã xử lý empty state)
        displayRecurringExpenses();
    }
    
    /**
     * Cập nhật recurring transaction trong local list và refresh UI ngay lập tức
     */
    private void updateRecurringTransactionLocally(Transaction transaction) {
        if (transaction == null || transaction.getId() == null || !isAdded() || getView() == null) return;
        
        // Nếu không còn là recurring, xóa khỏi list
        if (!transaction.isRecurring()) {
            removeRecurringTransactionLocally(transaction.getId());
            return;
        }
        
        // Tìm và cập nhật transaction trong list
        for (int i = 0; i < recurringExpensesList.size(); i++) {
            Transaction t = recurringExpensesList.get(i);
            if (t != null && transaction.getId().equals(t.getId())) {
                recurringExpensesList.set(i, transaction);
                break;
            }
        }
        
        // Sắp xếp lại theo ngày giảm dần
        recurringExpensesList.sort((t1, t2) -> {
            if (t1.getDate() == null && t2.getDate() == null) return 0;
            if (t1.getDate() == null) return 1;
            if (t2.getDate() == null) return -1;
            return t2.getDate().compareTo(t1.getDate());
        });
        
        // Refresh UI (displayRecurringExpenses đã xử lý empty state)
        displayRecurringExpenses();
    }
    
    /**
     * Xóa recurring transaction khỏi local list
     */
    private void removeRecurringTransactionLocally(String transactionId) {
        if (transactionId == null || !isAdded() || getView() == null) return;
        
        recurringExpensesList.removeIf(t -> t != null && transactionId.equals(t.getId()));
        
        // Refresh UI (displayRecurringExpenses đã xử lý empty state)
        displayRecurringExpenses();
    }
    
    /**
     * Hiển thị dialog xác nhận xóa giao dịch
     */
    private void showDeleteConfirmationDialog(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm))
                .setMessage(getString(R.string.delete_recurring_expense_confirm))
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
        
        // Nếu là giao dịch định kỳ, xóa cả các giao dịch tự động đã tạo
        if (transaction.isRecurring()) {
            // Xóa các giao dịch tự động trước
            firebaseHelper.deleteTransactionsFromRecurring(transaction.getId(), deleteGeneratedTask -> {
                if (!isAdded() || getContext() == null) return;
                
                // Sau đó xóa giao dịch định kỳ gốc
                firebaseHelper.deleteTransaction(transaction.getId(), task -> {
                    if (!isAdded() || getContext() == null) return;
                    String userId = prefsHelper.getUserId();
                    if (task.isSuccessful()) {
                        NotificationHelper.addSuccessNotification(getContext(), userId, 
                                getString(R.string.delete_recurring_expense_success));
                        loadRecurringExpenses();
                    } else {
                        NotificationHelper.addErrorNotification(getContext(), userId, 
                                getString(R.string.delete_recurring_expense_failed));
                    }
                });
            });
        } else {
            // Nếu không phải định kỳ, chỉ xóa giao dịch đó
            firebaseHelper.deleteTransaction(transaction.getId(), task -> {
                if (!isAdded() || getContext() == null) return;
                String userId = prefsHelper.getUserId();
                if (task.isSuccessful()) {
                    NotificationHelper.addSuccessNotification(getContext(), userId, 
                            getString(R.string.delete_recurring_expense_success));
                    loadRecurringExpenses();
                } else {
                    NotificationHelper.addErrorNotification(getContext(), userId, 
                            getString(R.string.delete_recurring_expense_failed));
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload categories và chi tiêu định kỳ khi quay lại fragment
        if (firebaseHelper != null) {
            loadCategories();
            loadRecurringExpenses();
        }
    }
}

