package com.example.appqlct.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appqlct.R;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.model.Budget;
import com.example.appqlct.model.Category;
import com.example.appqlct.model.Transaction;
import com.example.appqlct.model.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ReportTotalFragment - Fragment hiển thị báo cáo tổng quan (tất cả users)
 */
public class ReportTotalFragment extends Fragment {
    private TextView tvTotalUsers, tvTotalTransactions, tvTotalExpense, tvTotalBudget;
    private TextView tvExpenseCount, tvBudgetCount, tvTopCategory;
    private TextView tvCurrentMonth, tvMonthExpense, tvMonthBudget, tvMonthTransactions;
    private FirebaseHelper firebaseHelper;
    private Map<String, String> categoryIdToNameMap; // Map category ID sang tên
    private List<Category> expenseCategories; // Danh sách expense categories hợp lệ

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_total, container, false);
        
        initViews(view);
        initHelpers();
        loadReportData();
        
        return view;
    }

    private void initViews(View view) {
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        tvTotalTransactions = view.findViewById(R.id.tvTotalTransactions);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvTotalBudget = view.findViewById(R.id.tvTotalIncome); // Reuse same view ID
        tvExpenseCount = view.findViewById(R.id.tvExpenseCount);
        tvBudgetCount = view.findViewById(R.id.tvIncomeCount); // Reuse same view ID
        tvTopCategory = view.findViewById(R.id.tvTopCategory);
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        tvMonthExpense = view.findViewById(R.id.tvMonthExpense);
        tvMonthBudget = view.findViewById(R.id.tvMonthIncome); // Reuse same view ID
        tvMonthTransactions = view.findViewById(R.id.tvMonthTransactions);
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        categoryIdToNameMap = new HashMap<>();
        expenseCategories = new java.util.ArrayList<>();
        // Load categories để map ID sang tên
        loadCategories();
    }
    
    /**
     * Load tất cả categories để map ID sang tên và lọc expense categories
     */
    private void loadCategories() {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                if (!isAdded() || getContext() == null) return;
                
                categoryIdToNameMap.clear();
                expenseCategories.clear();
                
                for (Category category : categories) {
                    if (category.getId() != null && category.getName() != null) {
                        categoryIdToNameMap.put(category.getId(), category.getName());
                        // Cũng map tên sang tên (để hỗ trợ cả trường hợp category đã là tên)
                        categoryIdToNameMap.put(category.getName(), category.getName());
                        
                        // Lưu expense categories
                        if ("expense".equals(category.getType())) {
                            expenseCategories.add(category);
                        }
                    }
                }
            }

            @Override
            public void onError(String error) {
                // Nếu không load được categories, vẫn tiếp tục (sẽ hiển thị category ID hoặc tên gốc)
            }
        });
    }
    
    /**
     * Lấy tên category từ ID hoặc tên gốc
     */
    private String getCategoryName(String categoryIdOrName) {
        if (categoryIdOrName == null) return getString(R.string.unknown);
        // Nếu đã có trong map, trả về tên
        if (categoryIdToNameMap.containsKey(categoryIdOrName)) {
            return categoryIdToNameMap.get(categoryIdOrName);
        }
        // Nếu không có trong map, trả về giá trị gốc (có thể đã là tên)
        return categoryIdOrName;
    }

    /**
     * Load dữ liệu báo cáo tổng quan
     */
    private void loadReportData() {
        // Load users trước để lấy danh sách userId hợp lệ
        firebaseHelper.getAllUsers(new FirebaseHelper.OnUsersLoadedListener() {
            @Override
            public void onUsersLoaded(List<User> users) {
                if (!isAdded() || getContext() == null) return;
                
                // Tạo Set chứa các userId hợp lệ (chỉ users có role = "user", không phải admin)
                Set<String> validUserIds = new HashSet<>();
                int userCount = 0;
                for (User u : users) {
                    if (u.getUid() != null && !"admin".equals(u.getRole())) {
                        validUserIds.add(u.getUid());
                        userCount++;
                    }
                }
                tvTotalUsers.setText(String.valueOf(userCount));
                
                // Sau khi có danh sách users hợp lệ, load transactions và budgets
                loadTransactionsWithFilter(validUserIds);
                loadTotalBudgetWithFilter(validUserIds);
                
                // Load thống kê tháng hiện tại
                loadCurrentMonthStatsWithFilter(validUserIds);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Nếu không load được users, hiển thị 0
                tvTotalUsers.setText("0");
                tvTotalTransactions.setText("0");
                tvTotalExpense.setText(formatAmount(0));
                tvExpenseCount.setText(getString(R.string.transactions_count, 0));
                tvTopCategory.setText(getString(R.string.no_data));
                tvTotalBudget.setText(formatAmount(0));
                tvBudgetCount.setText("0 ngân sách");
            }
        });
    }
    
    /**
     * Load transactions và lọc theo userId hợp lệ
     */
    private void loadTransactionsWithFilter(Set<String> validUserIds) {
        firebaseHelper.getAllTransactions(new FirebaseHelper.OnTransactionsLoadedListener() {
            @Override
            public void onTransactionsLoaded(List<Transaction> transactions) {
                if (!isAdded() || getContext() == null) return;
                
                // Lọc chỉ lấy transactions của users hợp lệ
                List<Transaction> validTransactions = new java.util.ArrayList<>();
                for (Transaction t : transactions) {
                    if (t.getUserId() != null && validUserIds.contains(t.getUserId())) {
                        validTransactions.add(t);
                    }
                }
                
                int totalCount = validTransactions.size();
                int expenseCount = 0;
                double totalExpense = 0;
                
                // Thống kê theo category
                Map<String, Double> categoryExpense = new HashMap<>();
                
                for (Transaction t : validTransactions) {
                    // CHỈ tính các transactions thực tế, không tính recurring transaction gốc
                    if ("expense".equals(t.getType()) && !t.isRecurring()) {
                        expenseCount++;
                        totalExpense += t.getAmount();
                        
                        // Thống kê theo category
                        String category = t.getCategory();
                        categoryExpense.put(category, 
                                categoryExpense.getOrDefault(category, 0.0) + t.getAmount());
                    }
                }
                
                // Tìm category có chi tiêu nhiều nhất
                String topCategory = getString(R.string.no_data);
                double maxExpense = 0;
                String topCategoryKey = null;
                for (Map.Entry<String, Double> entry : categoryExpense.entrySet()) {
                    if (entry.getValue() > maxExpense) {
                        maxExpense = entry.getValue();
                        topCategoryKey = entry.getKey();
                    }
                }
                
                // Map category ID sang tên nếu có
                if (topCategoryKey != null) {
                    topCategory = getCategoryName(topCategoryKey);
                }
                
                tvTotalTransactions.setText(String.valueOf(totalCount));
                tvTotalExpense.setText(formatAmount(totalExpense));
                tvExpenseCount.setText(getString(R.string.transactions_count, expenseCount));
                if (maxExpense > 0) {
                    tvTopCategory.setText(topCategory + " (" + formatAmount(maxExpense) + ")");
                } else {
                    tvTopCategory.setText(getString(R.string.no_data));
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                tvTotalTransactions.setText("0");
                tvTotalExpense.setText(formatAmount(0));
                tvExpenseCount.setText(getString(R.string.transactions_count, 0));
                tvTopCategory.setText(getString(R.string.no_data));
            }
        });
    }

    /**
     * Load thống kê tháng hiện tại với filter userId
     */
    private void loadCurrentMonthStatsWithFilter(Set<String> validUserIds) {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1; // Lưu tháng hiện tại (1-12)
        int currentYear = calendar.get(Calendar.YEAR); // Lưu năm hiện tại
        
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startDate = calendar.getTime();
        
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.ENGLISH);
        tvCurrentMonth.setText(monthFormat.format(startDate));

        firebaseHelper.getAllTransactions(startDate, endDate, 
                new FirebaseHelper.OnTransactionsLoadedListener() {
            @Override
            public void onTransactionsLoaded(List<Transaction> transactions) {
                if (!isAdded() || getContext() == null) return;
                
                // Lọc chỉ lấy transactions của users hợp lệ
                List<Transaction> validTransactions = new java.util.ArrayList<>();
                for (Transaction t : transactions) {
                    if (t.getUserId() != null && validUserIds.contains(t.getUserId())) {
                        validTransactions.add(t);
                    }
                }
                
                int transactionCount = validTransactions.size();
                double totalExpense = 0;
                
                for (Transaction t : validTransactions) {
                    // CHỈ tính các transactions thực tế, không tính recurring transaction gốc
                    if ("expense".equals(t.getType()) && !t.isRecurring()) {
                        totalExpense += t.getAmount();
                    }
                }
                
                tvMonthExpense.setText(formatAmount(totalExpense));
                // Hiển thị tổng số giao dịch (cả income và expense)
                tvMonthTransactions.setText(String.valueOf(transactionCount));
                
                // Load ngân sách tháng hiện tại
                loadCurrentMonthBudgetWithFilter(currentMonth, currentYear, validUserIds);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                tvMonthExpense.setText(formatAmount(0));
                tvMonthTransactions.setText("0");
                tvMonthBudget.setText(formatAmount(0));
            }
        });
    }

    /**
     * Load tổng ngân sách từ tất cả budgets (chỉ tính expense categories hợp lệ và users hợp lệ)
     */
    private void loadTotalBudgetWithFilter(Set<String> validUserIds) {
        firebaseHelper.getAllBudgets(new FirebaseHelper.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                if (!isAdded() || getContext() == null) return;
                
                // Tạo Set chứa tên các expense categories hợp lệ
                Set<String> validCategoryNames = new HashSet<>();
                for (Category cat : expenseCategories) {
                    if (cat.getName() != null) {
                        validCategoryNames.add(cat.getName());
                    }
                }
                
                // Loại bỏ duplicate budgets (cùng userId, category, month, year)
                // VÀ chỉ tính budgets cho các expense categories hợp lệ VÀ users hợp lệ
                Map<String, Budget> uniqueBudgets = new HashMap<>();
                for (Budget budget : budgets) {
                    // CHỈ tính budgets của users hợp lệ
                    if (budget.getUserId() == null || !validUserIds.contains(budget.getUserId())) {
                        continue;
                    }
                    
                    // Kiểm tra month và year hợp lệ
                    if (budget.getMonth() < 1 || budget.getMonth() > 12 || 
                        budget.getYear() < 2000 || budget.getYear() > 2100) {
                        continue;
                    }
                    
                    // CHỈ tính budgets cho các expense categories hợp lệ
                    String categoryName = budget.getCategoryName();
                    if (categoryName == null || !validCategoryNames.contains(categoryName)) {
                        continue;
                    }
                    
                    String uniqueKey = budget.getUserId() + "_" + categoryName + "_" 
                            + budget.getMonth() + "_" + budget.getYear();
                    Budget existing = uniqueBudgets.get(uniqueKey);
                    if (existing == null) {
                        uniqueBudgets.put(uniqueKey, budget);
                    } else {
                        // Lấy budget mới nhất dựa vào updatedAt
                        Date currentDate = budget.getUpdatedAt() != null ? budget.getUpdatedAt() : budget.getCreatedAt();
                        Date existingDate = existing.getUpdatedAt() != null ? existing.getUpdatedAt() : existing.getCreatedAt();
                        if (currentDate != null && existingDate != null && currentDate.after(existingDate)) {
                            uniqueBudgets.put(uniqueKey, budget);
                        }
                    }
                }
                
                // Tính tổng ngân sách
                double totalBudget = 0;
                for (Budget budget : uniqueBudgets.values()) {
                    totalBudget += budget.getAmount();
                }
                
                tvTotalBudget.setText(formatAmount(totalBudget));
                tvBudgetCount.setText(uniqueBudgets.size() + " ngân sách");
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                tvTotalBudget.setText(formatAmount(0));
                tvBudgetCount.setText("0 ngân sách");
            }
        });
    }

    /**
     * Load ngân sách tháng hiện tại (chỉ tính expense categories hợp lệ và users hợp lệ)
     */
    private void loadCurrentMonthBudgetWithFilter(int month, int year, Set<String> validUserIds) {
        firebaseHelper.getAllBudgets(new FirebaseHelper.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                if (!isAdded() || getContext() == null) return;
                
                // Tạo Set chứa tên các expense categories hợp lệ
                Set<String> validCategoryNames = new HashSet<>();
                for (Category cat : expenseCategories) {
                    if (cat.getName() != null) {
                        validCategoryNames.add(cat.getName());
                    }
                }
                
                // Lọc budgets theo tháng/năm và chỉ tính expense categories hợp lệ VÀ users hợp lệ
                Map<String, Budget> uniqueBudgets = new HashMap<>();
                for (Budget budget : budgets) {
                    // CHỈ tính budgets của users hợp lệ
                    if (budget.getUserId() == null || !validUserIds.contains(budget.getUserId())) {
                        continue;
                    }
                    
                    // Kiểm tra month và year hợp lệ
                    if (budget.getMonth() != month || budget.getYear() != year) {
                        continue;
                    }
                    
                    // CHỈ tính budgets cho các expense categories hợp lệ
                    String categoryName = budget.getCategoryName();
                    if (categoryName == null || !validCategoryNames.contains(categoryName)) {
                        continue;
                    }
                    
                    String uniqueKey = budget.getUserId() + "_" + categoryName;
                    Budget existing = uniqueBudgets.get(uniqueKey);
                    if (existing == null) {
                        uniqueBudgets.put(uniqueKey, budget);
                    } else {
                        Date currentDate = budget.getUpdatedAt() != null ? budget.getUpdatedAt() : budget.getCreatedAt();
                        Date existingDate = existing.getUpdatedAt() != null ? existing.getUpdatedAt() : existing.getCreatedAt();
                        if (currentDate != null && existingDate != null && currentDate.after(existingDate)) {
                            uniqueBudgets.put(uniqueKey, budget);
                        }
                    }
                }
                
                // Tính tổng ngân sách tháng hiện tại
                double monthBudget = 0;
                for (Budget budget : uniqueBudgets.values()) {
                    monthBudget += budget.getAmount();
                }
                
                tvMonthBudget.setText(formatAmount(monthBudget));
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                tvMonthBudget.setText(formatAmount(0));
            }
        });
    }

    private String formatAmount(double amount) {
        return String.format(Locale.getDefault(), "%,.0f VND", amount);
    }
}

