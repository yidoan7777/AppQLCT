package com.example.appqlct.fragment.user;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.adapter.TransactionAdapter;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.helper.NotificationHelper;
import com.example.appqlct.helper.SharedPreferencesHelper;
import com.example.appqlct.model.Budget;
import com.example.appqlct.model.Category;
import com.example.appqlct.model.Transaction;

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

/**
 * DashboardFragment - Màn hình tổng quan hiển thị thống kê và giao dịch gần đây
 * Đây là màn hình mặc định sau khi đăng nhập
 */
public class DashboardFragment extends Fragment {
    // Constants
    private static final double BUDGET_WARNING_THRESHOLD = 80.0; // Ngưỡng cảnh báo: 80%
    private static final int RECENT_TRANSACTIONS_COUNT = 5; // Số giao dịch gần đây hiển thị
    
    // Views
    private TextView tvBudget, tvTotalExpense, tvRemaining, tvMonthYear;
    private RecyclerView recyclerViewRecent;
    
    // Adapters & Lists
    private TransactionAdapter adapter;
    private List<Transaction> recentTransactions;
    private List<Category> expenseCategories;
    
    // Helpers
    private FirebaseHelper firebaseHelper;
    private SharedPreferencesHelper prefsHelper;
    
    // State
    private boolean hasShownWarning = false; // Flag để chỉ hiển thị cảnh báo một lần mỗi lần load fragment
    private boolean needsRefresh = false; // Flag để đánh dấu cần refresh khi resume
    
    // Interface để thông báo cho MainActivity
    public interface BudgetWarningListener {
        void onBudgetWarning(double budget, double expense, double percentage);
        void onBudgetWarningCleared();
    }
    
    private BudgetWarningListener budgetWarningListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        initViews(view);
        initHelpers();
        setupRecyclerView();
        resetWarningState();
        loadDashboardData();

