package com.example.appqlct.fragment.user;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.appqlct.R;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.helper.NotificationHelper;
import com.example.appqlct.helper.SharedPreferencesHelper;
import com.example.appqlct.model.Budget;
import com.example.appqlct.model.Category;
import com.example.appqlct.model.Transaction;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AddTransactionFragment - Dialog fragment để thêm/sửa giao dịch
 * Cho phép chọn: ngày, số tiền, danh mục, ghi chú (chỉ chi tiêu)
 */
public class AddTransactionFragment extends DialogFragment {
    private static final String ARG_TRANSACTION = "transaction";
    
    private EditText etAmount, etNote;
    private TextView tvDate;
    private Spinner spinnerCategory;
    private CheckBox checkboxRecurring;
    private View layoutRecurringPeriod;
    private TextView tvStartMonth, tvEndMonth;
    private FirebaseHelper firebaseHelper;
    private SharedPreferencesHelper prefsHelper;
    private List<Category> expenseCategories; // Chỉ danh mục chi tiêu
    private OnTransactionAddedListener listener;
    private boolean categoriesLoaded = false; // Đánh dấu đã load xong danh mục chưa
    private Transaction editTransaction; // Transaction đang được chỉnh sửa (nếu có)
    private Transaction savedTransaction; // Transaction vừa được lưu (để gọi listener trong onDismiss)
    private Calendar selectedDate;
    private Calendar startMonth, endMonth; // Khoảng thời gian định kỳ
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
    private boolean isProcessing = false; // Flag để tránh xử lý trùng lặp
    private AlertDialog dialog; // Reference đến dialog để disable button

    public interface OnTransactionAddedListener {
        void onTransactionAdded();
        void onTransactionAdded(Transaction transaction); // Overload để truyền transaction object
    }
    
    /**
     * Adapter class để wrap lambda expression thành listener object
     * Giúp các fragment cũ vẫn có thể sử dụng lambda
     */
    public static class SimpleTransactionAddedListener implements OnTransactionAddedListener {
        private final Runnable callback;
        
        public SimpleTransactionAddedListener(Runnable callback) {
            this.callback = callback;
        }
        
        @Override
        public void onTransactionAdded() {
            if (callback != null) {
                callback.run();
            }
        }
        
        @Override
        public void onTransactionAdded(Transaction transaction) {
            // Gọi method cũ nếu không có implementation riêng
            onTransactionAdded();
        }
    }

    public void setOnTransactionAddedListener(OnTransactionAddedListener listener) {
        this.listener = listener;
    }
    
    /**
     * Helper method để set listener từ lambda (tương thích với code cũ)
     */
    public void setOnTransactionAddedListener(Runnable callback) {
        this.listener = new SimpleTransactionAddedListener(callback);
    }
    
    /**
     * Tạo instance mới để thêm transaction
     */
    public static AddTransactionFragment newInstance() {
        return new AddTransactionFragment();
    }
    
