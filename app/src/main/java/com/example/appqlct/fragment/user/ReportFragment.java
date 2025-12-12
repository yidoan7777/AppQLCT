package com.example.appqlct.fragment.user;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.adapter.CategoryReportAdapter;
import com.example.appqlct.adapter.TransactionAdapter;
import com.example.appqlct.fragment.user.AddTransactionFragment;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.helper.SharedPreferencesHelper;
import com.example.appqlct.model.Budget;
import com.example.appqlct.model.Category;
import com.example.appqlct.model.Transaction;
import android.app.AlertDialog;
import android.widget.Toast;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.firestore.ListenerRegistration;

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
 * ReportFragment - Hiển thị báo cáo chi tiết theo tháng
 * Bao gồm: Tổng chi, Ngân sách, Còn lại, Số lượng giao dịch, Biểu đồ tròn, Chi tiết theo danh mục, Danh sách giao dịch
 */
public class ReportFragment extends Fragment {
    private PieChart pieChart;
    private TextView tvMonthYear, tvTotalExpense, tvBudget, tvRemaining, tvTransactionCount, tvNoChartData, tvNoCategoryData;
    private TextView tvExpenseChange, tvTransactionChange, tvNoPreviousMonthData;
    private ImageView ivExpenseChange, ivTransactionChange;
    private LinearLayout layoutExpenseComparison, layoutTransactionComparison;
    private ImageButton btnPreviousMonth, btnNextMonth;
    private SwitchCompat switchViewMode;
    private RecyclerView recyclerViewCategories, recyclerViewTransactions;
    private CategoryReportAdapter categoryAdapter;
    private TransactionAdapter transactionAdapter;
    private FirebaseHelper firebaseHelper;
    private SharedPreferencesHelper prefsHelper;
    private Map<String, String> categoryIdToNameMap;
    private List<Category> expenseCategories;
    private List<CategoryReportItem> categoryReportItems;
    private List<Transaction> transactionList;
    private ListenerRegistration transactionsListener;
    
    // Dữ liệu tháng trước để so sánh
    private double previousMonthTotalExpense = 0;
    private int previousMonthTransactionCount = 0;
    private boolean hasPreviousMonthData = false;
    
    // Lưu dữ liệu tháng hiện tại để cập nhật so sánh sau khi load xong tháng trước
    private double currentMonthTotalExpense = 0;
    private int currentMonthTransactionCount = 0;
    
    // Tháng/năm hiện tại đang xem
    private Calendar selectedCalendar;
    private Calendar minCalendar; // Giới hạn tối thiểu (2 năm trước)
    private SimpleDateFormat monthYearFormat;
    private SimpleDateFormat yearFormat;
    
    // Chế độ xem: true = năm, false = tháng
    private boolean isYearlyMode = false;

    // Màu sắc cho các danh mục
    private int[] colors = {
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#673AB7"), // Deep Purple
            Color.parseColor("#3F51B5"), // Indigo
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#795548")  // Brown
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        initViews(view);
        initHelpers();
        setupPieChart();
        loadReportData();

        return view;
    }

