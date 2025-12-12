package com.example.appqlct.fragment.user;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
 * TransactionListFragment - Hiển thị danh sách giao dịch dạng bảng
 * Có thể lọc theo tháng/năm và danh mục
 */
public class TransactionListFragment extends Fragment {
    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";
    
    private TableLayout tableTransactions;
    private TextView tvEmpty;
    private TextView tvMonthYear;
    private ImageButton btnSelectMonthYear;
    private ImageButton btnResetFilter;
    private ImageButton btnViewAll; // Nút "Xem tất cả" khi vào từ danh mục cụ thể
    private Spinner spinnerCategory;
    private View rowCategoryFilter; // hàng chứa spinner lọc danh mục
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddTransaction;
    private List<Transaction> transactionList;
    private List<Transaction> allTransactions; // Lưu tất cả transactions để lọc
    private List<Category> allCategories; // Danh sách tất cả categories
    private Map<String, String> categoryIdToNameMap; // Map category ID -> category name
    private FirebaseHelper firebaseHelper;
    private SharedPreferencesHelper prefsHelper;
    private Calendar selectedCalendar;
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private boolean isFilteringByMonth = false; // Flag để biết có đang lọc theo tháng không
    private String selectedCategoryId = null; // Category ID đang được chọn để lọc (null = tất cả)
    private String initialCategoryName = null; // Tên category ban đầu (nếu có)
    
    /**
     * Tạo instance mới của TransactionListFragment
     * @param categoryId ID của category để filter (null nếu không filter)
     * @param categoryName Tên của category để hiển thị (null nếu không filter)
     * @return TransactionListFragment instance
     */
    public static TransactionListFragment newInstance(String categoryId, String categoryName) {
        TransactionListFragment fragment = new TransactionListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_ID, categoryId);
        args.putString(ARG_CATEGORY_NAME, categoryName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_table, container, false);

        // Đọc category ID và name từ Bundle (nếu có)
        if (getArguments() != null) {
            selectedCategoryId = getArguments().getString(ARG_CATEGORY_ID);
            initialCategoryName = getArguments().getString(ARG_CATEGORY_NAME);
        }

        initViews(view);
        initHelpers();
        setupCategorySpinner();
        loadCategories();
        loadTransactions();

