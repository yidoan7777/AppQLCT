package com.example.appqlct.fragment.user;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.adapter.BudgetAdapter;
import com.example.appqlct.adapter.CategoryViewAdapter;
import com.example.appqlct.helper.NotificationHelper;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.helper.SharedPreferencesHelper;
import com.example.appqlct.model.Budget;
import com.example.appqlct.model.Category;
import com.example.appqlct.model.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BudgetFragment - Quản lý ngân sách theo category
 * Hiển thị danh sách categories, khi click vào category sẽ mở dialog để nhập số tiền và chọn tháng
 */
public class BudgetFragment extends Fragment {
    private TextView tvMonthYear, tvMonthBudgetAmount, tvTotalBudget, tvTotalSpent, tvTotalRemaining, tvNoCategories;
    private RecyclerView recyclerViewCategories;
    private CategoryViewAdapter categoryAdapter;
    private List<Category> expenseCategories;
    private FirebaseHelper firebaseHelper;
    private SharedPreferencesHelper prefsHelper;
    private int currentMonth, currentYear;
    private int selectedMonth, selectedYear; // Tháng/năm đang được chọn để xem
    private ImageButton btnPreviousMonth, btnNextMonth;
    private Button btnResetToCurrentMonth;
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        initViews(view);
        initHelpers();
        setupRecyclerView();
        loadCategories();