        return view;
    }
    
    /**
     * Reset trạng thái cảnh báo khi fragment được tạo mới hoặc resume
     */
    private void resetWarningState() {
        hasShownWarning = false;
    }

    private void initViews(View view) {
        tvBudget = view.findViewById(R.id.tvBudget);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvRemaining = view.findViewById(R.id.tvRemaining);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        recyclerViewRecent = view.findViewById(R.id.recyclerViewRecent);

        // Floating Action Button để thêm giao dịch mới
        view.findViewById(R.id.fabAddTransaction).setOnClickListener(v -> {
            openAddTransactionDialog();
        });
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        prefsHelper = new SharedPreferencesHelper(requireContext());
        recentTransactions = new ArrayList<>();
        expenseCategories = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(recentTransactions, transaction -> {
            // Xử lý khi click vào transaction
        });
        
        // Ẩn nút Edit/Delete ở phần giao dịch gần đây (chỉ hiển thị, không cho sửa/xóa)
        adapter.setShowEditDeleteButtons(false);
        
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewRecent.setAdapter(adapter);
    }

    /**
     * Load dữ liệu dashboard (thống kê tháng hiện tại và giao dịch gần đây)
     */
    private void loadDashboardData() {
        String userId = prefsHelper.getUserId();
        
        // Display current month/year
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
        tvMonthYear.setText(sdf.format(new Date()));

        // Load categories trước, sau đó mới load budgets để đảm bảo chỉ tính budgets hợp lệ
        loadCategoriesAndBudgets(userId);
    }
    
    /**
     * Load categories và budgets, chỉ tính budgets cho các expense categories hợp lệ
     */
    private void loadCategoriesAndBudgets(String userId) {
        // Load categories trước
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
                
                // Sau khi có categories, load budgets
                loadBudgetsForDashboard(userId);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Nếu không load được categories, vẫn load budgets (nhưng sẽ không filter)
                loadBudgetsForDashboard(userId);
            }
        });
    }
    
    /**
     * Load budgets và tính tổng ngân sách (chỉ tính cho các expense categories hợp lệ)
     */
    private void loadBudgetsForDashboard(String userId) {
        // Lấy tháng/năm hiện tại
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);

        // Load ngân sách cho tháng hiện tại
        firebaseHelper.getUserBudgets(userId, currentMonth, currentYear, 
                new FirebaseHelper.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                if (!isAdded() || getContext() == null) return;
                
                // Tạo Set chứa tên các expense categories để kiểm tra
                Set<String> validCategoryNames = new HashSet<>();
                for (Category cat : expenseCategories) {
                    if (cat.getName() != null) {
                        validCategoryNames.add(cat.getName());
                    }
                }
                
                // Loại bỏ trùng lặp và chỉ tính budgets cho các category hợp lệ
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
                Map<String, Double> categoryBudgets = new HashMap<>();
                for (Map.Entry<String, Budget> entry : categoryBudgetsMap.entrySet()) {
                    categoryBudgets.put(entry.getKey(), entry.getValue().getAmount());
                }
                
                // Tính tổng ngân sách từ categoryBudgets (chỉ các category hợp lệ, đã loại bỏ trùng lặp)
                double totalBudget = 0;
                for (Double amount : categoryBudgets.values()) {
                    totalBudget += amount;
                }
                
                // Load transactions để tính tổng chi
                loadTransactionsAndCalculate(userId, totalBudget);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Nếu không có ngân sách, vẫn load transactions với budget = 0
                loadTransactionsAndCalculate(userId, 0);
            }
        });
    }

    /**
     * Load transactions và tính toán thống kê
     */
    private void loadTransactionsAndCalculate(String userId, double totalBudget) {
        // Lấy transactions trong tháng hiện tại
        Calendar calendar = Calendar.getInstance();
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
                        double totalExpense = calculateTotalExpense(transactions);
                        
                        // Tạo Set chứa các recurring transaction IDs đã có transactions thực tế trong tháng
                        // Chỉ tính recurring transaction nếu CHƯA có transaction thực tế nào được tạo từ nó
                        Set<String> recurringIdsWithActualTransactions = new HashSet<>();
                        for (Transaction t : transactions) {
                            // CHỈ kiểm tra các transactions thực tế (không phải recurring transaction gốc)
                            if (!t.isRecurring() && t.getRecurringTransactionId() != null && !t.getRecurringTransactionId().isEmpty()) {
                                recurringIdsWithActualTransactions.add(t.getRecurringTransactionId());
                            }
                        }
                        
                        // Tính toán và thêm các giao dịch định kỳ cho tháng hiện tại
                        Calendar currentMonthCal = Calendar.getInstance();
                        currentMonthCal.set(Calendar.DAY_OF_MONTH, 1);
                        currentMonthCal.set(Calendar.HOUR_OF_DAY, 0);
                        currentMonthCal.set(Calendar.MINUTE, 0);
                        currentMonthCal.set(Calendar.SECOND, 0);
                        currentMonthCal.set(Calendar.MILLISECOND, 0);
                        
                        for (Transaction recurring : recurringTransactions) {
                            if (!recurring.isRecurring() || 
                                recurring.getRecurringStartMonth() == null || 
                                recurring.getRecurringEndMonth() == null) {
                                continue;
                            }
                            
                            // Bỏ qua nếu đã có transactions thực tế được tạo từ recurring transaction này
                            // Logic này đảm bảo: nếu transaction đã được tạo (dù có bị xóa hay không), 
                            // thì không tính lại số tiền của recurring transaction
                            if (recurring.getId() != null && recurringIdsWithActualTransactions.contains(recurring.getId())) {
                                continue;
                            }
                            
                            // Kiểm tra xem tháng hiện tại có nằm trong khoảng thời gian định kỳ không
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
                            
                            // Kiểm tra xem tháng hiện tại có nằm trong khoảng thời gian không
                            if (isMonthInRange(currentMonthCal, startCal, endCal) && 
                                "expense".equals(recurring.getType())) {
                                // Thêm số tiền định kỳ vào tổng chi tiêu
                                // CHỈ tính nếu chưa có transaction thực tế nào được tạo từ recurring transaction này
                                totalExpense += recurring.getAmount();
                            }
                        }
                        
                        // Tính số tiền còn lại
                        double remaining = totalBudget - totalExpense;
                        
                        // Hiển thị thống kê lên UI
                        displayStats(totalBudget, totalExpense, remaining);
                        
                        // Kiểm tra và hiển thị cảnh báo ngân sách nếu cần
                        checkAndShowBudgetWarning(totalBudget, totalExpense);
                        
                        loadRecentTransactions(userId);
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || getContext() == null) return;
                        // Nếu không load được recurring transactions, vẫn tính với transactions thực tế
                        calculateAndDisplayStats(transactions, totalBudget);
                        loadRecentTransactions(userId);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Hiển thị với budget và expense = 0
                calculateAndDisplayStats(new ArrayList<>(), totalBudget);
                String userId = prefsHelper.getUserId();
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.error) + ": " + error);
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
        
        // Kiểm tra xem tháng được chọn có nằm trong khoảng [start, end] không
        return !check.before(start) && !check.after(end);
    }

    /**
     * Tính toán và hiển thị thống kê (ngân sách, tổng chi, còn lại)
     */
    private void calculateAndDisplayStats(List<Transaction> transactions, double totalBudget) {
        // Tính tổng chi tiêu
        double totalExpense = calculateTotalExpense(transactions);
        
        // Tính số tiền còn lại
        double remaining = totalBudget - totalExpense;
        
        // Hiển thị thống kê lên UI
        displayStats(totalBudget, totalExpense, remaining);
        
        // Kiểm tra và hiển thị cảnh báo ngân sách nếu cần
        checkAndShowBudgetWarning(totalBudget, totalExpense);
    }
    
    /**
     * Tính tổng chi tiêu từ danh sách giao dịch
     * CHỈ tính các transactions thực tế (không phải recurring transaction gốc)
     * Recurring transaction gốc (isRecurring = true) sẽ được tính riêng trong logic khác
     */
    private double calculateTotalExpense(List<Transaction> transactions) {
        double totalExpense = 0;
        for (Transaction t : transactions) {
            // CHỈ tính các transactions thực tế (isRecurring = false)
            // Recurring transaction gốc (isRecurring = true) không được tính ở đây
            // vì nó sẽ được tính riêng trong logic recurring transactions
            if ("expense".equals(t.getType()) && !t.isRecurring()) {
                totalExpense += t.getAmount();
            }
        }
        return totalExpense;
    }
    
    /**
     * Hiển thị thống kê lên các TextView
     */
    private void displayStats(double totalBudget, double totalExpense, double remaining) {
        tvBudget.setText(formatAmount(totalBudget));
        tvTotalExpense.setText(formatAmount(totalExpense));
        tvRemaining.setText(formatAmount(remaining));
        
        // Đổi màu còn lại: đỏ nếu âm, xanh nếu dương
        int colorRes = remaining < 0 ? R.color.expense_color : R.color.income_color;
        tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }
    
    /**
     * Kiểm tra và hiển thị cảnh báo ngân sách nếu chi tiêu đạt ngưỡng cảnh báo
     */
    private void checkAndShowBudgetWarning(double totalBudget, double totalExpense) {
        // Chỉ kiểm tra nếu có ngân sách
        if (totalBudget <= 0) {
            // Nếu không có ngân sách, reset cảnh báo
            if (hasShownWarning && budgetWarningListener != null) {
                hasShownWarning = false;
                budgetWarningListener.onBudgetWarningCleared();
            }
            return;
        }
        
        double percentage = (totalExpense / totalBudget) * 100;
        if (percentage >= BUDGET_WARNING_THRESHOLD) {
            // Thông báo cho MainActivity để hiển thị icon notification (badge số)
            if (budgetWarningListener != null) {
                budgetWarningListener.onBudgetWarning(totalBudget, totalExpense, percentage);
            }
            
            // Không hiển thị dialog nữa, chỉ cập nhật badge số trên icon thông báo
            hasShownWarning = true;
        } else {
            // Nếu không còn vượt 80%, reset cảnh báo
            if (hasShownWarning && budgetWarningListener != null) {
                hasShownWarning = false;
                budgetWarningListener.onBudgetWarningCleared();
            }
        }
    }
    
    /**
     * Set listener để thông báo cho MainActivity khi vượt 80% ngân sách
     */
    public void setBudgetWarningListener(BudgetWarningListener listener) {
        this.budgetWarningListener = listener;
    }

    /**
     * Load giao dịch gần đây (5 giao dịch mới nhất của tháng hiện tại)
     */
    private void loadRecentTransactions(String userId) {
        // Lấy transactions trong tháng hiện tại
        Calendar calendar = Calendar.getInstance();
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

        firebaseHelper.getMonthlyTransactions(userId, startDate, endDate, 
                new FirebaseHelper.OnTransactionsLoadedListener() {
            @Override
            public void onTransactionsLoaded(List<Transaction> transactions) {
                if (!isAdded() || getContext() == null) return;
                
                // Sắp xếp theo ngày giảm dần (mới nhất trước)
                transactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
                
                recentTransactions.clear();
                int count = Math.min(RECENT_TRANSACTIONS_COUNT, transactions.size());
                for (int i = 0; i < count; i++) {
                    recentTransactions.add(transactions.get(i));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                // Xử lý lỗi - không cần hiển thị Toast vì đây là load phụ
                if (!isAdded() || getContext() == null) return;
            }
        });
    }

    /**
     * Mở dialog để thêm giao dịch mới
     */
    private void openAddTransactionDialog() {
        AddTransactionFragment dialog = AddTransactionFragment.newInstance();
        dialog.setOnTransactionAddedListener(new AddTransactionFragment.OnTransactionAddedListener() {
            @Override
            public void onTransactionAdded() {
                // Đánh dấu cần refresh
                needsRefresh = true;
                
                // Fallback: reload từ Firestore nếu không có transaction object
                // Sử dụng Handler với delay để đảm bảo fragment đã resume
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded() && getView() != null && isResumed()) {
                        loadDashboardData();
                        needsRefresh = false;
                    } else if (isAdded()) {
                        // Nếu chưa resume, đánh dấu để refresh trong onResume
                        // needsRefresh đã được set = true ở trên
                    }
                }, 400);
            }
            
            @Override
            public void onTransactionAdded(Transaction transaction) {
                // Đánh dấu cần refresh
                needsRefresh = true;
                
                // Đảm bảo refresh sau khi dialog dismiss hoàn toàn
                // Sử dụng Handler với delay để đảm bảo fragment đã resume
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded() && getView() != null && isResumed()) {
                        addTransactionAndRefresh(transaction);
                        needsRefresh = false;
                    } else if (isAdded()) {
                        // Nếu chưa resume, đánh dấu để refresh trong onResume
                        // needsRefresh đã được set = true ở trên
                    }
                }, 400);
            }
        });
        dialog.show(getParentFragmentManager(), "AddTransactionDialog");
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
     * Thêm transaction vào dashboard và refresh UI ngay lập tức
     */
    private void addTransactionAndRefresh(Transaction transaction) {
        if (transaction == null || !isAdded() || getView() == null) return;
        
        // Luôn reload toàn bộ dữ liệu để đảm bảo tính chính xác
        // Vì cần tính lại tổng chi tiêu từ tất cả transactions trong tháng
        loadDashboardData();
    }
    
    /**
     * Refresh danh sách giao dịch gần đây với transaction mới
     */
    private void refreshRecentTransactions(Transaction newTransaction) {
        if (newTransaction == null || !isAdded() || getView() == null || adapter == null) return;
        
        // Thêm transaction mới vào đầu danh sách
        recentTransactions.add(0, newTransaction);
        
        // Giữ chỉ 5 giao dịch gần đây nhất
        if (recentTransactions.size() > RECENT_TRANSACTIONS_COUNT) {
            recentTransactions.remove(recentTransactions.size() - 1);
        }
        
        // Sắp xếp lại theo ngày giảm dần
        recentTransactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
        
        // Cập nhật adapter
        adapter.notifyDataSetChanged();
    }

    /**
     * Format số tiền
     */
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

    @Override
    public void onResume() {
        super.onResume();
        // Reset trạng thái cảnh báo
        resetWarningState();
        
        // Nếu có flag needsRefresh, refresh ngay lập tức
        if (needsRefresh && firebaseHelper != null) {
            needsRefresh = false;
            loadDashboardData();
        } else if (firebaseHelper != null) {
            // Nếu không có flag, vẫn reload để đảm bảo dữ liệu mới nhất
            // Nhưng chỉ reload nếu fragment đã được tạo view
            if (getView() != null) {
                loadDashboardData();
            }
        }
    }
    
    /**
     * Hiển thị dialog cảnh báo khi chi tiêu đạt 80% ngân sách
     */
    private void showBudgetWarningDialog(double totalBudget, double totalSpent, double percentage) {
        if (!isAdded() || getContext() == null) return;
        
        String message = String.format(Locale.getDefault(),
                getString(R.string.budget_warning_detailed),
                percentage,
                totalBudget,
                totalSpent,
                totalBudget - totalSpent);
        
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.budget_warning_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.understood), null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}

