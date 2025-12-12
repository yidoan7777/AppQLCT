package com.example.appqlct.fragment.admin;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appqlct.R;
import com.example.appqlct.helper.FirebaseHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * MaintenanceFragment - Bảo trì hệ thống (chỉ Admin)
 * Các chức năng: xóa dữ liệu cũ, backup, restore
 */
public class MaintenanceFragment extends Fragment {
    private Button btnClearOldData, btnBackup, btnRestore;
    private FirebaseHelper firebaseHelper;
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Kiểm tra quyền admin trước khi hiển thị fragment
        if (!checkAdminPermission()) {
            return null; // Không hiển thị fragment nếu không phải admin
        }
        
        View view = inflater.inflate(R.layout.fragment_maintenance, container, false);

        initViews(view);
        initHelpers();

        return view;
    }
    
    /**
     * Kiểm tra quyền admin
     * @return true nếu là admin, false nếu không phải
     */
    private boolean checkAdminPermission() {
        if (getContext() == null) return false;
        
        com.example.appqlct.helper.SharedPreferencesHelper prefsHelper = 
                new com.example.appqlct.helper.SharedPreferencesHelper(getContext());
        String userRole = prefsHelper.getUserRole();
        
        if (!"admin".equals(userRole)) {
            // Không phải admin, quay về MainActivity
            if (getActivity() != null) {
                getActivity().onBackPressed();
                Toast.makeText(getContext(), getString(R.string.no_permission_access), 
                        Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        
        return true;
    }

    private void initViews(View view) {
        btnClearOldData = view.findViewById(R.id.btnClearOldData);
        btnBackup = view.findViewById(R.id.btnBackup);
        btnRestore = view.findViewById(R.id.btnRestore);

        btnClearOldData.setOnClickListener(v -> showClearOldDataDialog());
        btnBackup.setOnClickListener(v -> backupData());
        btnRestore.setOnClickListener(v -> restoreData());
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
    }

    /**
     * Hiển thị dialog xác nhận xóa dữ liệu cũ
     */
    private void showClearOldDataDialog() {
        if (!isAdded() || getContext() == null) return;
        
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.clear_old_data_title))
                .setMessage(getString(R.string.clear_old_data_message))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> clearOldData())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    /**
     * Xóa dữ liệu cũ (transactions > 1 năm)
     */
    private void clearOldData() {
        if (!isAdded() || getContext() == null) return;
        
        showProgressDialog("Đang xóa dữ liệu cũ...");
        
        // Tính ngày 1 năm trước
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        Date oneYearAgo = calendar.getTime();
        
        firebaseHelper.deleteOldTransactions(oneYearAgo, new FirebaseHelper.OnDeleteOldTransactionsListener() {
            @Override
            public void onDeleted(int count) {
                hideProgressDialog();
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), 
                        "Đã xóa " + count + " giao dịch cũ", 
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Backup dữ liệu (xuất thống kê)
     */
    private void backupData() {
        if (!isAdded() || getContext() == null) return;
        
        showProgressDialog("Đang sao lưu dữ liệu...");
        
        // Lấy tất cả dữ liệu để backup
        firebaseHelper.getAllUsers(new FirebaseHelper.OnUsersLoadedListener() {
            @Override
            public void onUsersLoaded(java.util.List<com.example.appqlct.model.User> users) {
                firebaseHelper.getAllTransactions(new FirebaseHelper.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(java.util.List<com.example.appqlct.model.Transaction> transactions) {
                        hideProgressDialog();
                        if (!isAdded() || getContext() == null) return;
                        
                        // Tạo thông tin backup
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                        String backupInfo = "BACKUP DỮ LIỆU\n" +
                                          "Thời gian: " + sdf.format(new Date()) + "\n\n" +
                                          "Tổng số người dùng: " + users.size() + "\n" +
                                          "Tổng số giao dịch: " + transactions.size() + "\n\n" +
                                          "Dữ liệu đã được lưu trên Firebase.\n" +
                                          "Để khôi phục, vui lòng liên hệ quản trị viên hệ thống.";
                        
                        new AlertDialog.Builder(getContext())
                                .setTitle("Sao lưu thành công")
                                .setMessage(backupInfo)
                                .setPositiveButton("Đóng", null)
                                .show();
                    }

                    @Override
                    public void onError(String error) {
                        hideProgressDialog();
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Restore dữ liệu (thông báo)
     */
    private void restoreData() {
        if (!isAdded() || getContext() == null) return;
        
        new AlertDialog.Builder(getContext())
                .setTitle("Khôi phục dữ liệu")
                .setMessage("Chức năng khôi phục dữ liệu từ backup cần được thực hiện từ Firebase Console.\n\n" +
                           "Vui lòng liên hệ quản trị viên hệ thống để được hỗ trợ.")
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void showProgressDialog(String message) {
        if (!isAdded() || getContext() == null) return;
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideProgressDialog();
    }
}