    private void initViews(View view) {
        pieChart = view.findViewById(R.id.pieChart);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        btnPreviousMonth = view.findViewById(R.id.btnPreviousMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvBudget = view.findViewById(R.id.tvBudget);
        tvRemaining = view.findViewById(R.id.tvRemaining);
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        tvNoChartData = view.findViewById(R.id.tvNoChartData);
        tvNoCategoryData = view.findViewById(R.id.tvNoCategoryData);
        recyclerViewCategories = view.findViewById(R.id.recyclerViewCategories);
        recyclerViewTransactions = view.findViewById(R.id.recyclerViewTransactions);
        
        // So sánh với tháng trước
        tvExpenseChange = view.findViewById(R.id.tvExpenseChange);
        tvTransactionChange = view.findViewById(R.id.tvTransactionChange);
        tvNoPreviousMonthData = view.findViewById(R.id.tvNoPreviousMonthData);
        ivExpenseChange = view.findViewById(R.id.ivExpenseChange);
        ivTransactionChange = view.findViewById(R.id.ivTransactionChange);
        layoutExpenseComparison = view.findViewById(R.id.layoutExpenseComparison);
        layoutTransactionComparison = view.findViewById(R.id.layoutTransactionComparison);
        
        // Khởi tạo Calendar
        selectedCalendar = Calendar.getInstance();
        minCalendar = Calendar.getInstance();
        minCalendar.add(Calendar.YEAR, -2); // 2 năm trước
        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
        yearFormat = new SimpleDateFormat("yyyy", Locale.ENGLISH);
        
        // Khởi tạo switch
        switchViewMode = view.findViewById(R.id.switchViewMode);
        
        // Setup click listeners
        setupMonthNavigation();
        setupViewModeToggle();
        
        categoryReportItems = new ArrayList<>();
        categoryAdapter = new CategoryReportAdapter(categoryReportItems);
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewCategories.setAdapter(categoryAdapter);
        
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(transactionList, transaction -> {
            // Có thể xử lý click vào transaction nếu cần
        });
        
        // Thiết lập listeners cho edit và delete
        transactionAdapter.setOnTransactionEditListener(transaction -> {
            editTransaction(transaction);
        });
        
        transactionAdapter.setOnTransactionDeleteListener(transaction -> {
            deleteTransaction(transaction);
        });
        
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewTransactions.setAdapter(transactionAdapter);
    }
    
    /**
     * Thiết lập toggle chuyển đổi chế độ xem
     */
    private void setupViewModeToggle() {
        switchViewMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isYearlyMode = isChecked;
            // Reset dữ liệu tháng trước khi chuyển chế độ
            hasPreviousMonthData = false;
            previousMonthTotalExpense = 0;
            previousMonthTransactionCount = 0;
            // Cập nhật hiển thị và reload dữ liệu
            updateMonthDisplay();
            if (isYearlyMode) {
                loadReportDataForYear(selectedCalendar.get(Calendar.YEAR));
            } else {
                int month = selectedCalendar.get(Calendar.MONTH) + 1;
                int year = selectedCalendar.get(Calendar.YEAR);
                loadReportDataForMonth(month, year);
            }
        });
    }
    
    /**
     * Thiết lập điều hướng tháng/năm
     */
    private void setupMonthNavigation() {
        // Nút Previous
        btnPreviousMonth.setOnClickListener(v -> {
            if (isYearlyMode) {
                selectedCalendar.add(Calendar.YEAR, -1);
                if (isDateValid(selectedCalendar)) {
                    updateMonthDisplay();
                    loadReportDataForYear(selectedCalendar.get(Calendar.YEAR));
                } else {
                    selectedCalendar.add(Calendar.YEAR, 1);
                }
            } else {
                selectedCalendar.add(Calendar.MONTH, -1);
                if (isDateValid(selectedCalendar)) {
                    updateMonthDisplay();
                    loadReportDataForMonth(selectedCalendar.get(Calendar.MONTH) + 1, 
                                         selectedCalendar.get(Calendar.YEAR));
                } else {
                    selectedCalendar.add(Calendar.MONTH, 1);
                }
            }
        });
        
        // Nút Next
        btnNextMonth.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            if (isYearlyMode) {
                selectedCalendar.add(Calendar.YEAR, 1);
                // Không cho phép chọn năm trong tương lai
                if (selectedCalendar.get(Calendar.YEAR) <= now.get(Calendar.YEAR)) {
                    if (isDateValid(selectedCalendar)) {
                        updateMonthDisplay();
                        loadReportDataForYear(selectedCalendar.get(Calendar.YEAR));
                    } else {
                        selectedCalendar.add(Calendar.YEAR, -1);
                    }
                } else {
                    selectedCalendar.add(Calendar.YEAR, -1);
                }
            } else {
                selectedCalendar.add(Calendar.MONTH, 1);
                // Không cho phép chọn tháng trong tương lai
                if (selectedCalendar.before(now) || isSameMonth(selectedCalendar, now)) {
                    if (isDateValid(selectedCalendar)) {
                        updateMonthDisplay();
                        loadReportDataForMonth(selectedCalendar.get(Calendar.MONTH) + 1, 
                                             selectedCalendar.get(Calendar.YEAR));
                    } else {
                        selectedCalendar.add(Calendar.MONTH, -1);
                    }
                } else {
                    selectedCalendar.add(Calendar.MONTH, -1);
                }
            }
        });
        
        // Click vào TextView để mở DatePicker
        tvMonthYear.setOnClickListener(v -> showDatePicker());
        
        // Cập nhật hiển thị ban đầu
        updateMonthDisplay();
    }
    
    /**
     * Kiểm tra xem ngày có hợp lệ không (trong khoảng 2 năm gần nhất)
     */
    private boolean isDateValid(Calendar calendar) {
        return !calendar.before(minCalendar);
    }
    
    /**
     * Kiểm tra xem 2 Calendar có cùng tháng/năm không
     */
    private boolean isSameMonth(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }
    
    /**
     * Cập nhật hiển thị tháng/năm
     */
    private void updateMonthDisplay() {
        if (isYearlyMode) {
            tvMonthYear.setText(yearFormat.format(selectedCalendar.getTime()));
        } else {
            tvMonthYear.setText(monthYearFormat.format(selectedCalendar.getTime()));
        }
        
        // Disable/enable các nút dựa trên giới hạn
        Calendar now = Calendar.getInstance();
        if (isYearlyMode) {
            Calendar prevYear = (Calendar) selectedCalendar.clone();
            prevYear.add(Calendar.YEAR, -1);
            btnPreviousMonth.setEnabled(isDateValid(prevYear));
            btnNextMonth.setEnabled(selectedCalendar.get(Calendar.YEAR) < now.get(Calendar.YEAR));
        } else {
            Calendar prevMonth = (Calendar) selectedCalendar.clone();
            prevMonth.add(Calendar.MONTH, -1);
            btnPreviousMonth.setEnabled(isDateValid(prevMonth));
            btnNextMonth.setEnabled(!isSameMonth(selectedCalendar, now));
        }
    }
    
    /**
     * Hiển thị DatePicker để chọn tháng/năm
     */
    private void showDatePicker() {
        Calendar maxCalendar = Calendar.getInstance(); // Không cho chọn tương lai
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                if (isYearlyMode) {
                    selected.set(year, 0, 1); // Đặt tháng 1 cho năm
                } else {
                    selected.set(year, month, 1);
                }
                
                // Kiểm tra giới hạn
                if (selected.after(maxCalendar)) {
                    selected = (Calendar) maxCalendar.clone();
                }
                if (selected.before(minCalendar)) {
                    selected = (Calendar) minCalendar.clone();
                }
                
                if (isYearlyMode) {
                    selectedCalendar.set(year, 0, 1);
                    updateMonthDisplay();
                    loadReportDataForYear(year);
                } else {
                    selectedCalendar.set(year, month, 1);
                    updateMonthDisplay();
                    loadReportDataForMonth(month + 1, year);
                }
            },
            selectedCalendar.get(Calendar.YEAR),
            isYearlyMode ? 0 : selectedCalendar.get(Calendar.MONTH),
            1
        );
        
        // Set giới hạn
        datePickerDialog.getDatePicker().setMinDate(minCalendar.getTimeInMillis());
        datePickerDialog.getDatePicker().setMaxDate(maxCalendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        prefsHelper = new SharedPreferencesHelper(requireContext());
        categoryIdToNameMap = new HashMap<>();
        expenseCategories = new ArrayList<>();
    }

    /**
     * Thiết lập PieChart
     */
    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setCenterText(getString(R.string.expense));
        pieChart.setCenterTextSize(14f);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        pieChart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        pieChart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        pieChart.getLegend().setDrawInside(false);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
    }

    /**
     * Load dữ liệu báo cáo từ Firestore (tháng hiện tại)
     */
    private void loadReportData() {
        Calendar now = Calendar.getInstance();
        loadReportDataForMonth(now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR));
    }
    
    /**
     * Load dữ liệu báo cáo cho năm cụ thể
     */
    private void loadReportDataForYear(int year) {
        String userId = prefsHelper.getUserId();
        
        // Tạo Calendar cho năm được chọn (từ 1/1 đến 31/12)
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, 0, 1); // Tháng 1, ngày 1
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();
        
        // Ngày cuối cùng của năm (31/12)
        calendar.set(year, 11, 31); // Tháng 12, ngày 31
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        // Load categories trước để map ID với tên và lưu expense categories
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                categoryIdToNameMap.clear();
                expenseCategories.clear();
                for (Category category : categories) {
                    if (category.getId() != null && category.getName() != null) {
                        // Map ID -> Name
                        categoryIdToNameMap.put(category.getId(), category.getName());
                        // Map Name -> Name (để normalize, tránh trường hợp có cả ID và Name)
                        categoryIdToNameMap.put(category.getName(), category.getName());
                        // Lưu expense categories để filter budgets
                        if ("expense".equals(category.getType())) {
                            expenseCategories.add(category);
                        }
                    }
                }
                // Sau khi load categories, load transactions
                loadTransactionsAndBudgetsForYear(userId, startDate, endDate, year);
            }

            @Override
            public void onError(String error) {
                // Vẫn tiếp tục load transactions dù không load được categories
                loadTransactionsAndBudgetsForYear(userId, startDate, endDate, year);
            }
        });
    }
    
    /**
     * Load transactions và budgets với real-time listener cho năm
     */
    private void loadTransactionsAndBudgetsForYear(String userId, Date startDate, Date endDate, int year) {
        // Hủy listener cũ nếu có
        if (transactionsListener != null) {
            transactionsListener.remove();
            transactionsListener = null;
        }
        
        // Reload categories mỗi khi transactions thay đổi để đảm bảo map luôn đầy đủ
        reloadCategoriesAndThenLoadTransactionsForYear(userId, startDate, endDate, year);
    }
    
    /**
     * Reload categories rồi mới load transactions cho năm
     */
    private void reloadCategoriesAndThenLoadTransactionsForYear(String userId, Date startDate, Date endDate, int year) {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                // Cập nhật lại map categories và expense categories
                categoryIdToNameMap.clear();
                expenseCategories.clear();
                for (Category category : categories) {
                    if (category.getId() != null && category.getName() != null) {
                        // Map ID -> Name
                        categoryIdToNameMap.put(category.getId(), category.getName());
                        // Map Name -> Name (để normalize, tránh trường hợp có cả ID và Name)
                        categoryIdToNameMap.put(category.getName(), category.getName());
                        // Lưu expense categories để filter budgets
                        if ("expense".equals(category.getType())) {
                            expenseCategories.add(category);
                        }
                    }
                }
                
                // Sau khi reload categories, load transactions với real-time listener
                if (transactionsListener != null) {
                    transactionsListener.remove();
                    transactionsListener = null;
                }
                
                transactionsListener = firebaseHelper.listenMonthlyTransactions(userId, startDate, endDate, 
                        new FirebaseHelper.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        if (!isAdded() || getContext() == null) return;
                        
                        // Load budgets cho cả năm (tổng hợp tất cả các tháng)
                        loadBudgetsForYear(userId, year, transactions);
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || getContext() == null) return;
                        // Hiển thị với dữ liệu rỗng
                        calculateAndDisplayReport(new ArrayList<>(), new ArrayList<>(), null, year);
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Nếu không load được categories, vẫn tiếp tục load transactions
                if (transactionsListener != null) {
                    transactionsListener.remove();
                    transactionsListener = null;
                }
                
                transactionsListener = firebaseHelper.listenMonthlyTransactions(userId, startDate, endDate, 
                        new FirebaseHelper.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        if (!isAdded() || getContext() == null) return;
                        
                        loadBudgetsForYear(userId, year, transactions);
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || getContext() == null) return;
                        calculateAndDisplayReport(new ArrayList<>(), new ArrayList<>(), null, year);
                    }
                });
            }
        });
    }
    
    /**
     * Load budgets cho cả năm (tổng hợp từ tất cả các tháng)
     */
    private void loadBudgetsForYear(String userId, int year, List<Transaction> transactions) {
        // Load budgets cho tất cả 12 tháng và tổng hợp
        List<Budget> allBudgets = new ArrayList<>();
        final int[] loadedMonths = {0};
        final int totalMonths = 12;
        
        for (int month = 1; month <= 12; month++) {
            firebaseHelper.getUserBudgets(userId, month, year, 
                    new FirebaseHelper.OnBudgetsLoadedListener() {
                @Override
                public void onBudgetsLoaded(List<Budget> budgets) {
                    if (!isAdded() || getContext() == null) return;
                    
                    // Thêm budgets vào danh sách tổng hợp
                    allBudgets.addAll(budgets);
                    loadedMonths[0]++;
                    
                    // Khi đã load xong tất cả 12 tháng, tổng hợp budgets và tính toán
                    if (loadedMonths[0] == totalMonths) {
                        // Tổng hợp budgets: cộng dồn budgets của cùng category từ các tháng
                        List<Budget> aggregatedBudgets = aggregateBudgetsForYear(allBudgets);
                        calculateAndDisplayReport(transactions, aggregatedBudgets, null, year);
                    }
                }

                @Override
                public void onError(String error) {
                    if (!isAdded() || getContext() == null) return;
                    loadedMonths[0]++;
                    
                    // Nếu đã load xong tất cả (kể cả lỗi), vẫn hiển thị với dữ liệu có
                    if (loadedMonths[0] == totalMonths) {
                        List<Budget> aggregatedBudgets = aggregateBudgetsForYear(allBudgets);
                        calculateAndDisplayReport(transactions, aggregatedBudgets, null, year);
                    }
                }
            });
        }
    }
    
    /**
     * Tổng hợp budgets cho năm: cộng dồn budgets của cùng category từ tất cả các tháng
     */
    private List<Budget> aggregateBudgetsForYear(List<Budget> allBudgets) {
        Map<String, Double> categoryBudgetMap = new HashMap<>();
        
        // Tổng hợp budgets theo category
        for (Budget budget : allBudgets) {
            String categoryName = budget.getCategoryName();
            if (categoryName != null) {
                categoryBudgetMap.put(categoryName, 
                    categoryBudgetMap.getOrDefault(categoryName, 0.0) + budget.getAmount());
            }
        }
        
        // Tạo danh sách budgets đã tổng hợp
        List<Budget> aggregatedBudgets = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryBudgetMap.entrySet()) {
            // Tạo budget mới với amount đã tổng hợp (tháng và năm không quan trọng cho năm)
            Budget aggregatedBudget = new Budget();
            aggregatedBudget.setCategoryName(entry.getKey());
            aggregatedBudget.setAmount(entry.getValue());
            aggregatedBudgets.add(aggregatedBudget);
        }
        
        return aggregatedBudgets;
    }
    
    /**
     * Load dữ liệu báo cáo cho tháng/năm cụ thể
     */
    private void loadReportDataForMonth(int month, int year) {
        String userId = prefsHelper.getUserId();
        
        // Tạo Calendar cho tháng được chọn
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();
        
        // Ngày cuối cùng của tháng
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        // Load categories trước để map ID với tên và lưu expense categories
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                categoryIdToNameMap.clear();
                expenseCategories.clear();
                for (Category category : categories) {
                    if (category.getId() != null && category.getName() != null) {
                        // Map ID -> Name
                        categoryIdToNameMap.put(category.getId(), category.getName());
                        // Map Name -> Name (để normalize, tránh trường hợp có cả ID và Name)
                        categoryIdToNameMap.put(category.getName(), category.getName());
                        // Lưu expense categories để filter budgets
                        if ("expense".equals(category.getType())) {
                            expenseCategories.add(category);
                        }
                    }
                }
                // Sau khi load categories, load transactions
                loadTransactionsAndBudgets(userId, startDate, endDate, month, year);
                
                // Load dữ liệu tháng trước để so sánh (chỉ khi ở chế độ tháng)
                loadPreviousMonthData(userId, month, year);
            }

            @Override
            public void onError(String error) {
                // Vẫn tiếp tục load transactions dù không load được categories
                loadTransactionsAndBudgets(userId, startDate, endDate, month, year);
            }
        });
    }
    
    /**
     * Load transactions và budgets với real-time listener
     */
    private void loadTransactionsAndBudgets(String userId, Date startDate, Date endDate, 
                                             int month, int year) {
        // Hủy listener cũ nếu có
        if (transactionsListener != null) {
            transactionsListener.remove();
            transactionsListener = null;
        }
        
        // Reload categories mỗi khi transactions thay đổi để đảm bảo map luôn đầy đủ
        reloadCategoriesAndThenLoadTransactions(userId, startDate, endDate, month, year);
    }
    
    /**
     * Reload categories rồi mới load transactions
     */
    private void reloadCategoriesAndThenLoadTransactions(String userId, Date startDate, Date endDate, 
                                                         int month, int year) {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                // Cập nhật lại map categories và expense categories
                categoryIdToNameMap.clear();
                expenseCategories.clear();
                for (Category category : categories) {
                    if (category.getId() != null && category.getName() != null) {
                        // Map ID -> Name
                        categoryIdToNameMap.put(category.getId(), category.getName());
                        // Map Name -> Name (để normalize, tránh trường hợp có cả ID và Name)
                        categoryIdToNameMap.put(category.getName(), category.getName());
                        // Lưu expense categories để filter budgets
                        if ("expense".equals(category.getType())) {
                            expenseCategories.add(category);
                        }
                    }
                }
                
                // Sau khi reload categories, load transactions với real-time listener
                if (transactionsListener != null) {
                    transactionsListener.remove();
                    transactionsListener = null;
                }
                
                transactionsListener = firebaseHelper.listenMonthlyTransactions(userId, startDate, endDate, 
                        new FirebaseHelper.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        if (!isAdded() || getContext() == null) return;
                        
                        // Load budgets
                        firebaseHelper.getUserBudgets(userId, month, year, 
                                new FirebaseHelper.OnBudgetsLoadedListener() {
                            @Override
                            public void onBudgetsLoaded(List<Budget> budgets) {
                                if (!isAdded() || getContext() == null) return;
                                calculateAndDisplayReport(transactions, budgets, month, year);
                            }

                            @Override
                            public void onError(String error) {
                                if (!isAdded() || getContext() == null) return;
                                // Nếu không có budgets, vẫn hiển thị với budget = 0
                                calculateAndDisplayReport(transactions, new ArrayList<>(), month, year);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || getContext() == null) return;
                        // Hiển thị với dữ liệu rỗng
                        calculateAndDisplayReport(new ArrayList<>(), new ArrayList<>(), month, year);
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Nếu không load được categories, vẫn tiếp tục load transactions
                if (transactionsListener != null) {
                    transactionsListener.remove();
                    transactionsListener = null;
                }
                
                transactionsListener = firebaseHelper.listenMonthlyTransactions(userId, startDate, endDate, 
                        new FirebaseHelper.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        if (!isAdded() || getContext() == null) return;
                        
                        firebaseHelper.getUserBudgets(userId, month, year, 
                                new FirebaseHelper.OnBudgetsLoadedListener() {
                            @Override
                            public void onBudgetsLoaded(List<Budget> budgets) {
                                if (!isAdded() || getContext() == null) return;
                                calculateAndDisplayReport(transactions, budgets, month, year);
                            }

                            @Override
                            public void onError(String error) {
                                if (!isAdded() || getContext() == null) return;
                                calculateAndDisplayReport(transactions, new ArrayList<>(), month, year);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || getContext() == null) return;
                        calculateAndDisplayReport(new ArrayList<>(), new ArrayList<>(), month, year);
                    }
                });
            }
        });
    }

    /**
     * Tính toán và hiển thị báo cáo đầy đủ
     * @param transactions Danh sách transactions thực tế
     * @param budgets Danh sách budgets
     * @param month Tháng đang xem (1-12), null nếu là chế độ năm
     * @param year Năm đang xem
     */
    private void calculateAndDisplayReport(List<Transaction> transactions, List<Budget> budgets, Integer month, int year) {
        String userId = prefsHelper.getUserId();
        
        // Load recurring transactions để tính toán giao dịch định kỳ
        firebaseHelper.getUserRecurringTransactions(userId, 
                new FirebaseHelper.OnTransactionsLoadedListener() {
            @Override
            public void onTransactionsLoaded(List<Transaction> recurringTransactions) {
                if (!isAdded() || getContext() == null) return;
                
                double totalIncome = 0;
                double totalExpense = 0;
                Map<String, Double> expenseByCategory = new HashMap<>();
                Map<String, Integer> countByCategory = new HashMap<>();

                // Tính toán từ transactions thực tế (KHÔNG bao gồm recurring transaction gốc)
                for (Transaction t : transactions) {
                    // CHỈ tính các transactions thực tế, không tính recurring transaction gốc
                    if ("income".equals(t.getType()) && !t.isRecurring()) {
                        totalIncome += t.getAmount();
                    } else if ("expense".equals(t.getType()) && !t.isRecurring()) {
                        totalExpense += t.getAmount();
                        // Normalize category: nếu là ID thì map sang tên, nếu là tên thì giữ nguyên
                        String categoryKey = normalizeCategory(t.getCategory());
                        expenseByCategory.put(categoryKey, 
                                expenseByCategory.getOrDefault(categoryKey, 0.0) + t.getAmount());
                        countByCategory.put(categoryKey, 
                                countByCategory.getOrDefault(categoryKey, 0) + 1);
                    }
                }
                
                // Tính toán và thêm các giao dịch định kỳ
                // Tạo Set chứa các recurring transaction IDs đã có transactions thực tế
                // Chỉ tính recurring transaction nếu CHƯA có transaction thực tế nào được tạo từ nó
                Set<String> recurringIdsWithActualTransactions = new HashSet<>();
                for (Transaction t : transactions) {
                    // CHỈ kiểm tra các transactions thực tế (không phải recurring transaction gốc)
                    if (!t.isRecurring() && t.getRecurringTransactionId() != null && !t.getRecurringTransactionId().isEmpty()) {
                        recurringIdsWithActualTransactions.add(t.getRecurringTransactionId());
                    }
                }
                
                if (month != null) {
                    // Chế độ tháng: tính cho tháng cụ thể
                    Calendar selectedMonthCal = Calendar.getInstance();
                    selectedMonthCal.set(year, month - 1, 1);
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
                        
                        if (isMonthInRange(selectedMonthCal, startCal, endCal)) {
                            totalExpense += recurring.getAmount();
                            String categoryKey = normalizeCategory(recurring.getCategory());
                            expenseByCategory.put(categoryKey, 
                                    expenseByCategory.getOrDefault(categoryKey, 0.0) + recurring.getAmount());
                            countByCategory.put(categoryKey, 
                                    countByCategory.getOrDefault(categoryKey, 0) + 1);
                        }
                    }
                } else {
                    // Chế độ năm: tính cho tất cả các tháng trong năm
                    // Tạo Map để track các recurring transaction IDs đã được tính cho từng tháng
                    Map<Integer, Set<String>> recurringIdsByMonth = new HashMap<>();
                    for (int m = 1; m <= 12; m++) {
                        recurringIdsByMonth.put(m, new HashSet<>());
                        // Kiểm tra transactions thực tế trong tháng này
                        Calendar monthStart = Calendar.getInstance();
                        monthStart.set(year, m - 1, 1, 0, 0, 0);
                        Calendar monthEnd = Calendar.getInstance();
                        monthEnd.set(year, m - 1, monthStart.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                        
                        for (Transaction t : transactions) {
                            // CHỈ kiểm tra các transactions thực tế (không phải recurring transaction gốc)
                            if (!t.isRecurring() &&
                                t.getDate() != null && 
                                t.getDate().compareTo(monthStart.getTime()) >= 0 && 
                                t.getDate().compareTo(monthEnd.getTime()) <= 0 &&
                                t.getRecurringTransactionId() != null && 
                                !t.getRecurringTransactionId().isEmpty()) {
                                recurringIdsByMonth.get(m).add(t.getRecurringTransactionId());
                            }
                        }
                    }
                    
                    for (int m = 1; m <= 12; m++) {
                        Calendar monthCal = Calendar.getInstance();
                        monthCal.set(year, m - 1, 1);
                        monthCal.set(Calendar.HOUR_OF_DAY, 0);
                        monthCal.set(Calendar.MINUTE, 0);
                        monthCal.set(Calendar.SECOND, 0);
                        monthCal.set(Calendar.MILLISECOND, 0);
                        
                        for (Transaction recurring : recurringTransactions) {
                            // Chỉ tính các recurring expense transactions
                            if (!recurring.isRecurring() || 
                                !"expense".equals(recurring.getType()) ||
                                recurring.getRecurringStartMonth() == null || 
                                recurring.getRecurringEndMonth() == null) {
                                continue;
                            }
                            
                            // Bỏ qua nếu đã có transactions thực tế được tạo từ recurring transaction này trong tháng này
                            if (recurring.getId() != null && recurringIdsByMonth.get(m).contains(recurring.getId())) {
                                continue;
                            }
                            
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
                            
                            if (isMonthInRange(monthCal, startCal, endCal)) {
                                totalExpense += recurring.getAmount();
                                String categoryKey = normalizeCategory(recurring.getCategory());
                                expenseByCategory.put(categoryKey, 
                                        expenseByCategory.getOrDefault(categoryKey, 0.0) + recurring.getAmount());
                                countByCategory.put(categoryKey, 
                                        countByCategory.getOrDefault(categoryKey, 0) + 1);
                            }
                        }
                    }
                }
                
                // Tiếp tục với phần còn lại của method
                displayReportData(transactions, budgets, totalIncome, totalExpense, expenseByCategory, countByCategory);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Nếu không load được recurring transactions, vẫn tính với transactions thực tế
                // (KHÔNG bao gồm recurring transaction gốc)
                double totalIncome = 0;
                double totalExpense = 0;
                Map<String, Double> expenseByCategory = new HashMap<>();
                Map<String, Integer> countByCategory = new HashMap<>();

                for (Transaction t : transactions) {
                    // CHỈ tính các transactions thực tế, không tính recurring transaction gốc
                    if ("income".equals(t.getType()) && !t.isRecurring()) {
                        totalIncome += t.getAmount();
                    } else if ("expense".equals(t.getType()) && !t.isRecurring()) {
                        totalExpense += t.getAmount();
                        String categoryKey = normalizeCategory(t.getCategory());
                        expenseByCategory.put(categoryKey, 
                                expenseByCategory.getOrDefault(categoryKey, 0.0) + t.getAmount());
                        countByCategory.put(categoryKey, 
                                countByCategory.getOrDefault(categoryKey, 0) + 1);
                    }
                }
                
                displayReportData(transactions, budgets, totalIncome, totalExpense, expenseByCategory, countByCategory);
            }
        });
    }
    
    /**
     * Hiển thị dữ liệu báo cáo (tách ra để tái sử dụng)
     */
    private void displayReportData(List<Transaction> transactions, List<Budget> budgets,
                                   double totalIncome, double totalExpense,
                                   Map<String, Double> expenseByCategory, Map<String, Integer> countByCategory) {

        // Tính số dư
        double balance = totalIncome - totalExpense;

        // Tính tổng ngân sách (chỉ tính cho các expense categories hợp lệ, loại bỏ trùng lặp)
        // Tạo Set chứa tên các expense categories để kiểm tra (đã map sang tiếng Anh)
        Set<String> validCategoryNames = new HashSet<>();
        for (Category cat : expenseCategories) {
            if (cat.getName() != null) {
                // Map từ tiếng Việt sang tiếng Anh nếu cần
                String mappedName = mapCategoryNameToEnglish(cat.getName());
                validCategoryNames.add(mappedName);
                // Cũng thêm tên gốc để hỗ trợ cả trường hợp budgets có tên tiếng Anh
                validCategoryNames.add(cat.getName());
            }
        }
        
        // Loại bỏ trùng lặp và chỉ tính budgets cho các category hợp lệ
        // Map budgets theo category name (đã normalize)
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

        // Tính còn lại
        double remaining = totalBudget - totalExpense;

        // Hiển thị thông tin
        if (!isAdded() || getContext() == null) return;
        
        tvTotalExpense.setText(formatAmount(totalExpense));
        tvBudget.setText(formatAmount(totalBudget));
        tvRemaining.setText(formatAmount(remaining));
        tvTransactionCount.setText(String.valueOf(transactions.size()));
        
        // Lưu dữ liệu tháng hiện tại để cập nhật so sánh sau khi load xong tháng trước
        currentMonthTotalExpense = totalExpense;
        currentMonthTransactionCount = transactions.size();
        
        // Cập nhật so sánh với tháng trước (chỉ khi ở chế độ tháng)
        if (!isYearlyMode) {
            updateComparisonDisplay(totalExpense, transactions.size());
        } else {
            // Ẩn phần so sánh khi ở chế độ năm
            if (layoutExpenseComparison != null) {
                layoutExpenseComparison.setVisibility(View.GONE);
            }
            if (layoutTransactionComparison != null) {
                layoutTransactionComparison.setVisibility(View.GONE);
            }
            if (tvNoPreviousMonthData != null) {
                tvNoPreviousMonthData.setVisibility(View.GONE);
            }
        }

        // Đổi màu còn lại
        if (remaining < 0) {
            tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_color));
        } else {
            tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_color));
        }
        
        // Cập nhật danh sách giao dịch
        transactionList.clear();
        transactionList.addAll(transactions);
        // Sắp xếp theo ngày giảm dần (mới nhất trước)
        transactionList.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        // Reload categories trong adapter để đảm bảo map luôn đầy đủ
        transactionAdapter.reloadCategories();
        transactionAdapter.notifyDataSetChanged();

        // Tạo danh sách chi tiết theo danh mục
        categoryReportItems.clear();
        for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
            String categoryKey = entry.getKey(); // Đã được normalize, là tên category
            String categoryName = categoryKey; // categoryKey đã là tên rồi
            double amount = entry.getValue();
            int count = countByCategory.getOrDefault(categoryKey, 0);
            double percentage = totalExpense > 0 ? (amount / totalExpense) * 100 : 0;
            
            categoryReportItems.add(new CategoryReportItem(categoryName, amount, count, percentage));
        }
        // Sắp xếp theo số tiền giảm dần
        categoryReportItems.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));
        categoryAdapter.notifyDataSetChanged();
        
        // Hiển thị/ẩn thông báo khi không có dữ liệu
        if (categoryReportItems.isEmpty()) {
            recyclerViewCategories.setVisibility(View.GONE);
            tvNoCategoryData.setVisibility(View.VISIBLE);
        } else {
            recyclerViewCategories.setVisibility(View.VISIBLE);
            tvNoCategoryData.setVisibility(View.GONE);
        }

        // Tạo PieChart data với tên danh mục
        if (!expenseByCategory.isEmpty()) {
            // Hiển thị PieChart, ẩn TextView thông báo
            pieChart.setVisibility(View.VISIBLE);
            tvNoChartData.setVisibility(View.GONE);
            
            List<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
                String categoryKey = entry.getKey(); // Đã được normalize, là tên category
                String categoryName = categoryKey; // categoryKey đã là tên rồi
                entries.add(new PieEntry(entry.getValue().floatValue(), categoryName));
            }

            PieDataSet dataSet = new PieDataSet(entries, getString(R.string.category_label_pie_chart));
            dataSet.setColors(colors);
            dataSet.setValueTextSize(11f);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueFormatter(new PercentFormatter(pieChart));

            PieData data = new PieData(dataSet);
            pieChart.setData(data);
            pieChart.setCenterText(getString(R.string.expense));
            pieChart.invalidate();
        } else {
            // Ẩn PieChart, hiển thị TextView thông báo
            pieChart.setVisibility(View.GONE);
            tvNoChartData.setVisibility(View.VISIBLE);
            pieChart.setData(null);
        }
    }
    
    /**
     * Class để lưu thông tin báo cáo theo danh mục
     */
    public static class CategoryReportItem {
        private String categoryName;
        private double amount;
        private int count;
        private double percentage;

        public CategoryReportItem(String categoryName, double amount, int count, double percentage) {
            this.categoryName = categoryName;
            this.amount = amount;
            this.count = count;
            this.percentage = percentage;
        }

        public String getCategoryName() { return categoryName; }
        public double getAmount() { return amount; }
        public int getCount() { return count; }
        public double getPercentage() { return percentage; }
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
     * Normalize category: nếu là ID thì map sang tên, nếu là tên thì giữ nguyên
     * Đảm bảo normalize đúng để matching với budgets
     */
    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return getString(R.string.unknown);
        }
        
        // Nếu tìm thấy trong map, trả về giá trị đã được map
        String normalized = categoryIdToNameMap.get(category);
        if (normalized != null) {
            // Nếu normalized khác với category gốc, nghĩa là đã map được (ID -> Name)
            // Nếu normalized == category, nghĩa là map Name -> Name (đã là tên rồi)
            // Map từ tiếng Việt sang tiếng Anh nếu cần (để tương thích với budgets cũ)
            return mapCategoryNameToEnglish(normalized);
        }
        
        // Nếu không tìm thấy trong map, thử map từ tiếng Việt sang tiếng Anh
        String mappedName = mapCategoryNameToEnglish(category);
        // Kiểm tra lại xem tên đã map có trong map không
        normalized = categoryIdToNameMap.get(mappedName);
        if (normalized != null) {
            return mapCategoryNameToEnglish(normalized);
        }
        
        // Nếu không tìm thấy trong map:
        // - Nếu category là ID (chuỗi dài, không có khoảng trắng, giống Firestore ID)
        //   => có thể category đã bị xóa khỏi database
        // - Nếu category là tên (có khoảng trắng hoặc ngắn) => có thể là tên mới
        
        // Kiểm tra xem category có phải là ID không (Firestore ID thường dài > 15 ký tự, không có khoảng trắng)
        if (category.length() > 15 && !category.contains(" ") && !category.contains("-")) {
            // Có thể là ID của category đã bị xóa
            return getString(R.string.deleted_category);
        }
        
        // Nếu không phải ID, có thể là tên mới chưa có trong map, trả về tên đã map
        return mappedName;
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
     * Load dữ liệu tháng trước để so sánh
     */
    private void loadPreviousMonthData(String userId, int currentMonth, int currentYear) {
        // Tính tháng trước
        Calendar prevMonthCalendar = Calendar.getInstance();
        prevMonthCalendar.set(currentYear, currentMonth - 1, 1);
        prevMonthCalendar.add(Calendar.MONTH, -1);
        
        int prevMonth = prevMonthCalendar.get(Calendar.MONTH) + 1;
        int prevYear = prevMonthCalendar.get(Calendar.YEAR);
        
        // Kiểm tra xem tháng trước có hợp lệ không (trong khoảng 2 năm gần nhất)
        if (!isDateValid(prevMonthCalendar)) {
            hasPreviousMonthData = false;
            previousMonthTotalExpense = 0;
            previousMonthTransactionCount = 0;
            return;
        }
        
        // Tạo Calendar cho tháng trước
        Calendar calendar = Calendar.getInstance();
        calendar.set(prevYear, prevMonth - 1, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();
        
        // Ngày cuối cùng của tháng trước
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();
        
        // Load transactions của tháng trước
        firebaseHelper.getMonthlyTransactions(userId, startDate, endDate, 
                new FirebaseHelper.OnTransactionsLoadedListener() {
            @Override
            public void onTransactionsLoaded(List<Transaction> transactions) {
                if (!isAdded() || getContext() == null) return;
                
                // Tính tổng chi và số lượng giao dịch của tháng trước
                double totalExpense = 0;
                int transactionCount = transactions.size();
                
                for (Transaction t : transactions) {
                    if ("expense".equals(t.getType())) {
                        totalExpense += t.getAmount();
                    }
                }
                
                previousMonthTotalExpense = totalExpense;
                previousMonthTransactionCount = transactionCount;
                hasPreviousMonthData = true;
                
                // Cập nhật hiển thị so sánh với dữ liệu tháng hiện tại đã lưu
                if (!isAdded() || getContext() == null) return;
                updateComparisonDisplay(currentMonthTotalExpense, currentMonthTransactionCount);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                hasPreviousMonthData = false;
                previousMonthTotalExpense = 0;
                previousMonthTransactionCount = 0;
            }
        });
    }
    
    /**
     * Cập nhật hiển thị so sánh với tháng trước
     */
    private void updateComparisonDisplay(double currentTotalExpense, int currentTransactionCount) {
        if (!isAdded() || getContext() == null) return;
        
        if (!hasPreviousMonthData || previousMonthTotalExpense == 0) {
            // Không có dữ liệu tháng trước
            if (layoutExpenseComparison != null) {
                layoutExpenseComparison.setVisibility(View.GONE);
            }
            if (layoutTransactionComparison != null) {
                layoutTransactionComparison.setVisibility(View.GONE);
            }
            if (tvNoPreviousMonthData != null) {
                tvNoPreviousMonthData.setVisibility(View.VISIBLE);
            }
            return;
        }
        
        // Ẩn thông báo không có dữ liệu
        if (tvNoPreviousMonthData != null) {
            tvNoPreviousMonthData.setVisibility(View.GONE);
        }
        
        // Tính toán so sánh tổng chi
        double expenseDiff = currentTotalExpense - previousMonthTotalExpense;
        double expensePercentChange = previousMonthTotalExpense > 0 
                ? (expenseDiff / previousMonthTotalExpense) * 100 
                : (currentTotalExpense > 0 ? 100 : 0);
        
        // Hiển thị so sánh tổng chi
        if (layoutExpenseComparison != null) {
            layoutExpenseComparison.setVisibility(View.VISIBLE);
            
            String expenseChangeText;
            int expenseColor;
            int expenseIcon;
            
            if (expenseDiff > 0) {
                // Tăng chi tiêu
                expenseChangeText = String.format(Locale.getDefault(), 
                        "+%,.0f VND (+%.1f%%)", expenseDiff, expensePercentChange);
                expenseColor = ContextCompat.getColor(requireContext(), R.color.expense_color);
                expenseIcon = R.drawable.ic_arrow_up;
            } else if (expenseDiff < 0) {
                // Giảm chi tiêu
                expenseChangeText = String.format(Locale.getDefault(), 
                        "%,.0f VND (%.1f%%)", expenseDiff, expensePercentChange);
                expenseColor = ContextCompat.getColor(requireContext(), R.color.income_color);
                expenseIcon = R.drawable.ic_arrow_down;
            } else {
                // Không thay đổi
                expenseChangeText = "0 VND (0%)";
                expenseColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
                expenseIcon = R.drawable.ic_arrow_up; // Có thể dùng icon khác nếu muốn
            }
            
            if (tvExpenseChange != null) {
                tvExpenseChange.setText(expenseChangeText);
                tvExpenseChange.setTextColor(expenseColor);
            }
            
            if (ivExpenseChange != null) {
                ivExpenseChange.setImageResource(expenseIcon);
                ivExpenseChange.setColorFilter(expenseColor);
            }
        }
        
        // Tính toán so sánh số lượng giao dịch
        int transactionDiff = currentTransactionCount - previousMonthTransactionCount;
        double transactionPercentChange = previousMonthTransactionCount > 0 
                ? ((double) transactionDiff / previousMonthTransactionCount) * 100 
                : (currentTransactionCount > 0 ? 100 : 0);
        
        // Hiển thị so sánh số lượng giao dịch
        if (layoutTransactionComparison != null) {
            layoutTransactionComparison.setVisibility(View.VISIBLE);
            
            String transactionChangeText;
            int transactionColor;
            int transactionIcon;
            
            if (transactionDiff > 0) {
                // Tăng số lượng giao dịch
                transactionChangeText = String.format(Locale.getDefault(), 
                        "+%d (+%.1f%%)", transactionDiff, transactionPercentChange);
                transactionColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
                transactionIcon = R.drawable.ic_arrow_up;
            } else if (transactionDiff < 0) {
                // Giảm số lượng giao dịch
                transactionChangeText = String.format(Locale.getDefault(), 
                        "%d (%.1f%%)", transactionDiff, transactionPercentChange);
                transactionColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
                transactionIcon = R.drawable.ic_arrow_down;
            } else {
                // Không thay đổi
                transactionChangeText = "0 (0%)";
                transactionColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
                transactionIcon = R.drawable.ic_arrow_up;
            }
            
            if (tvTransactionChange != null) {
                tvTransactionChange.setText(transactionChangeText);
                tvTransactionChange.setTextColor(transactionColor);
            }
            
            if (ivTransactionChange != null) {
                ivTransactionChange.setImageResource(transactionIcon);
                ivTransactionChange.setColorFilter(transactionColor);
            }
        }
    }

    private String formatAmount(double amount) {
        return String.format(Locale.getDefault(), "%,.0f VND", amount);
    }
    
    /**
     * Mở dialog để sửa transaction
     */
    private void editTransaction(Transaction transaction) {
        AddTransactionFragment editDialog = AddTransactionFragment.newInstance(transaction);
        editDialog.setOnTransactionAddedListener(new AddTransactionFragment.OnTransactionAddedListener() {
            @Override
            public void onTransactionAdded() {
                // Reload dữ liệu sau khi sửa
                if (isYearlyMode) {
                    loadReportDataForYear(selectedCalendar.get(Calendar.YEAR));
                } else {
                    loadReportDataForMonth(selectedCalendar.get(Calendar.MONTH) + 1, 
                                         selectedCalendar.get(Calendar.YEAR));
                }
            }
            
            @Override
            public void onTransactionAdded(Transaction transaction) {
                onTransactionAdded();
            }
        });
        editDialog.show(getParentFragmentManager(), "EditTransaction");
    }
    
    /**
     * Xóa transaction với xác nhận
     */
    private void deleteTransaction(Transaction transaction) {
        if (!isAdded() || getContext() == null) return;
        
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.delete_transaction))
                .setMessage(getString(R.string.delete_transaction_confirm))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    if (transaction.getId() == null || transaction.getId().isEmpty()) {
                        Toast.makeText(getContext(), getString(R.string.cannot_delete_transaction_invalid_id), 
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    firebaseHelper.deleteTransaction(transaction.getId(), task -> {
                        if (!isAdded() || getContext() == null) return;
                        
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), getString(R.string.delete_transaction_success), 
                                    Toast.LENGTH_SHORT).show();
                            // Reload dữ liệu sau khi xóa
                            if (isYearlyMode) {
                                loadReportDataForYear(selectedCalendar.get(Calendar.YEAR));
                            } else {
                                loadReportDataForMonth(selectedCalendar.get(Calendar.MONTH) + 1, 
                                                     selectedCalendar.get(Calendar.YEAR));
                            }
                        } else {
                            String error = task.getException() != null ? 
                                    task.getException().getMessage() : getString(R.string.unknown);
                            Toast.makeText(getContext(), getString(R.string.delete_transaction_failed, error), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(getString(R.string.cancel_button), null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload báo cáo cho tháng/năm đang chọn khi quay lại fragment
        if (firebaseHelper != null && selectedCalendar != null) {
            if (isYearlyMode) {
                loadReportDataForYear(selectedCalendar.get(Calendar.YEAR));
            } else {
                loadReportDataForMonth(selectedCalendar.get(Calendar.MONTH) + 1, 
                                     selectedCalendar.get(Calendar.YEAR));
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Hủy listener khi fragment không còn hiển thị để tiết kiệm tài nguyên
        if (transactionsListener != null) {
            transactionsListener.remove();
            transactionsListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Đảm bảo hủy listener khi fragment bị destroy
        if (transactionsListener != null) {
            transactionsListener.remove();
            transactionsListener = null;
        }
    }
}


