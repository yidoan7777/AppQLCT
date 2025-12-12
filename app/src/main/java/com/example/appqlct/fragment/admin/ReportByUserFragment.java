package com.example.appqlct.fragment.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.adapter.MonthReportAdapter;
import com.example.appqlct.adapter.UserReportAdapter;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.model.Budget;
import com.example.appqlct.model.Category;
import com.example.appqlct.model.Transaction;
import com.example.appqlct.model.User;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * ReportByUserFragment - Fragment hiển thị báo cáo theo từng người dùng
 */
public class ReportByUserFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private UserReportAdapter adapter;
    private List<UserReportAdapter.UserReportItem> reportItems;
    private FirebaseHelper firebaseHelper;
    private List<Category> expenseCategories; // Danh sách expense categories hợp lệ

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_by_user, container, false);
        
        initViews(view);
        initHelpers();
        setupRecyclerView();
        loadUserReports();
        
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmpty = view.findViewById(R.id.tvEmpty);
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        reportItems = new ArrayList<>();
        expenseCategories = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new UserReportAdapter(reportItems, item -> {
            // Hiển thị dialog chi tiết
            showUserReportDetailDialog(item);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Load báo cáo của tất cả users (chỉ user, không bao gồm admin)
     */
    private void loadUserReports() {
        // Load expense categories trước để lọc budgets hợp lệ
        loadExpenseCategories(() -> {
            loadUsersAndReports();
        });
    }
    
    /**
     * Load expense categories để lọc budgets hợp lệ
     */
    private void loadExpenseCategories(Runnable onComplete) {
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
                
                Log.d("ReportByUser", "Loaded " + expenseCategories.size() + " expense categories");
                
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Nếu không load được categories, vẫn tiếp tục (nhưng sẽ không filter)
                Log.w("ReportByUser", "Failed to load categories: " + error);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
    
    /**
     * Load users và tính toán báo cáo
     */
    private void loadUsersAndReports() {
        firebaseHelper.getAllUsers(new FirebaseHelper.OnUsersLoadedListener() {
            @Override
            public void onUsersLoaded(List<User> users) {
                if (!isAdded() || getContext() == null) return;
                
                reportItems.clear();
                
                // Lọc chỉ lấy users có role == "user" (không bao gồm admin)
                List<User> regularUsers = new ArrayList<>();
                for (User user : users) {
                    if (user.getRole() != null && user.getRole().equals("user")) {
                        regularUsers.add(user);
                    }
                }
                
                // Load transactions của từng user
                final int[] loadedCount = {0};
                final int totalUsers = regularUsers.size();
                
                if (totalUsers == 0) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    return;
                }
                
                for (User user : regularUsers) {
                    loadUserTransactions(user, () -> {
                        loadedCount[0]++;
                        if (loadedCount[0] >= totalUsers) {
                            // Đã load xong tất cả, cập nhật UI
                            adapter.notifyDataSetChanged();
                            if (reportItems.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Load transactions của một user và tính toán thống kê
     */
    private void loadUserTransactions(User user, Runnable onComplete) {
        // Load budgets và transactions cùng lúc để tính tổng ngân sách đúng
        String userId = user.getUid();
        Log.d("ReportByUser", "Loading budgets for user: " + userId + " (name: " + user.getName() + ")");
        firebaseHelper.getUserBudgets(userId, new FirebaseHelper.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                if (!isAdded() || getContext() == null) return;
                
                // DEBUG: Log số lượng budgets và userId của từng budget
                Log.d("ReportByUser", String.format("User %s: Loaded %d budgets", userId, budgets.size()));
                for (Budget budget : budgets) {
                    Log.d("ReportByUser", String.format("  Budget: userId=%s, category=%s, amount=%.0f, month=%d, year=%d", 
                        budget.getUserId(), budget.getCategoryName(), budget.getAmount(), budget.getMonth(), budget.getYear()));
                }
                
                // Sau đó load transactions để tính tổng ngân sách dựa trên các tháng có dữ liệu
                firebaseHelper.getUserTransactions(user.getUid(), new FirebaseHelper.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        if (!isAdded() || getContext() == null) return;
                        
                        // Tạo Set chứa tên các expense categories hợp lệ để kiểm tra
                        Set<String> validCategoryNames = new HashSet<>();
                        for (Category cat : expenseCategories) {
                            if (cat.getName() != null) {
                                validCategoryNames.add(cat.getName());
                            }
                        }
                        
                        // Loại bỏ budgets duplicate: nếu có nhiều budgets cho cùng category, month, year
                        // thì chỉ lấy budget mới nhất (dựa vào updatedAt hoặc createdAt)
                        // VÀ chỉ tính budgets cho các expense categories hợp lệ
                        Map<String, Budget> uniqueBudgets = new HashMap<>();
                        for (Budget budget : budgets) {
                            int year = budget.getYear();
                            int month = budget.getMonth();
                            
                            // Kiểm tra month và year hợp lệ
                            if (month < 1 || month > 12 || year < 2000 || year > 2100) {
                                continue;
                            }
                            
                            // CHỈ tính budgets cho các expense categories hợp lệ
                            String categoryName = budget.getCategoryName();
                            if (categoryName == null || !validCategoryNames.contains(categoryName)) {
                                Log.d("ReportByUser", String.format("SKIP budget - category không hợp lệ: %s (user: %s)", 
                                    categoryName, userId));
                                continue;
                            }
                            
                            // Tạo key duy nhất: categoryName_month_year
                            String uniqueKey = String.format(Locale.getDefault(), "%s_%04d-%02d", 
                                categoryName, year, month);
                            
                            Budget existing = uniqueBudgets.get(uniqueKey);
                            if (existing == null) {
                                uniqueBudgets.put(uniqueKey, budget);
                            } else {
                                // Nếu đã có, so sánh updatedAt để lấy budget mới nhất
                                Date existingDate = existing.getUpdatedAt() != null ? existing.getUpdatedAt() : existing.getCreatedAt();
                                Date currentDate = budget.getUpdatedAt() != null ? budget.getUpdatedAt() : budget.getCreatedAt();
                                if (existingDate == null || (currentDate != null && currentDate.after(existingDate))) {
                                    uniqueBudgets.put(uniqueKey, budget);
                                }
                            }
                        }
                        
                        // Nhóm budgets theo tháng (chỉ tính từ budgets unique)
                        Map<String, Double> budgetMap = new HashMap<>();
                        for (Budget budget : uniqueBudgets.values()) {
                            int year = budget.getYear();
                            int month = budget.getMonth();
                            
                            String monthKey = String.format(Locale.getDefault(), "%04d-%02d", year, month);
                            double currentBudget = budgetMap.getOrDefault(monthKey, 0.0);
                            budgetMap.put(monthKey, currentBudget + budget.getAmount());
                        }
                        
                        // Nhóm transactions theo tháng để xác định các tháng có dữ liệu
                        Map<String, Double> expenseMap = new HashMap<>();
                        for (Transaction transaction : transactions) {
                            if (transaction.getDate() == null) continue;
                            if (!"expense".equals(transaction.getType())) continue;
                            
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(transaction.getDate());
                            int year = cal.get(Calendar.YEAR);
                            int month = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH trả về 0-11
                            
                            String monthKey = String.format(Locale.getDefault(), "%04d-%02d", year, month);
                            double currentExpense = expenseMap.getOrDefault(monthKey, 0.0);
                            expenseMap.put(monthKey, currentExpense + transaction.getAmount());
                        }
                        
                        // Tính tổng ngân sách từ TẤT CẢ các tháng có budget (không chỉ các tháng có transaction)
                        // Điều này đảm bảo tổng budget phản ánh đúng tổng ngân sách của user
                        double totalBudget = 0;
                        for (Map.Entry<String, Double> entry : budgetMap.entrySet()) {
                            String monthKey = entry.getKey();
                            double monthBudget = entry.getValue();
                            totalBudget += monthBudget;
                            
                            // DEBUG: Log từng tháng có budget
                            double monthExpense = expenseMap.getOrDefault(monthKey, 0.0);
                            Log.d("ReportByUser", String.format("Tháng %s: budget=%.0f, expense=%.0f", 
                                monthKey, monthBudget, monthExpense));
                        }
                        
                        // Tính tổng chi tiêu từ tất cả transactions
                        double totalExpense = 0;
                        for (Double expense : expenseMap.values()) {
                            totalExpense += expense;
                        }
                        
                        // DEBUG: Log tổng kết
                        Log.d("ReportByUser", String.format("User %s: Tổng budget=%.0f (từ %d tháng có budget), Tổng expense=%.0f (từ %d tháng có transaction)", 
                            user.getUid(), totalBudget, budgetMap.size(), totalExpense, expenseMap.size()));
                        
                        UserReportAdapter.UserReportItem item = new UserReportAdapter.UserReportItem(
                            user, totalExpense, totalBudget, transactions.size()
                        );
                        reportItems.add(item);
                        
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Nếu lỗi load transactions, tính tổng budget từ budgets hợp lệ
                        // Tạo Set chứa tên các expense categories hợp lệ
                        Set<String> validCategoryNames = new HashSet<>();
                        for (Category cat : expenseCategories) {
                            if (cat.getName() != null) {
                                validCategoryNames.add(cat.getName());
                            }
                        }
                        
                        // Loại bỏ duplicate và chỉ tính budgets hợp lệ
                        Map<String, Budget> uniqueBudgets = new HashMap<>();
                        for (Budget budget : budgets) {
                            int year = budget.getYear();
                            int month = budget.getMonth();
                            
                            if (month < 1 || month > 12 || year < 2000 || year > 2100) {
                                continue;
                            }
                            
                            String categoryName = budget.getCategoryName();
                            if (categoryName == null || !validCategoryNames.contains(categoryName)) {
                                continue;
                            }
                            
                            String uniqueKey = String.format(Locale.getDefault(), "%s_%04d-%02d", 
                                categoryName, year, month);
                            
                            Budget existing = uniqueBudgets.get(uniqueKey);
                            if (existing == null) {
                                uniqueBudgets.put(uniqueKey, budget);
                            } else {
                                Date existingDate = existing.getUpdatedAt() != null ? existing.getUpdatedAt() : existing.getCreatedAt();
                                Date currentDate = budget.getUpdatedAt() != null ? budget.getUpdatedAt() : budget.getCreatedAt();
                                if (existingDate == null || (currentDate != null && currentDate.after(existingDate))) {
                                    uniqueBudgets.put(uniqueKey, budget);
                                }
                            }
                        }
                        
                        double totalBudget = 0;
                        for (Budget budget : uniqueBudgets.values()) {
                            totalBudget += budget.getAmount();
                        }
                        
                        UserReportAdapter.UserReportItem item = new UserReportAdapter.UserReportItem(
                            user, 0, totalBudget, 0
                        );
                        reportItems.add(item);
                        
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Nếu lỗi load budgets, chỉ load transactions
                firebaseHelper.getUserTransactions(user.getUid(), new FirebaseHelper.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        if (!isAdded() || getContext() == null) return;
                        
                        double totalExpense = 0;
                        
                        for (Transaction t : transactions) {
                            // CHỈ tính các transactions thực tế, không tính recurring transaction gốc
                            if ("expense".equals(t.getType()) && !t.isRecurring()) {
                                totalExpense += t.getAmount();
                            }
                        }
                        
                        UserReportAdapter.UserReportItem item = new UserReportAdapter.UserReportItem(
                            user, totalExpense, 0, transactions.size()
                        );
                        reportItems.add(item);
                        
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }

                    @Override
                    public void onError(String error2) {
                        // Nếu cả hai đều lỗi, thêm user với số liệu 0
                        UserReportAdapter.UserReportItem item = new UserReportAdapter.UserReportItem(
                            user, 0, 0, 0
                        );
                        reportItems.add(item);
                        
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                });
            }
        });
    }

    /**
     * Hiển thị dialog chi tiết báo cáo của user
     */
    private void showUserReportDetailDialog(UserReportAdapter.UserReportItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_user_report_detail, null);
        
        // Bind dữ liệu tổng quan
        TextView tvDetailUserName = dialogView.findViewById(R.id.tvDetailUserName);
        TextView tvDetailUserEmail = dialogView.findViewById(R.id.tvDetailUserEmail);
        TextView tvDetailUserRole = dialogView.findViewById(R.id.tvDetailUserRole);
        TextView tvDetailTotalExpense = dialogView.findViewById(R.id.tvDetailTotalExpense);
        TextView tvDetailTotalBudget = dialogView.findViewById(R.id.tvDetailTotalBudget);
        TextView tvDetailTotalRemaining = dialogView.findViewById(R.id.tvDetailTotalRemaining);
        TextView tvDetailTransactionCount = dialogView.findViewById(R.id.tvDetailTransactionCount);
        
        User user = item.getUser();
        tvDetailUserName.setText(user.getName());
        tvDetailUserEmail.setText(user.getEmail());
        tvDetailUserRole.setText(user.getRole().equals("admin") ? "Admin" : "User");
        tvDetailTotalExpense.setText(formatAmount(item.getTotalExpense()));
        tvDetailTotalBudget.setText(formatAmount(item.getTotalBudget()));
        
        double remaining = item.getTotalBudget() - item.getTotalExpense();
        tvDetailTotalRemaining.setText(formatAmount(remaining));
        // Đổi màu: đỏ nếu âm, xanh nếu dương
        if (remaining < 0) {
            tvDetailTotalRemaining.setTextColor(getResources().getColor(R.color.expense_color));
        } else {
            tvDetailTotalRemaining.setTextColor(getResources().getColor(R.color.income_color));
        }
        
        tvDetailTransactionCount.setText(String.valueOf(item.getTransactionCount()));
        
        // Load và hiển thị thống kê theo tháng
        loadMonthlyStatsForUser(user.getUid(), dialogView);
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Đóng", null)
                .create();
        dialog.show();
    }

    /**
     * Load và nhóm transactions theo tháng, sau đó hiển thị trong RecyclerView
     */
    private void loadMonthlyStatsForUser(String userId, View dialogView) {
        RecyclerView recyclerViewMonths = dialogView.findViewById(R.id.recyclerViewMonths);
        recyclerViewMonths.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Load budgets trước
        firebaseHelper.getUserBudgets(userId, new FirebaseHelper.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                if (!isAdded() || getContext() == null) return;
                
                // DEBUG: Log số lượng budgets
                Log.d("ReportByUser", "User: " + userId + " - Số lượng budgets: " + budgets.size());
                
                // Tạo Set chứa tên các expense categories hợp lệ để kiểm tra
                Set<String> validCategoryNames = new HashSet<>();
                for (Category cat : expenseCategories) {
                    if (cat.getName() != null) {
                        validCategoryNames.add(cat.getName());
                    }
                }
                
                // Loại bỏ budgets duplicate: nếu có nhiều budgets cho cùng category, month, year
                // thì chỉ lấy budget mới nhất (dựa vào updatedAt hoặc createdAt)
                // VÀ chỉ tính budgets cho các expense categories hợp lệ
                Map<String, Budget> uniqueBudgets = new HashMap<>();
                for (Budget budget : budgets) {
                    int year = budget.getYear();
                    int month = budget.getMonth();
                    
                    // Kiểm tra month và year hợp lệ
                    if (month < 1 || month > 12) {
                        Log.w("ReportByUser", String.format("SKIP budget - month không hợp lệ: %d (phải là 1-12)", month));
                        continue;
                    }
                    if (year < 2000 || year > 2100) {
                        Log.w("ReportByUser", String.format("SKIP budget - year không hợp lệ: %d", year));
                        continue;
                    }
                    
                    // CHỈ tính budgets cho các expense categories hợp lệ
                    String categoryName = budget.getCategoryName();
                    if (categoryName == null || !validCategoryNames.contains(categoryName)) {
                        Log.d("ReportByUser", String.format("SKIP budget - category không hợp lệ: %s (user: %s)", 
                            categoryName, userId));
                        continue;
                    }
                    
                    // Tạo key duy nhất: categoryName_month_year
                    String uniqueKey = String.format(Locale.getDefault(), "%s_%04d-%02d", 
                        categoryName, year, month);
                    
                    Budget existing = uniqueBudgets.get(uniqueKey);
                    if (existing == null) {
                        uniqueBudgets.put(uniqueKey, budget);
                    } else {
                        // Nếu đã có, so sánh updatedAt để lấy budget mới nhất
                        Date existingDate = existing.getUpdatedAt() != null ? existing.getUpdatedAt() : existing.getCreatedAt();
                        Date currentDate = budget.getUpdatedAt() != null ? budget.getUpdatedAt() : budget.getCreatedAt();
                        if (existingDate == null || (currentDate != null && currentDate.after(existingDate))) {
                            uniqueBudgets.put(uniqueKey, budget);
                            Log.d("ReportByUser", String.format("Thay thế budget duplicate: %s (cũ: %.0f, mới: %.0f)", 
                                uniqueKey, existing.getAmount(), budget.getAmount()));
                        }
                    }
                }
                
                // DEBUG: Log số lượng budgets sau khi loại bỏ duplicate
                Log.d("ReportByUser", "Số lượng budgets sau khi loại bỏ duplicate: " + uniqueBudgets.size());
                
                // Nhóm budgets theo tháng (chỉ tính từ budgets unique)
                Map<String, Double> budgetMap = new HashMap<>();
                for (Budget budget : uniqueBudgets.values()) {
                    int year = budget.getYear();
                    int month = budget.getMonth();
                    
                    // DEBUG: Log từng budget
                    Log.d("ReportByUser", String.format("Budget: category=%s, amount=%.0f, month=%d, year=%d", 
                        budget.getCategoryName(), budget.getAmount(), month, year));
                    
                    // Tạo monthKey với format "YYYY-MM" (month đã là 1-12)
                    String monthKey = String.format(Locale.getDefault(), "%04d-%02d", year, month);
                    double currentBudget = budgetMap.getOrDefault(monthKey, 0.0);
                    budgetMap.put(monthKey, currentBudget + budget.getAmount());
                    
                    // DEBUG: Log monthKey và tổng budget
                    Log.d("ReportByUser", String.format("monthKey=%s, tổng budget=%.0f (từ %.0f + %.0f)", 
                        monthKey, budgetMap.get(monthKey), currentBudget, budget.getAmount()));
                }
                
                // DEBUG: Log tất cả monthKeys trong budgetMap
                Log.d("ReportByUser", String.format("Tổng kết budgetMap: %d tháng có budget", budgetMap.size()));
                for (Map.Entry<String, Double> entry : budgetMap.entrySet()) {
                    Log.d("ReportByUser", String.format("  monthKey=%s, budget=%.0f", entry.getKey(), entry.getValue()));
                }
                
                // Sau đó load transactions
                firebaseHelper.getUserTransactions(userId, new FirebaseHelper.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        if (!isAdded() || getContext() == null) return;
                        
                        // Nhóm transactions theo tháng
                        Map<String, MonthReportAdapter.MonthReportItem> monthMap = new HashMap<>();
                        
                        for (Transaction transaction : transactions) {
                            if (transaction.getDate() == null) continue;
                            
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(transaction.getDate());
                            
                            // Tạo key cho tháng (YYYY-MM)
                            int year = cal.get(Calendar.YEAR);
                            int month = cal.get(Calendar.MONTH); // Calendar.MONTH trả về 0-11
                            
                            // Tạo monthKey với format "YYYY-MM" (month + 1 để có 1-12)
                            String monthKey = String.format(Locale.getDefault(), "%04d-%02d", year, month + 1);
                            
                            // DEBUG: Log monthKey từ transaction
                            Log.d("ReportByUser", String.format("Transaction date: %s, monthKey=%s", 
                                transaction.getDate().toString(), monthKey));
                            
                            MonthReportAdapter.MonthReportItem monthItem = monthMap.get(monthKey);
                            if (monthItem == null) {
                                // Lấy budget từ budgetMap
                                double monthBudget = budgetMap.getOrDefault(monthKey, 0.0);
                                
                                // DEBUG: Log khi tạo monthItem
                                Log.d("ReportByUser", String.format("Tạo monthItem mới: monthKey=%s, monthBudget=%.0f, budgetMap có key này? %s", 
                                    monthKey, monthBudget, budgetMap.containsKey(monthKey)));
                                
                                Calendar monthCalendar = Calendar.getInstance();
                                monthCalendar.set(year, month, 1);
                                
                                // DEBUG: Log Calendar values
                                Log.d("ReportByUser", String.format("Tạo Calendar: year=%d, month (Calendar)=%d, monthKey=%s", 
                                    year, month, monthKey));
                                
                                monthItem = new MonthReportAdapter.MonthReportItem(
                                    (Calendar) monthCalendar.clone(), 0, monthBudget, 0
                                );
                                monthMap.put(monthKey, monthItem);
                            }
                            
                            // Cập nhật thống kê (chỉ tính expense)
                            if ("expense".equals(transaction.getType())) {
                                // Cập nhật expense và count, giữ nguyên budget
                                double newExpense = monthItem.getTotalExpense() + transaction.getAmount();
                                int newCount = monthItem.getTransactionCount() + 1;
                                
                                // DEBUG: Log khi cập nhật expense
                                Log.d("ReportByUser", String.format("Cập nhật expense: monthKey=%s, expense=%.0f->%.0f, budget=%.0f", 
                                    monthKey, monthItem.getTotalExpense(), newExpense, monthItem.getTotalBudget()));
                                
                                monthItem = new MonthReportAdapter.MonthReportItem(
                                    monthItem.getMonthCalendar(),
                                    newExpense,
                                    monthItem.getTotalBudget(), // Giữ nguyên budget đã có
                                    newCount
                                );
                                monthMap.put(monthKey, monthItem);
                            }
                        }
                        
                        // Thêm các tháng có budget nhưng chưa có transaction
                        for (Map.Entry<String, Double> entry : budgetMap.entrySet()) {
                            String monthKey = entry.getKey();
                            if (!monthMap.containsKey(monthKey)) {
                                String[] parts = monthKey.split("-");
                                int year = Integer.parseInt(parts[0]);
                                int month = Integer.parseInt(parts[1]) - 1; // Parse từ "01"-"12" thành 0-11
                                
                                // DEBUG: Log khi thêm tháng chỉ có budget
                                Log.d("ReportByUser", String.format("Thêm tháng chỉ có budget: monthKey=%s, budget=%.0f", 
                                    monthKey, entry.getValue()));
                                
                                Calendar monthCalendar = Calendar.getInstance();
                                monthCalendar.set(year, month, 1);
                                
                                // DEBUG: Log Calendar values khi thêm tháng chỉ có budget
                                Log.d("ReportByUser", String.format("Tạo Calendar từ monthKey: year=%d, month (Calendar)=%d, monthKey=%s", 
                                    year, month, monthKey));
                                
                                monthMap.put(monthKey, new MonthReportAdapter.MonthReportItem(
                                    monthCalendar, 0, entry.getValue(), 0
                                ));
                            }
                        }
                        
                        // DEBUG: Log tổng kết
                        Log.d("ReportByUser", String.format("Tổng kết: %d tháng có dữ liệu, budgetMap có %d keys", 
                            monthMap.size(), budgetMap.size()));
                        for (Map.Entry<String, MonthReportAdapter.MonthReportItem> entry : monthMap.entrySet()) {
                            MonthReportAdapter.MonthReportItem item = entry.getValue();
                            Log.d("ReportByUser", String.format("Tháng %s: expense=%.0f, budget=%.0f", 
                                entry.getKey(), item.getTotalExpense(), item.getTotalBudget()));
                        }
                        
                        // Chuyển Map thành List và sắp xếp theo chi tiêu (tháng tiêu nhiều nhất trước)
                        List<MonthReportAdapter.MonthReportItem> monthItems = new ArrayList<>(monthMap.values());
                        Collections.sort(monthItems, (item1, item2) -> {
                            // Sắp xếp theo chi tiêu giảm dần (tháng tiêu nhiều nhất trước)
                            double expense1 = item1.getTotalExpense();
                            double expense2 = item2.getTotalExpense();
                            if (expense1 != expense2) {
                                return Double.compare(expense2, expense1); // Giảm dần
                            }
                            // Nếu chi tiêu bằng nhau, sắp xếp theo tháng mới nhất trước
                            Calendar cal1 = item1.getMonthCalendar();
                            Calendar cal2 = item2.getMonthCalendar();
                            if (cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)) {
                                return cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR);
                            }
                            return cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH);
                        });
                        
                        // Tính tổng chi tiêu của tất cả các tháng
                        double totalMonthlyExpense = 0;
                        for (MonthReportAdapter.MonthReportItem item : monthItems) {
                            totalMonthlyExpense += item.getTotalExpense();
                        }
                        
                        // Hiển thị tổng chi tiêu các tháng
                        TextView tvTotalMonthlyExpense = dialogView.findViewById(R.id.tvTotalMonthlyExpense);
                        if (tvTotalMonthlyExpense != null) {
                            tvTotalMonthlyExpense.setText("Tổng: " + formatAmount(totalMonthlyExpense));
                        }
                        
                        // Hiển thị trong RecyclerView
                        MonthReportAdapter monthAdapter = new MonthReportAdapter(monthItems, monthItem -> {
                            // Có thể mở dialog chi tiết hơn cho tháng này nếu cần
                            showMonthDetailDialog(userId, monthItem);
                        });
                        recyclerViewMonths.setAdapter(monthAdapter);
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || getContext() == null) return;
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Nếu lỗi load budgets, vẫn load transactions với budget = 0
                firebaseHelper.getUserTransactions(userId, new FirebaseHelper.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        if (!isAdded() || getContext() == null) return;
                        
                        Map<String, MonthReportAdapter.MonthReportItem> monthMap = new HashMap<>();
                        
                        for (Transaction transaction : transactions) {
                            if (transaction.getDate() == null) continue;
                            
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(transaction.getDate());
                            
                            int year = cal.get(Calendar.YEAR);
                            int month = cal.get(Calendar.MONTH);
                            
                            Calendar monthCalendar = Calendar.getInstance();
                            monthCalendar.set(year, month, 1);
                            
                            String monthKey = String.format(Locale.getDefault(), "%04d-%02d", year, month + 1);
                            
                            MonthReportAdapter.MonthReportItem monthItem = monthMap.get(monthKey);
                            if (monthItem == null) {
                                monthItem = new MonthReportAdapter.MonthReportItem(
                                    (Calendar) monthCalendar.clone(), 0, 0, 0
                                );
                                monthMap.put(monthKey, monthItem);
                            }
                            
                            if ("expense".equals(transaction.getType())) {
                                monthItem = new MonthReportAdapter.MonthReportItem(
                                    monthItem.getMonthCalendar(),
                                    monthItem.getTotalExpense() + transaction.getAmount(),
                                    0,
                                    monthItem.getTransactionCount() + 1
                                );
                                monthMap.put(monthKey, monthItem);
                            }
                        }
                        
                        List<MonthReportAdapter.MonthReportItem> monthItems = new ArrayList<>(monthMap.values());
                        Collections.sort(monthItems, (item1, item2) -> {
                            // Sắp xếp theo chi tiêu giảm dần (tháng tiêu nhiều nhất trước)
                            double expense1 = item1.getTotalExpense();
                            double expense2 = item2.getTotalExpense();
                            if (expense1 != expense2) {
                                return Double.compare(expense2, expense1); // Giảm dần
                            }
                            // Nếu chi tiêu bằng nhau, sắp xếp theo tháng mới nhất trước
                            Calendar cal1 = item1.getMonthCalendar();
                            Calendar cal2 = item2.getMonthCalendar();
                            if (cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)) {
                                return cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR);
                            }
                            return cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH);
                        });
                        
                        // Tính tổng chi tiêu của tất cả các tháng
                        double totalMonthlyExpense = 0;
                        for (MonthReportAdapter.MonthReportItem item : monthItems) {
                            totalMonthlyExpense += item.getTotalExpense();
                        }
                        
                        // Hiển thị tổng chi tiêu các tháng
                        TextView tvTotalMonthlyExpense = dialogView.findViewById(R.id.tvTotalMonthlyExpense);
                        if (tvTotalMonthlyExpense != null) {
                            tvTotalMonthlyExpense.setText("Tổng: " + formatAmount(totalMonthlyExpense));
                        }
                        
                        MonthReportAdapter monthAdapter = new MonthReportAdapter(monthItems, monthItem -> {
                            showMonthDetailDialog(userId, monthItem);
                        });
                        recyclerViewMonths.setAdapter(monthAdapter);
                    }

                    @Override
                    public void onError(String error2) {
                        if (!isAdded() || getContext() == null) return;
                    }
                });
            }
        });
    }

    /**
     * Hiển thị dialog chi tiết cho một tháng cụ thể
     */
    private void showMonthDetailDialog(String userId, MonthReportAdapter.MonthReportItem monthItem) {
        Calendar monthCal = monthItem.getMonthCalendar();
        
        // Tính toán ngày đầu và cuối tháng
        Calendar startCal = (Calendar) monthCal.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        Date startDate = startCal.getTime();
        
        Calendar endCal = (Calendar) monthCal.clone();
        endCal.add(Calendar.MONTH, 1);
        endCal.add(Calendar.DAY_OF_MONTH, -1);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        Date endDate = endCal.getTime();
        
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.ENGLISH);
        String monthText = monthFormat.format(startDate);
        
        // Load transactions for this month
        firebaseHelper.getMonthlyTransactions(userId, startDate, endDate,
                new FirebaseHelper.OnTransactionsLoadedListener() {
            @Override
            public void onTransactionsLoaded(List<Transaction> transactions) {
                if (!isAdded() || getContext() == null) return;
                
                // Create detail message
                double remaining = monthItem.getTotalBudget() - monthItem.getTotalExpense();
                StringBuilder detail = new StringBuilder();
                detail.append(monthText).append("\n\n");
                detail.append(getString(R.string.total_budget_label, formatAmount(monthItem.getTotalBudget()))).append("\n");
                detail.append(getString(R.string.total_expense_label, formatAmount(monthItem.getTotalExpense()))).append("\n");
                detail.append(getString(R.string.remaining_label_detail, formatAmount(remaining))).append("\n");
                detail.append(getString(R.string.transaction_count_label, monthItem.getTransactionCount())).append("\n\n");
                
                if (!transactions.isEmpty()) {
                    detail.append(getString(R.string.transaction_list_label)).append("\n");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    for (Transaction t : transactions) {
                        if ("expense".equals(t.getType())) {
                            String amountStr = String.format(Locale.getDefault(), "%,.0f", t.getAmount());
                            detail.append(String.format(Locale.getDefault(), 
                                "\n%s Expense: -%s VND%s",
                                dateFormat.format(t.getDate()),
                                amountStr,
                                t.getNote() != null && !t.getNote().isEmpty() ? "\n  Ghi chú: " + t.getNote() : ""
                            ));
                        }
                    }
                }
                
                new AlertDialog.Builder(requireContext())
                        .setTitle("Chi tiết " + monthText)
                        .setMessage(detail.toString())
                        .setPositiveButton("Đóng", null)
                        .show();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
            }
        });
    }

    private String formatAmount(double amount) {
        return String.format(Locale.getDefault(), "%,.0f VND", amount);
    }
}