        return view;
    }

    private void initViews(View view) {
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        tvMonthBudgetAmount = view.findViewById(R.id.tvMonthBudgetAmount);
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent);
        tvTotalRemaining = view.findViewById(R.id.tvTotalRemaining);
        tvNoCategories = view.findViewById(R.id.tvNoBudgets); // Reuse this TextView
        recyclerViewCategories = view.findViewById(R.id.recyclerViewBudgets); // Reuse RecyclerView
        btnPreviousMonth = view.findViewById(R.id.btnPreviousMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        btnResetToCurrentMonth = view.findViewById(R.id.btnResetToCurrentMonth);
        Button btnAddBudget = view.findViewById(R.id.btnAddBudget);
        btnAddBudget.setVisibility(View.GONE); // Hide add button, categories are clickable

        // Khởi tạo tháng/năm hiện tại
        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentYear = cal.get(Calendar.YEAR);
        selectedMonth = currentMonth;
        selectedYear = currentYear;
        
        // Hiển thị tháng/năm hiện tại
        updateMonthYearDisplay();
        
        // Nút chuyển tháng trước
        btnPreviousMonth.setOnClickListener(v -> {
            moveToPreviousMonth();
        });
        
        // Nút chuyển tháng sau
        btnNextMonth.setOnClickListener(v -> {
            moveToNextMonth();
        });
        
        // Click vào text để chọn tháng/năm
        tvMonthYear.setOnClickListener(v -> {
            showMonthYearPicker();
        });
        
        // Nút reset về tháng hiện tại
        btnResetToCurrentMonth.setOnClickListener(v -> {
            resetToCurrentMonth();
        });
        
        // Ẩn nút reset nếu đang ở tháng hiện tại
        updateResetButtonVisibility();
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        prefsHelper = new SharedPreferencesHelper(requireContext());
        expenseCategories = new ArrayList<>();
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryViewAdapter(expenseCategories);
        recyclerViewCategories.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerViewCategories.setAdapter(categoryAdapter);
        
        // Set click listener for categories
        categoryAdapter.setOnCategoryClickListener(category -> {
            showBudgetInputDialog(category);
        });
        
        // Set long click listener for deleting budget
        categoryAdapter.setOnCategoryLongClickListener((category, hasBudget) -> {
            if (hasBudget) {
                showDeleteBudgetDialog(category);
            }
            return true;
        });
    }

    /**
     * Load danh sách categories (chỉ expense categories)
     */
    private void loadCategories() {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                if (!isAdded() || getContext() == null) return;
                
                // Lọc chỉ lấy expense categories
                expenseCategories.clear();
                for (Category cat : categories) {
                    if ("expense".equals(cat.getType())) {
                        expenseCategories.add(cat);
                    }
                }
                
                // Cập nhật UI
                updateCategoriesRecyclerView();
                loadBudgetsAndCalculateSpending();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                String userId = prefsHelper.getUserId();
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.error_occurred, error));
                updateCategoriesRecyclerView();
            }
        });
    }

    /**
     * Load budgets và tính toán chi tiêu cho tháng được chọn
     */
    private void loadBudgetsAndCalculateSpending() {
        String userId = prefsHelper.getUserId();
        
        // Load ngân sách cho tháng được chọn
        firebaseHelper.getUserBudgets(userId, selectedMonth, selectedYear, 
                new FirebaseHelper.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgetList) {
                if (!isAdded() || getContext() == null) return;
                
                // Tính toán chi tiêu
                calculateSpendingForSelectedMonth(budgetList);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Không hiển thị lỗi, chỉ reset UI
                updateSummaryUI(0, 0);
            }
        });
    }

    /**
     * Tính toán chi tiêu cho tháng được chọn
     */
    private void calculateSpendingForSelectedMonth(List<Budget> budgets) {
        String userId = prefsHelper.getUserId();
        
        // Lấy transactions trong tháng được chọn
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear, selectedMonth - 1, 1, 0, 0, 0);
        Date startDate = calendar.getTime();
        
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        // Load cả transactions thực tế và recurring transactions
        firebaseHelper.getMonthlyTransactions(userId, startDate, endDate, 
                new FirebaseHelper.OnTransactionsLoadedListener() {
            @Override
            public void onTransactionsLoaded(List<Transaction> transactions) {
                if (!isAdded() || getContext() == null) return;
                
                // Load recurring transactions để tính toán giao dịch định kỳ
                firebaseHelper.getUserRecurringTransactions(userId, 
                        new FirebaseHelper.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> recurringTransactions) {
                        if (!isAdded() || getContext() == null) return;
                        
                        // Tính tổng chi tiêu từ transactions thực tế (KHÔNG bao gồm recurring transaction gốc)
                        double totalSpent = 0;
                        for (Transaction t : transactions) {
                            // CHỈ tính các transactions thực tế, không tính recurring transaction gốc
                            if ("expense".equals(t.getType()) && !t.isRecurring()) {
                                totalSpent += t.getAmount();
                            }
                        }
                        
                        // Tạo Set chứa tên các expense categories để kiểm tra
                        Set<String> validCategoryNames = new HashSet<>();
                        for (Category cat : expenseCategories) {
                            if (cat.getName() != null) {
                                validCategoryNames.add(cat.getName());
                            }
                        }
                        
                        // Tính toán budget và spending cho từng category
                        Map<String, Double> categoryBudgets = new HashMap<>();
                        Map<String, Double> categorySpent = new HashMap<>();
                        
                        // Map budgets theo category name
                        // Chỉ tính budgets cho các category có trong expenseCategories (đã loại bỏ trùng lặp)
                        // Nếu có nhiều budgets cho cùng category, lấy budget mới nhất (dựa vào updatedAt hoặc createdAt)
                        Map<String, Budget> categoryBudgetsMap = new HashMap<>();
                        for (Budget budget : budgets) {
                            String categoryName = budget.getCategoryName();
                            // Map tên category từ tiếng Việt sang tiếng Anh nếu cần
                            String mappedCategoryName = mapCategoryNameToEnglish(categoryName);
                            // Chỉ xử lý nếu category này hợp lệ
                            if (!validCategoryNames.contains(mappedCategoryName)) {
                                continue;
                            }
                            // Sử dụng tên đã map để lưu vào map
                            categoryName = mappedCategoryName;
                            
                            Budget existing = categoryBudgetsMap.get(categoryName);
                            if (existing == null) {
                                categoryBudgetsMap.put(categoryName, budget);
                            } else {
                                // Nếu đã có, so sánh updatedAt để lấy budget mới nhất
                                Date existingDate = existing.getUpdatedAt() != null ? existing.getUpdatedAt() : existing.getCreatedAt();
                                Date currentDate = budget.getUpdatedAt() != null ? budget.getUpdatedAt() : budget.getCreatedAt();
                                if (existingDate == null || (currentDate != null && currentDate.after(existingDate))) {
                                    categoryBudgetsMap.put(categoryName, budget);
                                }
                            }
                        }
                        
                        // Chuyển sang Map<categoryName, amount> để tính tổng
                        for (Map.Entry<String, Budget> entry : categoryBudgetsMap.entrySet()) {
                            categoryBudgets.put(entry.getKey(), entry.getValue().getAmount());
                        }
                        
                        // Tính tổng ngân sách từ categoryBudgets (chỉ các category hợp lệ, đã loại bỏ trùng lặp)
                        double totalBudget = 0;
                        for (Double amount : categoryBudgets.values()) {
                            totalBudget += amount;
                        }
                        
                        // Tính chi tiêu theo category từ transactions thực tế (KHÔNG bao gồm recurring transaction gốc)
                        for (Transaction t : transactions) {
                            // CHỈ tính các transactions thực tế, không tính recurring transaction gốc
                            if ("expense".equals(t.getType()) && !t.isRecurring()) {
                                String categoryName = normalizeCategoryName(t.getCategory());
                                double currentSpent = categorySpent.getOrDefault(categoryName, 0.0);
                                categorySpent.put(categoryName, currentSpent + t.getAmount());
                            }
                        }
                        
                        // Tính toán và thêm các giao dịch định kỳ cho tháng được chọn
                        // Tạo Set chứa các recurring transaction IDs đã có transactions thực tế trong tháng
                        // Chỉ tính recurring transaction nếu CHƯA có transaction thực tế nào được tạo từ nó
                        Set<String> recurringIdsWithActualTransactions = new HashSet<>();
                        for (Transaction t : transactions) {
                            // CHỈ kiểm tra các transactions thực tế (không phải recurring transaction gốc)
                            if (!t.isRecurring() && t.getRecurringTransactionId() != null && !t.getRecurringTransactionId().isEmpty()) {
                                recurringIdsWithActualTransactions.add(t.getRecurringTransactionId());
                            }
                        }
                        
                        Calendar selectedMonthCal = Calendar.getInstance();
                        selectedMonthCal.set(selectedYear, selectedMonth - 1, 1);
                        selectedMonthCal.set(Calendar.HOUR_OF_DAY, 0);
                        selectedMonthCal.set(Calendar.MINUTE, 0);
                        selectedMonthCal.set(Calendar.SECOND, 0);
                        selectedMonthCal.set(Calendar.MILLISECOND, 0);
                        
                        for (Transaction recurring : recurringTransactions) {
                            // Chỉ tính các recurring expense transactions
                            if (!recurring.isRecurring() || 
                                !"expense".equals(recurring.getType()) ||
                                recurring.getRecurringStartMonth() == null || 
                                recurring.getRecurringEndMonth() == null) {
                                continue;
                            }
                            
                            // Bỏ qua nếu đã có transactions thực tế được tạo từ recurring transaction này
                            if (recurring.getId() != null && recurringIdsWithActualTransactions.contains(recurring.getId())) {
                                continue;
                            }
                            
                            // Kiểm tra xem tháng được chọn có nằm trong khoảng thời gian định kỳ không
                            Calendar startCal = Calendar.getInstance();
                            startCal.setTime(recurring.getRecurringStartMonth());
                            startCal.set(Calendar.DAY_OF_MONTH, 1);
                            startCal.set(Calendar.HOUR_OF_DAY, 0);
                            startCal.set(Calendar.MINUTE, 0);
                            startCal.set(Calendar.SECOND, 0);
                            startCal.set(Calendar.MILLISECOND, 0);
                            
                            Calendar endCal = Calendar.getInstance();
                            endCal.setTime(recurring.getRecurringEndMonth());
                            endCal.set(Calendar.DAY_OF_MONTH, 1);
                            endCal.set(Calendar.HOUR_OF_DAY, 0);
                            endCal.set(Calendar.MINUTE, 0);
                            endCal.set(Calendar.SECOND, 0);
                            endCal.set(Calendar.MILLISECOND, 0);
                            
                            // Kiểm tra xem tháng được chọn có nằm trong khoảng thời gian không
                            if (isMonthInRange(selectedMonthCal, startCal, endCal)) {
                                // Thêm số tiền định kỳ vào tổng chi tiêu
                                totalSpent += recurring.getAmount();
                                
                                // Thêm vào chi tiêu theo category
                                String categoryName = normalizeCategoryName(recurring.getCategory());
                                double currentSpent = categorySpent.getOrDefault(categoryName, 0.0);
                                categorySpent.put(categoryName, currentSpent + recurring.getAmount());
                            }
                        }
                        
                        // Cập nhật adapter với thông tin budget và spending
                        categoryAdapter.updateBudgetData(categoryBudgets, categorySpent);
                        
                        // Cập nhật UI tổng quan
                        updateSummaryUI(totalBudget, totalSpent);
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || getContext() == null) return;
                        // Nếu không load được recurring transactions, vẫn tính với transactions thực tế
                        calculateSpendingWithoutRecurring(transactions, budgets);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Không hiển thị lỗi
            }
        });
    }
    
    /**
     * Kiểm tra xem một tháng có nằm trong khoảng thời gian không
     */
    private boolean isMonthInRange(Calendar monthToCheck, Calendar startMonth, Calendar endMonth) {
        // So sánh năm và tháng
        int checkYear = monthToCheck.get(Calendar.YEAR);
        int checkMonth = monthToCheck.get(Calendar.MONTH);
        
        int startYear = startMonth.get(Calendar.YEAR);
        int startMonthValue = startMonth.get(Calendar.MONTH);
        
        int endYear = endMonth.get(Calendar.YEAR);
        int endMonthValue = endMonth.get(Calendar.MONTH);
        
        // Tạo Calendar để so sánh
        Calendar check = Calendar.getInstance();
        check.set(checkYear, checkMonth, 1);
        
        Calendar start = Calendar.getInstance();
        start.set(startYear, startMonthValue, 1);
        
        Calendar end = Calendar.getInstance();
        end.set(endYear, endMonthValue, 1);
        
        return !check.before(start) && !check.after(end);
    }
    
    /**
     * Tính toán chi tiêu không bao gồm recurring transactions (fallback)
     */
    private void calculateSpendingWithoutRecurring(List<Transaction> transactions, List<Budget> budgets) {
        // Tính tổng chi tiêu (KHÔNG bao gồm recurring transaction gốc)
        double totalSpent = 0;
        for (Transaction t : transactions) {
            // CHỈ tính các transactions thực tế, không tính recurring transaction gốc
            if ("expense".equals(t.getType()) && !t.isRecurring()) {
                totalSpent += t.getAmount();
            }
        }
        
        // Tạo Set chứa tên các expense categories để kiểm tra
        Set<String> validCategoryNames = new HashSet<>();
        for (Category cat : expenseCategories) {
            if (cat.getName() != null) {
                validCategoryNames.add(cat.getName());
            }
        }
        
        // Tính toán budget và spending cho từng category
        Map<String, Double> categoryBudgets = new HashMap<>();
        Map<String, Double> categorySpent = new HashMap<>();
        
        // Map budgets theo category name
        Map<String, Budget> categoryBudgetsMap = new HashMap<>();
        for (Budget budget : budgets) {
            String categoryName = budget.getCategoryName();
            String mappedCategoryName = mapCategoryNameToEnglish(categoryName);
            if (!validCategoryNames.contains(mappedCategoryName)) {
                continue;
            }
            categoryName = mappedCategoryName;
            
            Budget existing = categoryBudgetsMap.get(categoryName);
            if (existing == null) {
                categoryBudgetsMap.put(categoryName, budget);
            } else {
                Date existingDate = existing.getUpdatedAt() != null ? existing.getUpdatedAt() : existing.getCreatedAt();
                Date currentDate = budget.getUpdatedAt() != null ? budget.getUpdatedAt() : budget.getCreatedAt();
                if (existingDate == null || (currentDate != null && currentDate.after(existingDate))) {
                    categoryBudgetsMap.put(categoryName, budget);
                }
            }
        }
        
        for (Map.Entry<String, Budget> entry : categoryBudgetsMap.entrySet()) {
            categoryBudgets.put(entry.getKey(), entry.getValue().getAmount());
        }
        
        double totalBudget = 0;
        for (Double amount : categoryBudgets.values()) {
            totalBudget += amount;
        }
        
        for (Transaction t : transactions) {
            // CHỈ tính các transactions thực tế, không tính recurring transaction gốc
            if ("expense".equals(t.getType()) && !t.isRecurring()) {
                String categoryName = normalizeCategoryName(t.getCategory());
                double currentSpent = categorySpent.getOrDefault(categoryName, 0.0);
                categorySpent.put(categoryName, currentSpent + t.getAmount());
            }
        }
        
        categoryAdapter.updateBudgetData(categoryBudgets, categorySpent);
        updateSummaryUI(totalBudget, totalSpent);
    }

    /**
     * Cập nhật UI tổng quan
     */
    private void updateSummaryUI(double totalBudget, double totalSpent) {
        // Cập nhật số tiền ngân sách tháng đó ở header
        if (selectedMonth == currentMonth && selectedYear == currentYear) {
            tvMonthBudgetAmount.setText(getString(R.string.this_month_budget, formatAmount(totalBudget)));
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(selectedYear, selectedMonth - 1, 1);
            String monthYearStr = monthYearFormat.format(cal.getTime());
            tvMonthBudgetAmount.setText(getString(R.string.month_budget, monthYearStr, formatAmount(totalBudget)));
        }
        
        // Cập nhật card tổng quan
        tvTotalBudget.setText(formatAmount(totalBudget));
        tvTotalSpent.setText(formatAmount(totalSpent));
        double remaining = totalBudget - totalSpent;
        tvTotalRemaining.setText(formatAmount(remaining));
        
        if (remaining < 0) {
            tvTotalRemaining.setTextColor(getResources().getColor(R.color.expense_color));
        } else {
            tvTotalRemaining.setTextColor(getResources().getColor(R.color.income_color));
        }
    }
    
    /**
     * Chuyển sang tháng trước
     */
    private void moveToPreviousMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth - 1, 1);
        cal.add(Calendar.MONTH, -1);
        selectedMonth = cal.get(Calendar.MONTH) + 1;
        selectedYear = cal.get(Calendar.YEAR);
        
        updateMonthYearDisplay();
        updateResetButtonVisibility();
        loadBudgetsAndCalculateSpending();
    }
    
    /**
     * Chuyển sang tháng sau
     */
    private void moveToNextMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth - 1, 1);
        cal.add(Calendar.MONTH, 1);
        selectedMonth = cal.get(Calendar.MONTH) + 1;
        selectedYear = cal.get(Calendar.YEAR);
        
        updateMonthYearDisplay();
        updateResetButtonVisibility();
        loadBudgetsAndCalculateSpending();
    }
    
    /**
     * Reset về tháng hiện tại
     */
    private void resetToCurrentMonth() {
        selectedMonth = currentMonth;
        selectedYear = currentYear;
        
        updateMonthYearDisplay();
        updateResetButtonVisibility();
        loadBudgetsAndCalculateSpending();
    }
    
    /**
     * Hiển thị dialog chọn tháng/năm
     */
    private void showMonthYearPicker() {
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth - 1, 1);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, selectedYear, selectedMonth, dayOfMonth) -> {
                BudgetFragment.this.selectedYear = selectedYear;
                BudgetFragment.this.selectedMonth = selectedMonth + 1;
                
                updateMonthYearDisplay();
                updateResetButtonVisibility();
                loadBudgetsAndCalculateSpending();
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            1 // Ngày mặc định là 1
        );
        
        datePickerDialog.show();
    }
    
    /**
     * Cập nhật hiển thị tháng/năm
     */
    private void updateMonthYearDisplay() {
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth - 1, 1);
        tvMonthYear.setText(monthYearFormat.format(cal.getTime()));
    }
    
    /**
     * Cập nhật hiển thị nút reset về tháng hiện tại
     */
    private void updateResetButtonVisibility() {
        if (selectedMonth == currentMonth && selectedYear == currentYear) {
            btnResetToCurrentMonth.setVisibility(View.GONE);
        } else {
            btnResetToCurrentMonth.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Cập nhật RecyclerView categories
     */
    private void updateCategoriesRecyclerView() {
        categoryAdapter.notifyDataSetChanged();
        
        if (expenseCategories.isEmpty()) {
            tvNoCategories.setVisibility(View.VISIBLE);
            recyclerViewCategories.setVisibility(View.GONE);
            tvNoCategories.setText(getString(R.string.no_expense_categories));
        } else {
            tvNoCategories.setVisibility(View.GONE);
            recyclerViewCategories.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hiển thị dialog nhập số tiền và chọn tháng cho category
     */
    private void showBudgetInputDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_budget_month, null);
        
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        
        // Thêm TextWatcher để format số tiền với dấu chấm (theo chuẩn Việt Nam)
        TextWatcher amountTextWatcher = new TextWatcher() {
            private boolean isFormatting = false;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) {
                    return;
                }
                
                isFormatting = true;
                
                // Loại bỏ tất cả ký tự không phải số (bao gồm dấu chấm, phẩy)
                String cleanString = s.toString().replaceAll("[^0-9]", "");
                
                if (!cleanString.isEmpty()) {
                    try {
                        // Parse số
                        long parsed = Long.parseLong(cleanString);
                        
                        // Format lại với dấu chấm (.) làm separator hàng nghìn (chuẩn Việt Nam)
                        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
                        symbols.setGroupingSeparator('.');
                        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
                        formatter.setGroupingSize(3);
                        String formatted = formatter.format(parsed);
                        
                        // Chỉ update nếu khác với text hiện tại
                        if (!formatted.equals(s.toString())) {
                            // Lưu số lượng digits trước cursor
                            int cursorPos = Math.min(etAmount.getSelectionStart(), s.length());
                            String textBeforeCursor = s.toString().substring(0, cursorPos);
                            int digitsBeforeCursor = textBeforeCursor.replaceAll("[^0-9]", "").length();
                            
                            // Thay thế text
                            s.replace(0, s.length(), formatted);
                            
                            // Tìm vị trí cursor mới dựa trên số lượng digits
                            int newSelection = formatted.length();
                            if (digitsBeforeCursor > 0) {
                                int digitCount = 0;
                                for (int i = 0; i < formatted.length(); i++) {
                                    if (Character.isDigit(formatted.charAt(i))) {
                                        digitCount++;
                                        if (digitCount == digitsBeforeCursor) {
                                            newSelection = i + 1;
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            // Đảm bảo selection nằm trong phạm vi hợp lệ
                            final int finalSelection = Math.max(0, Math.min(newSelection, formatted.length()));
                            
                            // Sử dụng post để set selection sau khi text đã được update hoàn toàn
                            etAmount.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String currentText = etAmount.getText().toString();
                                        int safeSelection = Math.max(0, Math.min(finalSelection, currentText.length()));
                                        etAmount.setSelection(safeSelection);
                                    } catch (Exception e) {
                                        // Nếu vẫn lỗi, đặt cursor ở cuối
                                        try {
                                            etAmount.setSelection(etAmount.getText().length());
                                        } catch (Exception ex) {
                                            // Bỏ qua nếu vẫn lỗi
                                        }
                                    }
                                }
                            });
                        }
                    } catch (NumberFormatException e) {
                        // Nếu không parse được, giữ nguyên
                    } catch (Exception e) {
                        // Bắt mọi exception khác để tránh crash
                        e.printStackTrace();
                    }
                } else {
                    // Nếu rỗng, xóa hết
                    s.clear();
                }
                
                isFormatting = false;
            }
        };
        etAmount.addTextChangedListener(amountTextWatcher);
        
        CheckBox checkboxAllMonths = dialogView.findViewById(R.id.checkboxAllMonths);
        CheckBox[] monthCheckboxes = new CheckBox[12];
        monthCheckboxes[0] = dialogView.findViewById(R.id.checkboxMonth1);
        monthCheckboxes[1] = dialogView.findViewById(R.id.checkboxMonth2);
        monthCheckboxes[2] = dialogView.findViewById(R.id.checkboxMonth3);
        monthCheckboxes[3] = dialogView.findViewById(R.id.checkboxMonth4);
        monthCheckboxes[4] = dialogView.findViewById(R.id.checkboxMonth5);
        monthCheckboxes[5] = dialogView.findViewById(R.id.checkboxMonth6);
        monthCheckboxes[6] = dialogView.findViewById(R.id.checkboxMonth7);
        monthCheckboxes[7] = dialogView.findViewById(R.id.checkboxMonth8);
        monthCheckboxes[8] = dialogView.findViewById(R.id.checkboxMonth9);
        monthCheckboxes[9] = dialogView.findViewById(R.id.checkboxMonth10);
        monthCheckboxes[10] = dialogView.findViewById(R.id.checkboxMonth11);
        monthCheckboxes[11] = dialogView.findViewById(R.id.checkboxMonth12);
        
        // Khi chọn "Tất cả các tháng", tự động chọn tất cả các checkbox
        checkboxAllMonths.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CheckBox cb : monthCheckboxes) {
                cb.setChecked(isChecked);
            }
        });
        
        // Khi bỏ chọn một tháng, bỏ chọn "Tất cả các tháng"
        for (CheckBox cb : monthCheckboxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    checkboxAllMonths.setChecked(false);
                } else {
                    // Kiểm tra xem tất cả đã được chọn chưa
                    boolean allChecked = true;
                    for (CheckBox checkBox : monthCheckboxes) {
                        if (!checkBox.isChecked()) {
                            allChecked = false;
                            break;
                        }
                    }
                    checkboxAllMonths.setChecked(allChecked);
                }
            });
        }
        
        AlertDialog dialog = builder.setView(dialogView)
                .setTitle(getString(R.string.budget_for_category, category.getName()))
                .setPositiveButton(getString(R.string.save), null) // Set null để tự xử lý
                .setNegativeButton(getString(R.string.cancel), null)
                .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String amountStr = etAmount.getText().toString().trim();
                
                if (TextUtils.isEmpty(amountStr)) {
                    etAmount.setError(getString(R.string.please_enter_amount));
                    return;
                }
                
                try {
                    // Remove all dots and commas before parsing (keep only numbers)
                    String cleanAmountStr = amountStr.replaceAll("[.,]", "");
                    double amount = Double.parseDouble(cleanAmountStr);
                    if (amount <= 0) {
                        etAmount.setError(getString(R.string.amount_must_be_greater_than_zero));
                        return;
                    }
                    
                    // Get list of selected months
                    List<Integer> selectedMonths = new ArrayList<>();
                    if (checkboxAllMonths.isChecked()) {
                        // Select all 12 months
                        for (int i = 1; i <= 12; i++) {
                            selectedMonths.add(i);
                        }
                    } else {
                        // Only get selected months
                        for (int i = 0; i < monthCheckboxes.length; i++) {
                            if (monthCheckboxes[i].isChecked()) {
                                selectedMonths.add(i + 1);
                            }
                        }
                    }
                    
                    if (selectedMonths.isEmpty()) {
                        String userId = prefsHelper.getUserId();
                        NotificationHelper.addInfoNotification(getContext(), userId, 
                                getString(R.string.please_select_at_least_one_month));
                        return;
                    }
                    
                    // Lưu ngân sách cho từng tháng
                    saveBudgetsForMonths(category.getName(), amount, selectedMonths);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    etAmount.setError(getString(R.string.invalid_amount));
                }
            });
        });
        
        dialog.show();
    }
    
    /**
     * Lưu ngân sách cho nhiều tháng
     * Kiểm tra và cập nhật nếu đã tồn tại, thêm mới nếu chưa có
     */
    private void saveBudgetsForMonths(String categoryName, double amount, List<Integer> months) {
        String userId = prefsHelper.getUserId();
        AtomicInteger processedCount = new AtomicInteger(0);
        int totalMonths = months.size();
        
        // Load tất cả budgets hiện tại để kiểm tra
        firebaseHelper.getUserBudgets(userId, new FirebaseHelper.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> existingBudgets) {
                if (!isAdded() || getContext() == null) return;
                
                // Xử lý từng tháng
                for (int month : months) {
                    // Tìm budget đã tồn tại cho category và tháng này (cho selectedYear)
                    Budget existingBudget = null;
                    for (Budget b : existingBudgets) {
                        if (b.getCategoryName().equals(categoryName) 
                                && b.getMonth() == month 
                                && b.getYear() == selectedYear) {
                            existingBudget = b;
                            break;
                        }
                    }
                    
                    if (existingBudget != null) {
                        // Cập nhật budget đã tồn tại
                        existingBudget.setAmount(amount);
                        existingBudget.setUpdatedAt(new Date());
                        firebaseHelper.updateBudget(existingBudget, task -> {
                            if (!isAdded() || getContext() == null) return;
                            int count = processedCount.incrementAndGet();
                            if (count == totalMonths) {
                                String userId = prefsHelper.getUserId();
                                NotificationHelper.addSuccessNotification(getContext(), userId, 
                                        getString(R.string.budget_updated_for_months, totalMonths));
                                loadBudgetsAndCalculateSpending();
                            }
                        });
                    } else {
                        // Add new budget - use selectedYear when adding budget
                        Budget newBudget = new Budget(null, userId, categoryName, amount, month, selectedYear);
                        firebaseHelper.addBudget(newBudget, task -> {
                            if (!isAdded() || getContext() == null) return;
                            int count = processedCount.incrementAndGet();
                            if (count == totalMonths) {
                                String userId = prefsHelper.getUserId();
                                NotificationHelper.addSuccessNotification(getContext(), userId, 
                                        getString(R.string.budget_saved_for_months, totalMonths));
                                loadBudgetsAndCalculateSpending();
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // If cannot load, still try to add new - use selectedYear when adding budget
                for (int month : months) {
                    Budget budget = new Budget(null, userId, categoryName, amount, month, selectedYear);
                    firebaseHelper.addBudget(budget, task -> {
                        if (!isAdded() || getContext() == null) return;
                        int count = processedCount.incrementAndGet();
                        if (count == totalMonths) {
                            Toast.makeText(getContext(), 
                                    getString(R.string.budget_saved_for_months, totalMonths), 
                                    Toast.LENGTH_SHORT).show();
                            loadBudgetsAndCalculateSpending();
                        }
                    });
                }
            }
        });
    }

    private String formatAmount(double amount) {
        return String.format(Locale.getDefault(), "%,.0f VND", amount);
    }
    
    /**
     * Map tên category từ tiếng Việt sang tiếng Anh
     * Để tương thích với budgets cũ có tên tiếng Việt
     */
    private String mapCategoryNameToEnglish(String categoryName) {
        if (categoryName == null) {
            return null;
        }
        
        // Map các tên category từ tiếng Việt sang tiếng Anh
        switch (categoryName) {
            case "Ăn uống":
                return "Food & Dining";
            case "Giao thông":
                return "Transportation";
            case "Giáo dục":
                return "Education";
            case "Tiện ích":
                return "Utilities";
            case "Giải trí":
                return "Entertainment";
            default:
                // Nếu không phải tên tiếng Việt, giữ nguyên
                return categoryName;
        }
    }
    
    /**
     * Hiển thị dialog xác nhận xóa budget
     */
    private void showDeleteBudgetDialog(Category category) {
        String userId = prefsHelper.getUserId();
        
        // Load budgets để tìm budget cần xóa
        firebaseHelper.getUserBudgets(userId, selectedMonth, selectedYear, 
                new FirebaseHelper.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                if (!isAdded() || getContext() == null) return;
                
                // Tìm budget cho category này trong tháng/năm được chọn
                Budget budgetToDelete = null;
                String categoryName = category.getName();
                String mappedCategoryName = mapCategoryNameToEnglish(categoryName);
                
                for (Budget budget : budgets) {
                    String budgetCategoryName = mapCategoryNameToEnglish(budget.getCategoryName());
                    if (budgetCategoryName.equals(mappedCategoryName) 
                            && budget.getMonth() == selectedMonth 
                            && budget.getYear() == selectedYear) {
                        budgetToDelete = budget;
                        break;
                    }
                }
                
                if (budgetToDelete == null) {
                    // Không tìm thấy budget
                    NotificationHelper.addInfoNotification(getContext(), userId, 
                            getString(R.string.no_budget_found_for_category));
                    return;
                }
                
                // Tạo biến final để sử dụng trong lambda
                final Budget finalBudgetToDelete = budgetToDelete;
                
                // Hiển thị dialog xác nhận
                new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.delete_budget))
                        .setMessage(getString(R.string.delete_budget_confirm, category.getName(), 
                                formatAmount(finalBudgetToDelete.getAmount())))
                        .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                            deleteBudgetForCategory(finalBudgetToDelete);
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.error_occurred, error));
            }
        });
    }
    
    /**
     * Xóa budget cho category
     */
    private void deleteBudgetForCategory(Budget budget) {
        if (budget == null || budget.getId() == null) {
            String userId = prefsHelper.getUserId();
            NotificationHelper.addErrorNotification(getContext(), userId, 
                    getString(R.string.cannot_delete_budget_invalid_id));
            return;
        }
        
        firebaseHelper.deleteBudget(budget.getId(), task -> {
            if (!isAdded() || getContext() == null) return;
            
            String userId = prefsHelper.getUserId();
            if (task.isSuccessful()) {
                NotificationHelper.addSuccessNotification(getContext(), userId, 
                        getString(R.string.budget_deleted));
                loadBudgetsAndCalculateSpending();
            } else {
                String error = task.getException() != null ? 
                        task.getException().getMessage() : getString(R.string.unknown_error);
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.delete_budget_failed, error));
            }
        });
    }
    
    /**
     * Normalize category name: có thể là ID hoặc tên
     * Nếu là ID thì cần map sang tên, nếu là tên thì giữ nguyên
     * Đảm bảo normalize đúng để matching với budgets
     */
    private String normalizeCategoryName(String category) {
        if (category == null || category.trim().isEmpty()) {
            return getString(R.string.unknown);
        }
        
        // Kiểm tra xem category có trong danh sách expenseCategories không
        for (Category cat : expenseCategories) {
            if (cat.getId() != null && cat.getId().equals(category)) {
                // Nếu category là ID, map sang tên
                String name = cat.getName();
                // Map tên từ tiếng Việt sang tiếng Anh nếu cần (để tương thích với budgets cũ)
                return mapCategoryNameToEnglish(name);
            }
            if (cat.getName() != null && cat.getName().equals(category)) {
                // Nếu category đã là tên, map từ tiếng Việt sang tiếng Anh nếu cần
                return mapCategoryNameToEnglish(cat.getName());
            }
        }
        
        // Nếu không tìm thấy trong expenseCategories, thử map từ tiếng Việt sang tiếng Anh
        String mappedName = mapCategoryNameToEnglish(category);
        // Kiểm tra lại xem tên đã map có trong expenseCategories không
        for (Category cat : expenseCategories) {
            if (cat.getName() != null && cat.getName().equals(mappedName)) {
                return mappedName;
            }
        }
        
        // Nếu không tìm thấy, có thể là ID của category đã bị xóa hoặc tên mới
        // Kiểm tra xem có phải là ID không (Firestore ID thường dài > 15 ký tự)
        if (category.length() > 15 && !category.contains(" ") && !category.contains("-")) {
            return getString(R.string.deleted_category_text);
        }
        
        // Giữ nguyên nếu là tên (đã thử map rồi)
        return mappedName;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật tháng/năm hiện tại (có thể đã thay đổi khi app ở background)
        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentYear = cal.get(Calendar.YEAR);
        
        // Cập nhật hiển thị và nút reset
        updateMonthYearDisplay();
        updateResetButtonVisibility();
        
        // Reload categories và ngân sách khi quay lại fragment
        if (firebaseHelper != null) {
            loadCategories();
        }
    }
}