        return view;
    }

    private void initViews(View view) {
        tableTransactions = view.findViewById(R.id.tableTransactions);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        btnSelectMonthYear = view.findViewById(R.id.btnSelectMonthYear);
        btnResetFilter = view.findViewById(R.id.btnResetFilter);
        btnViewAll = view.findViewById(R.id.btnViewAll);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        rowCategoryFilter = view.findViewById(R.id.rowCategoryFilter);
        fabAddTransaction = view.findViewById(R.id.fabAddTransaction);
        
        selectedCalendar = Calendar.getInstance();
        
        // Ẩn hàng lọc danh mục nếu mở từ một danh mục cụ thể
        if (selectedCategoryId != null && rowCategoryFilter != null) {
            rowCategoryFilter.setVisibility(View.GONE);
        }
        
        // Ẩn nút reset và FAB, hiển thị nút "Xem tất cả" nếu mở từ một danh mục cụ thể
        if (selectedCategoryId != null) {
            if (btnResetFilter != null) {
                btnResetFilter.setVisibility(View.GONE);
            }
            if (fabAddTransaction != null) {
                fabAddTransaction.setVisibility(View.GONE);
            }
            if (btnViewAll != null) {
                btnViewAll.setVisibility(View.VISIBLE);
            }
        } else {
            if (btnViewAll != null) {
                btnViewAll.setVisibility(View.GONE);
            }
        }

        // Nút chọn tháng/năm
        btnSelectMonthYear.setOnClickListener(v -> {
            showMonthYearPicker();
        });
        
        // Nút reset về "Tất cả giao dịch" - chỉ hiển thị khi không vào từ danh mục cụ thể
        btnResetFilter.setOnClickListener(v -> {
            isFilteringByMonth = false;
            selectedCategoryId = null;
            initialCategoryName = null; // Reset category name
            spinnerCategory.setSelection(0); // Reset về "Tất cả"
            applyFilter();
        });
        
        // Nút "Xem tất cả" - hiển thị khi vào từ danh mục cụ thể
        btnViewAll.setOnClickListener(v -> {
            // Reset về xem tất cả giao dịch
            selectedCategoryId = null;
            initialCategoryName = null;
            isFilteringByMonth = false;
            
            // Hiển thị lại spinner và FAB
            if (rowCategoryFilter != null) {
                rowCategoryFilter.setVisibility(View.VISIBLE);
            }
            if (fabAddTransaction != null) {
                fabAddTransaction.setVisibility(View.VISIBLE);
            }
            if (btnViewAll != null) {
                btnViewAll.setVisibility(View.GONE);
            }
            
            // Reset spinner về "Tất cả"
            spinnerCategory.setSelection(0);
            
            // Áp dụng filter
            applyFilter();
        });
        
        // Click vào text để chọn tháng/năm
        tvMonthYear.setOnClickListener(v -> {
            if (isFilteringByMonth) {
                showMonthYearPicker();
            }
        });

        // Floating Action Button để thêm giao dịch mới
        fabAddTransaction.setOnClickListener(v -> {
            // Mở dialog/fragment để thêm giao dịch
            openAddTransactionDialog();
        });
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        prefsHelper = new SharedPreferencesHelper(requireContext());
        transactionList = new ArrayList<>();
        allTransactions = new ArrayList<>();
        allCategories = new ArrayList<>();
        categoryIdToNameMap = new HashMap<>();
    }

    /**
     * Setup spinner để lọc theo danh mục
     */
    private void setupCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add(getString(R.string.all)); // Item đầu tiên là "Tất cả"
        
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryNames
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        
        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // "Tất cả" được chọn
                    selectedCategoryId = null;
                } else {
                    // Lấy category ID từ position (position - 1 vì có "Tất cả" ở đầu)
                    Category selectedCategory = allCategories.get(position - 1);
                    selectedCategoryId = selectedCategory.getId();
                }
                applyFilter();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
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
                
                // Cập nhật spinner
                updateCategorySpinner();
            }

            @Override
            public void onError(String error) {
                // Xử lý lỗi - không cần hiển thị Toast
            }
        });
    }

    /**
     * Cập nhật spinner với danh sách categories mới
     */
    private void updateCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add(getString(R.string.all));
        
        for (Category category : allCategories) {
            if (category != null && category.getName() != null) {
                categoryNames.add(category.getName());
            }
        }
        
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryNames
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        
        // Nếu có category ID từ Bundle, tự động chọn category trong spinner
        if (selectedCategoryId != null) {
            // Tìm category name từ selectedCategoryId
            String categoryName = categoryIdToNameMap.get(selectedCategoryId);
            if (categoryName != null) {
                int position = categoryNames.indexOf(categoryName);
                if (position > 0) { // position > 0 vì "Tất cả" ở vị trí 0
                    spinnerCategory.setSelection(position);
                }
            }
        }
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
                
                // Áp dụng filter nếu có
                applyFilter();
            }

            @Override
            public void onError(String error) {
                // Xử lý lỗi - không cần hiển thị Toast
                if (!isAdded() || getContext() == null) return;
            }
        });
    }
    
    /**
     * Áp dụng filter theo tháng/năm và danh mục
     */
    private void applyFilter() {
        transactionList.clear();
        
        // Bước 1: Lọc theo tháng/năm nếu có
        List<Transaction> filteredByMonth = new ArrayList<>();
        if (isFilteringByMonth && selectedCalendar != null) {
            // Tạo filterCalendar từ selectedCalendar (đã được normalize)
            Calendar filterCalendar = (Calendar) selectedCalendar.clone();
            
            // Đảm bảo filterCalendar được normalize về đầu tháng
            filterCalendar.set(Calendar.DAY_OF_MONTH, 1);
            filterCalendar.set(Calendar.HOUR_OF_DAY, 0);
            filterCalendar.set(Calendar.MINUTE, 0);
            filterCalendar.set(Calendar.SECOND, 0);
            filterCalendar.set(Calendar.MILLISECOND, 0);
            
            int filterYear = filterCalendar.get(Calendar.YEAR);
            int filterMonth = filterCalendar.get(Calendar.MONTH);
            
            // Debug log
            android.util.Log.d("TransactionList", "Filtering by: " + (filterMonth + 1) + "/" + filterYear);
            
            for (Transaction transaction : allTransactions) {
                if (transaction == null || transaction.getDate() == null) {
                    continue;
                }
                
                Calendar transactionCalendar = Calendar.getInstance();
                transactionCalendar.setTime(transaction.getDate());
                
                // Normalize transaction date để chỉ so sánh năm/tháng
                int transactionYear = transactionCalendar.get(Calendar.YEAR);
                int transactionMonth = transactionCalendar.get(Calendar.MONTH);
                
                // Chỉ thêm transaction nếu cùng năm và cùng tháng
                if (transactionYear == filterYear && transactionMonth == filterMonth) {
                    filteredByMonth.add(transaction);
                }
            }
            
            // Debug log
            android.util.Log.d("TransactionList", "Filtered transactions count: " + filteredByMonth.size());
        } else {
            filteredByMonth.addAll(allTransactions);
        }
        
        // Bước 2: Lọc theo danh mục nếu có
        if (selectedCategoryId != null) {
            // Lấy category name từ selectedCategoryId
            String selectedCategoryName = categoryIdToNameMap.get(selectedCategoryId);
            
            for (Transaction transaction : filteredByMonth) {
                String transactionCategory = transaction.getCategory();
                
                // So sánh với category ID hoặc category name
                boolean matches = false;
                if (selectedCategoryId.equals(transactionCategory)) {
                    matches = true;
                } else if (selectedCategoryName != null) {
                    // So sánh với category name
                    String transactionCategoryName = categoryIdToNameMap.get(transactionCategory);
                    if (selectedCategoryName.equals(transactionCategoryName) || 
                        selectedCategoryName.equals(transactionCategory)) {
                        matches = true;
                    }
                }
                
                if (matches) {
                    transactionList.add(transaction);
                }
            }
        } else {
            transactionList.addAll(filteredByMonth);
        }
        
        // Chỉ hiển thị expense transactions
        List<Transaction> expenseTransactions = new ArrayList<>();
        for (Transaction transaction : transactionList) {
            if (!transaction.isIncome()) {
                expenseTransactions.add(transaction);
            }
        }
        transactionList = expenseTransactions;
        
        // Sắp xếp theo ngày giảm dần (mới nhất trước)
        transactionList.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
        
        // Cập nhật UI
        if (isFilteringByMonth && selectedCalendar != null) {
            // Format trực tiếp từ selectedCalendar (đã được normalize trong DatePicker callback)
            // Debug log trước khi format
            android.util.Log.d("TransactionList", "Updating UI - selectedCalendar.MONTH: " + selectedCalendar.get(Calendar.MONTH));
            android.util.Log.d("TransactionList", "Updating UI - selectedCalendar.YEAR: " + selectedCalendar.get(Calendar.YEAR));
            android.util.Log.d("TransactionList", "Updating UI - selectedCalendar time: " + selectedCalendar.getTime().toString());
            
            String formattedText = monthYearFormat.format(selectedCalendar.getTime());
            android.util.Log.d("TransactionList", "Updating UI - Formatted text: " + formattedText);
            
            // Nếu có category name, hiển thị cả category và tháng/năm
            if (initialCategoryName != null) {
                tvMonthYear.setText(initialCategoryName + " - " + formattedText);
            } else {
                tvMonthYear.setText(formattedText);
            }
            btnResetFilter.setVisibility(View.VISIBLE);
        } else {
            // Nếu có category name, hiển thị tên category
            if (initialCategoryName != null) {
                tvMonthYear.setText(initialCategoryName);
            } else {
                tvMonthYear.setText(getString(R.string.all_transactions));
            }
            btnResetFilter.setVisibility(View.GONE);
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
        
        // Ẩn nút reset và hiển thị nút "Xem tất cả" nếu đang xem từ danh mục cụ thể
        if (selectedCategoryId != null) {
            if (btnResetFilter != null) {
                btnResetFilter.setVisibility(View.GONE);
            }
            if (btnViewAll != null) {
                btnViewAll.setVisibility(View.VISIBLE);
            }
        } else {
            if (btnViewAll != null) {
                btnViewAll.setVisibility(View.GONE);
            }
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
                // Nếu không có note, hiển thị số thứ tự
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
                // Lưu ý: selectedMonth từ DatePickerDialog là 0-based (0 = tháng 1, 10 = tháng 11, 11 = tháng 12)
                selectedCalendar.clear();
                selectedCalendar.set(Calendar.YEAR, selectedYear);
                selectedCalendar.set(Calendar.MONTH, selectedMonth); // selectedMonth đã là 0-based, set trực tiếp
                selectedCalendar.set(Calendar.DAY_OF_MONTH, 1);
                selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                selectedCalendar.set(Calendar.MINUTE, 0);
                selectedCalendar.set(Calendar.SECOND, 0);
                selectedCalendar.set(Calendar.MILLISECOND, 0);
                
                // Debug log
                android.util.Log.d("TransactionList", "DatePicker callback - selectedMonth (0-based): " + selectedMonth);
                android.util.Log.d("TransactionList", "DatePicker callback - selectedYear: " + selectedYear);
                android.util.Log.d("TransactionList", "After set - Calendar.MONTH: " + selectedCalendar.get(Calendar.MONTH));
                android.util.Log.d("TransactionList", "After set - Calendar.YEAR: " + selectedCalendar.get(Calendar.YEAR));
                android.util.Log.d("TransactionList", "Display will show: " + (selectedCalendar.get(Calendar.MONTH) + 1) + "/" + selectedCalendar.get(Calendar.YEAR));
                
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
     * Thêm transaction vào local list và refresh UI ngay lập tức
     */
    private void addTransactionLocally(Transaction transaction) {
        if (transaction == null) return;
        
        // Thêm vào allTransactions
        allTransactions.add(transaction);
        
        // Áp dụng filter để cập nhật transactionList
        applyFilter();
        
        // Refresh UI (displayTransactions đã xử lý empty state)
        displayTransactions();
    }
    
    /**
     * Cập nhật transaction trong local list và refresh UI ngay lập tức
     */
    private void updateTransactionLocally(Transaction transaction) {
        if (transaction == null || transaction.getId() == null) return;
        
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
     * Mở dialog để thêm giao dịch mới
     */
    private void openAddTransactionDialog() {
        AddTransactionFragment dialog = AddTransactionFragment.newInstance();
        dialog.setOnTransactionAddedListener(new AddTransactionFragment.OnTransactionAddedListener() {
            @Override
            public void onTransactionAdded() {
                // Fallback: reload từ Firestore nếu không có transaction object
                loadTransactions();
            }
            
            @Override
            public void onTransactionAdded(Transaction transaction) {
                // Thêm vào local list ngay lập tức, không cần reload
                addTransactionLocally(transaction);
            }
        });
        dialog.show(getParentFragmentManager(), "AddTransactionDialog");
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
                // Cập nhật trong local list ngay lập tức, không cần reload
                updateTransactionLocally(updatedTransaction);
            }
        });
        dialog.show(getParentFragmentManager(), "EditTransactionDialog");
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