    /**
     * Tạo instance mới để sửa transaction
     */
    public static AddTransactionFragment newInstance(Transaction transaction) {
        AddTransactionFragment fragment = new AddTransactionFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRANSACTION, transaction);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lấy transaction từ arguments nếu có (chế độ edit)
        if (getArguments() != null) {
            editTransaction = (Transaction) getArguments().getSerializable(ARG_TRANSACTION);
            // Nếu date là null (do Serializable với Date có thể gặp vấn đề), khôi phục lại
            if (editTransaction != null && editTransaction.getDate() == null) {
                // Nếu date null, set về ngày hiện tại
                editTransaction.setDate(new Date());
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_transaction, null);

        initViews(view);
        initHelpers();
        loadCategories();

        String title = editTransaction != null ? getString(R.string.edit) : getString(R.string.add_transaction);
        builder.setView(view)
                .setTitle(title)
                .setPositiveButton(getString(R.string.save), (dialog, which) -> saveTransaction())
                .setNegativeButton(getString(R.string.cancel), null);

        dialog = builder.create();
        
        // Set listener để lấy button và disable khi đang xử lý
        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button positiveButton = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setOnClickListener(v -> {
                    if (!isProcessing) {
                        saveTransaction();
                    }
                });
            }
        });
        
        return dialog;
    }

    private void initViews(View view) {
        etAmount = view.findViewById(R.id.etAmount);
        etNote = view.findViewById(R.id.etNote);
        tvDate = view.findViewById(R.id.tvDate);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        checkboxRecurring = view.findViewById(R.id.checkboxRecurring);
        layoutRecurringPeriod = view.findViewById(R.id.layoutRecurringPeriod);
        tvStartMonth = view.findViewById(R.id.tvStartMonth);
        tvEndMonth = view.findViewById(R.id.tvEndMonth);
        
        // Khởi tạo selectedDate - luôn là ngày hiện tại
        selectedDate = Calendar.getInstance();
        if (editTransaction != null && editTransaction.getDate() != null) {
            selectedDate.setTime(editTransaction.getDate());
        }
        
        // Hiển thị ngày
        tvDate.setText(dateFormat.format(selectedDate.getTime()));
        
        // Click vào tvDate để chọn ngày
        tvDate.setOnClickListener(v -> showDatePicker());
        
        // Click vào tvStartMonth để chọn tháng bắt đầu
        if (tvStartMonth != null) {
            tvStartMonth.setOnClickListener(v -> showMonthPicker(true));
        }
        
        // Click vào tvEndMonth để chọn tháng kết thúc
        if (tvEndMonth != null) {
            tvEndMonth.setOnClickListener(v -> showMonthPicker(false));
        }
        
        // Listener cho checkboxRecurring - hiển thị/ẩn UI chọn khoảng thời gian
        checkboxRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Không tự động đổi ngày, chỉ hiển thị/ẩn UI chọn khoảng thời gian
            updateRecurringPeriodVisibility(isChecked);
        });
        
        // Ẩn layout khoảng thời gian ban đầu
        if (layoutRecurringPeriod != null) {
            layoutRecurringPeriod.setVisibility(View.GONE);
        }
        
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
                        // Sử dụng Locale Việt Nam để đảm bảo format đúng
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
        
        // Nếu đang edit, điền dữ liệu vào form
        if (editTransaction != null) {
            // Format số tiền với dấu chấm (.) khi hiển thị (chuẩn Việt Nam)
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("#,###", symbols);
            formatter.setGroupingSize(3);
            etAmount.setText(formatter.format((long)editTransaction.getAmount()));
            etNote.setText(editTransaction.getNote() != null ? editTransaction.getNote() : "");
            checkboxRecurring.setChecked(editTransaction.isRecurring());
            
            // Nếu đang edit và là recurring, khôi phục khoảng thời gian
            if (editTransaction.isRecurring()) {
                if (editTransaction.getRecurringStartMonth() != null) {
                    startMonth = Calendar.getInstance();
                    startMonth.setTime(editTransaction.getRecurringStartMonth());
                }
                if (editTransaction.getRecurringEndMonth() != null) {
                    endMonth = Calendar.getInstance();
                    endMonth.setTime(editTransaction.getRecurringEndMonth());
                }
                updateRecurringPeriodVisibility(true);
            }
        }
    }
    
    /**
     * Hiển thị/ẩn UI chọn khoảng thời gian định kỳ
     */
    private void updateRecurringPeriodVisibility(boolean isVisible) {
        if (layoutRecurringPeriod != null) {
            layoutRecurringPeriod.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
        // Khởi tạo giá trị mặc định nếu đang chọn định kỳ
        if (isVisible) {
            if (startMonth == null) {
                // Mặc định: từ tháng hiện tại
                startMonth = Calendar.getInstance();
                startMonth.set(Calendar.DAY_OF_MONTH, 1);
            }
            if (endMonth == null) {
                // Mặc định: đến 12 tháng sau
                endMonth = Calendar.getInstance();
                endMonth.add(Calendar.MONTH, 12);
                endMonth.set(Calendar.DAY_OF_MONTH, 1);
            }
            updateMonthDisplay();
        }
    }
    
    /**
     * Cập nhật hiển thị tháng bắt đầu và kết thúc
     */
    private void updateMonthDisplay() {
        if (tvStartMonth != null && startMonth != null) {
            tvStartMonth.setText(monthFormat.format(startMonth.getTime()));
        }
        if (tvEndMonth != null && endMonth != null) {
            tvEndMonth.setText(monthFormat.format(endMonth.getTime()));
        }
    }
    
    /**
     * Hiển thị MonthPicker để chọn tháng
     */
    private void showMonthPicker(boolean isStartMonth) {
        Calendar calendar = isStartMonth ? 
            (startMonth != null ? startMonth : Calendar.getInstance()) :
            (endMonth != null ? endMonth : Calendar.getInstance());
        
        // Tạo DatePickerDialog nhưng chỉ cho phép chọn tháng/năm
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, 1);
                selected.set(Calendar.HOUR_OF_DAY, 0);
                selected.set(Calendar.MINUTE, 0);
                selected.set(Calendar.SECOND, 0);
                selected.set(Calendar.MILLISECOND, 0);
                
                if (isStartMonth) {
                    startMonth = selected;
                    // Đảm bảo tháng kết thúc không sớm hơn tháng bắt đầu
                    if (endMonth != null && endMonth.before(startMonth)) {
                        endMonth = (Calendar) startMonth.clone();
                        endMonth.add(Calendar.MONTH, 1);
                    }
                } else {
                    endMonth = selected;
                    // Đảm bảo tháng kết thúc không sớm hơn tháng bắt đầu
                    if (startMonth != null && endMonth.before(startMonth)) {
                        String userId = prefsHelper.getUserId();
                        NotificationHelper.addInfoNotification(requireContext(), userId, 
                            getString(R.string.end_month_must_be_after_start_month));
                        endMonth = (Calendar) startMonth.clone();
                        endMonth.add(Calendar.MONTH, 1);
                    }
                }
                updateMonthDisplay();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            1 // Luôn chọn ngày 1
        );
        datePickerDialog.show();
    }
    
    /**
     * Hiển thị DatePicker để chọn ngày
     */
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                tvDate.setText(dateFormat.format(selectedDate.getTime()));
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        prefsHelper = new SharedPreferencesHelper(requireContext());
        expenseCategories = new ArrayList<>();
    }

    /**
     * Load danh sách categories từ Firestore (chỉ expense categories)
     */
    private void loadCategories() {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categoryList) {
                if (!isAdded() || getContext() == null) return;
                
                expenseCategories.clear();
                if (categoryList != null) {
                    // Chỉ lấy expense categories
                    for (Category cat : categoryList) {
                        if (cat != null && "expense".equals(cat.getType())) {
                            expenseCategories.add(cat);
                        }
                    }
                }
                
                categoriesLoaded = true; // Đánh dấu đã load xong
                
                // Cập nhật spinner danh mục
                updateCategorySpinner();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                String userId = prefsHelper.getUserId();
                NotificationHelper.addErrorNotification(requireContext(), userId, 
                        getString(R.string.error_loading_categories, error != null ? error : getString(R.string.unknown)));
            }
        });
    }
    
    /**
     * Cập nhật spinner danh mục với danh sách expense categories
     */
    private void updateCategorySpinner() {
        if (!isAdded() || getContext() == null) return;
        
        List<String> categoryNames = new ArrayList<>();
        for (Category cat : expenseCategories) {
            if (cat != null && cat.getName() != null) {
                categoryNames.add(cat.getName());
            }
        }
        
        // Nếu không có danh mục nào, thêm một item thông báo
        if (categoryNames.isEmpty()) {
            categoryNames.add(getString(R.string.no_expense_categories));
        }
        
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        
        // Nếu đang edit, chọn category hiện tại
        if (editTransaction != null && editTransaction.getCategory() != null) {
            String currentCategory = editTransaction.getCategory();
            for (int i = 0; i < expenseCategories.size(); i++) {
                if (expenseCategories.get(i).getName().equals(currentCategory)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }
        
        // Nếu không có danh mục thực sự, hiển thị thông báo
        if (expenseCategories.isEmpty()) {
            String userId = prefsHelper.getUserId();
            NotificationHelper.addInfoNotification(getContext(), userId, 
                    getString(R.string.no_expense_categories_message));
        }
    }

    /**
     * Lưu transaction mới vào Firestore
     */
    private void saveTransaction() {
        // Kiểm tra xem đang xử lý không, tránh xử lý trùng lặp
        if (isProcessing) {
            return;
        }
        
        String amountStr = etAmount.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        int categoryIndex = spinnerCategory.getSelectedItemPosition();

        // Validation
        if (amountStr.isEmpty()) {
            etAmount.setError(getString(R.string.required_field));
            return;
        }

        // Kiểm tra xem có danh mục thực sự không (không phải item thông báo)
        if (expenseCategories.isEmpty()) {
            String userId = prefsHelper.getUserId();
            NotificationHelper.addInfoNotification(requireContext(), userId, 
                    getString(R.string.please_initialize_categories));
            return;
        }
        
        if (categoryIndex < 0 || categoryIndex >= expenseCategories.size()) {
            String userId = prefsHelper.getUserId();
            NotificationHelper.addInfoNotification(requireContext(), userId, 
                    getString(R.string.please_select_category));
            return;
        }

        try {
            // Loại bỏ tất cả dấu chấm và phẩy trước khi parse (chỉ giữ số)
            String cleanAmountStr = amountStr.replaceAll("[.,]", "");
            double amount = Double.parseDouble(cleanAmountStr);
            if (amount <= 0) {
                etAmount.setError(getString(R.string.amount_must_be_greater_than_zero));
                return;
            }
            
            // Set flag đang xử lý và disable button (sau khi validation pass)
            isProcessing = true;
            if (dialog != null) {
                android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    positiveButton.setEnabled(false);
                }
            }

            Category selectedCategory = expenseCategories.get(categoryIndex);
            String type = "expense"; // Chỉ cho phép thêm expense
            String userId = prefsHelper.getUserId();
            Date currentDate = Calendar.getInstance().getTime();

            boolean isRecurring = checkboxRecurring.isChecked();
            
            // Validation cho khoảng thời gian nếu là định kỳ
            if (isRecurring) {
                if (startMonth == null || endMonth == null) {
                    isProcessing = false;
                    if (dialog != null) {
                        android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        if (positiveButton != null) {
                            positiveButton.setEnabled(true);
                        }
                    }
                    NotificationHelper.addInfoNotification(requireContext(), userId, 
                            getString(R.string.please_select_recurring_period));
                    return;
                }
                if (endMonth.before(startMonth)) {
                    isProcessing = false;
                    if (dialog != null) {
                        android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        if (positiveButton != null) {
                            positiveButton.setEnabled(true);
                        }
                    }
                    NotificationHelper.addInfoNotification(requireContext(), userId, 
                            getString(R.string.end_month_must_be_after_start_month));
                    return;
                }
            }
            
            if (editTransaction != null) {
                // Chế độ sửa
                editTransaction.setAmount(amount);
                editTransaction.setCategory(selectedCategory.getName());
                editTransaction.setNote(note);
                editTransaction.setDate(selectedDate.getTime());
                editTransaction.setRecurring(isRecurring);
                // Lưu khoảng thời gian nếu là định kỳ
                if (isRecurring && startMonth != null && endMonth != null) {
                    editTransaction.setRecurringStartMonth(startMonth.getTime());
                    editTransaction.setRecurringEndMonth(endMonth.getTime());
                } else {
                    editTransaction.setRecurringStartMonth(null);
                    editTransaction.setRecurringEndMonth(null);
                }
                
                // Nếu đang sửa giao dịch định kỳ, cần xóa các giao dịch tự động cũ và tạo lại
                boolean wasRecurring = editTransaction.isRecurring();
                String oldRecurringTransactionId = editTransaction.getId();
                
                firebaseHelper.updateTransaction(editTransaction, task -> {
                    if (!isAdded() || getContext() == null) {
                        isProcessing = false;
                        return;
                    }
                    if (task.isSuccessful()) {
                        // Nếu đang sửa giao dịch định kỳ, xóa các giao dịch tự động cũ và tạo lại
                        if (wasRecurring && oldRecurringTransactionId != null) {
                            // Xóa các giao dịch tự động cũ
                            firebaseHelper.deleteTransactionsFromRecurring(oldRecurringTransactionId, deleteTask -> {
                                if (!isAdded() || getContext() == null) {
                                    isProcessing = false;
                                    return;
                                }
                                
                                // Tạo lại các giao dịch tự động nếu vẫn là định kỳ
                                if (isRecurring && startMonth != null && endMonth != null) {
                                    firebaseHelper.generateMonthlyTransactionsFromRecurring(editTransaction, generateTask -> {
                                        if (!isAdded() || getContext() == null) {
                                            isProcessing = false;
                                            return;
                                        }
                                        NotificationHelper.addSuccessNotification(getContext(), userId, 
                                                getString(R.string.edit_transaction_success));
                                        
                                        // Lưu transaction để gọi listener trong onDismiss (sau khi dialog dismiss)
                                        savedTransaction = editTransaction;
                                        
                                        isProcessing = false;
                                        dismiss();
                                    });
                                } else {
                                    // Không còn là định kỳ nữa, chỉ cần xóa các giao dịch tự động cũ
                                    NotificationHelper.addSuccessNotification(getContext(), userId, 
                                            getString(R.string.edit_transaction_success));
                                    
                                    // Lưu transaction để gọi listener trong onDismiss (sau khi dialog dismiss)
                                    savedTransaction = editTransaction;
                                    
                                    isProcessing = false;
                                    dismiss();
                                }
                            });
                        } else if (isRecurring && startMonth != null && endMonth != null) {
                            // Nếu trước đó không phải định kỳ nhưng bây giờ là định kỳ, tạo các giao dịch tự động
                            firebaseHelper.generateMonthlyTransactionsFromRecurring(editTransaction, generateTask -> {
                                if (!isAdded() || getContext() == null) {
                                    isProcessing = false;
                                    return;
                                }
                                NotificationHelper.addSuccessNotification(getContext(), userId, 
                                        getString(R.string.edit_transaction_success));
                                
                                // Lưu transaction để gọi listener trong onDismiss (sau khi dialog dismiss)
                                savedTransaction = editTransaction;
                                
                                isProcessing = false;
                                dismiss();
                            });
                        } else {
                            NotificationHelper.addSuccessNotification(getContext(), userId, 
                                    getString(R.string.edit_transaction_success));
                            
                            // Lưu transaction để gọi listener trong onDismiss (sau khi dialog dismiss)
                            savedTransaction = editTransaction;
                            
                            isProcessing = false;
                            dismiss();
                        }
                    } else {
                        isProcessing = false;
                        if (dialog != null) {
                            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            if (positiveButton != null) {
                                positiveButton.setEnabled(true);
                            }
                        }
                        NotificationHelper.addErrorNotification(getContext(), userId, 
                                getString(R.string.error));
                    }
                });
            } else {
                // Chế độ thêm mới
                Transaction transaction = new Transaction(
                        null, // id sẽ được tạo bởi Firestore
                        userId,
                        amount,
                        selectedCategory.getName(), // Lưu tên category thay vì ID để hiển thị
                        note,
                        selectedDate.getTime(),
                        type,
                        isRecurring
                );
                
                // Lưu khoảng thời gian nếu là định kỳ
                if (isRecurring && startMonth != null && endMonth != null) {
                    transaction.setRecurringStartMonth(startMonth.getTime());
                    transaction.setRecurringEndMonth(endMonth.getTime());
                }

                firebaseHelper.addTransaction(transaction, task -> {
                    if (!isAdded() || getContext() == null) {
                        isProcessing = false;
                        return;
                    }
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Lấy ID từ Firestore và set vào transaction
                        String transactionId = task.getResult().getId();
                        transaction.setId(transactionId);
                        
                        // Nếu là chi tiêu định kỳ, tạo các giao dịch chi tiêu tự động cho từng tháng
                        if (isRecurring && startMonth != null && endMonth != null) {
                            firebaseHelper.generateMonthlyTransactionsFromRecurring(transaction, generateTask -> {
                                if (!isAdded() || getContext() == null) {
                                    isProcessing = false;
                                    return;
                                }
                                if (generateTask.isSuccessful()) {
                                    NotificationHelper.addSuccessNotification(getContext(), userId, 
                                            getString(R.string.add_transaction_success));
                                } else {
                                    // Vẫn hiển thị thành công nếu lưu được giao dịch định kỳ, 
                                    // nhưng có thể có lỗi khi tạo các giao dịch tự động
                                    NotificationHelper.addSuccessNotification(getContext(), userId, 
                                            getString(R.string.add_transaction_success));
                                    android.util.Log.e("AddTransaction", "Error generating monthly transactions", generateTask.getException());
                                }
                                
                                // Lưu transaction để gọi listener trong onDismiss (sau khi dialog dismiss)
                                savedTransaction = transaction;
                                
                                isProcessing = false;
                                dismiss();
                                
                                // Kiểm tra ngân sách sau một chút (không block UI)
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    if (isAdded() && getContext() != null) {
                                        checkBudgetThreshold(transaction);
                                    }
                                }, 500);
                            });
                        } else {
                            NotificationHelper.addSuccessNotification(getContext(), userId, 
                                    getString(R.string.add_transaction_success));
                            
                            // Lưu transaction để gọi listener trong onDismiss (sau khi dialog dismiss)
                            savedTransaction = transaction;
                            
                            isProcessing = false;
                            dismiss();
                            
                            // Kiểm tra ngân sách sau một chút (không block UI)
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (isAdded() && getContext() != null) {
                                    checkBudgetThreshold(transaction);
                                }
                            }, 500);
                        }
                    } else {
                        isProcessing = false;
                        if (dialog != null) {
                            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            if (positiveButton != null) {
                                positiveButton.setEnabled(true);
                            }
                        }
                        NotificationHelper.addErrorNotification(getContext(), userId, 
                                getString(R.string.error));
                    }
                });
            }

        } catch (NumberFormatException e) {
            isProcessing = false;
            if (dialog != null) {
                android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    positiveButton.setEnabled(true);
                }
            }
            etAmount.setError(getString(R.string.invalid_amount));
        }
    }

    /**
     * Kiểm tra xem chi tiêu có chạm mốc 80% ngân sách không
     * Hiển thị thông báo nếu đạt ngưỡng
     */
    private void checkBudgetThreshold(Transaction transaction) {
        if (!isAdded() || getContext() == null) return;
        
        // Chỉ kiểm tra cho expense transactions
        if (!"expense".equals(transaction.getType())) {
            return;
        }

        // Lấy tháng/năm của transaction
        Calendar cal = Calendar.getInstance();
        cal.setTime(transaction.getDate());
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        
        String userId = prefsHelper.getUserId();
        
        // Lấy ngân sách cho tháng đó
        firebaseHelper.getUserBudgets(userId, month, year, new FirebaseHelper.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                if (!isAdded() || getContext() == null) return;
                
                if (budgets == null || budgets.isEmpty()) {
                    // Không có ngân sách, không cần kiểm tra
                    return;
                }
                
                // Tính tổng ngân sách
                double totalBudget = 0;
                for (Budget budget : budgets) {
                    totalBudget += budget.getAmount();
                }
                
                if (totalBudget <= 0) {
                    // Không có ngân sách hợp lệ
                    return;
                }
                
                // Copy giá trị vào biến final để sử dụng trong inner class
                final double finalTotalBudget = totalBudget;
                
                // Tính tổng chi tiêu trong tháng
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month - 1, 1, 0, 0, 0);
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
                        
                        // Tính tổng chi tiêu
                        double totalSpent = 0;
                        for (Transaction t : transactions) {
                            if ("expense".equals(t.getType())) {
                                totalSpent += t.getAmount();
                            }
                        }
                        
                        // Kiểm tra xem có >= 80% ngân sách không
                        double percentage = (totalSpent / finalTotalBudget) * 100;
                        
                        // Không hiển thị dialog, chỉ tạo notification để cập nhật badge số
                        // Notification sẽ được tạo tự động bởi DashboardFragment thông qua MainActivity
                        if (percentage >= 80) {
                            // Không hiển thị dialog nữa, chỉ để notification badge cập nhật
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Không hiển thị lỗi, chỉ bỏ qua kiểm tra
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Không hiển thị lỗi, chỉ bỏ qua kiểm tra
            }
        });
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
    
    /**
     * Gọi khi dialog dismiss - đảm bảo fragment đã resume trước khi refresh
     */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        
        // Gọi listener sau khi dialog dismiss hoàn toàn
        // Sử dụng post và delay đủ lớn để đảm bảo fragment đã resume
        if (savedTransaction != null && listener != null) {
            // Lưu transaction và listener vào biến local để tránh bị clear
            Transaction transactionToNotify = savedTransaction;
            OnTransactionAddedListener listenerToCall = listener;
            
            // Clear ngay để tránh gọi lại
            savedTransaction = null;
            
            // Đợi một chút để đảm bảo dialog đã dismiss hoàn toàn và fragment đã resume
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Ưu tiên gọi listener với transaction object (có thông tin đầy đủ hơn)
                if (listenerToCall != null && transactionToNotify != null) {
                    listenerToCall.onTransactionAdded(transactionToNotify);
                } else if (listenerToCall != null) {
                    // Fallback: gọi method không có parameter nếu không có transaction
                    listenerToCall.onTransactionAdded();
                }
            }, 400); // Tăng delay lên 400ms để đảm bảo fragment đã resume hoàn toàn
        } else if (listener != null) {
            // Nếu không có savedTransaction nhưng có listener, vẫn gọi callback
            OnTransactionAddedListener listenerToCall = listener;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (listenerToCall != null) {
                    listenerToCall.onTransactionAdded();
                }
            }, 400);
        }
    }
}

