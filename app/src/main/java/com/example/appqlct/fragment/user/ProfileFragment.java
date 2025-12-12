package com.example.appqlct.fragment.user;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appqlct.R;
import com.example.appqlct.LoginActivity;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.helper.NotificationHelper;
import com.example.appqlct.helper.SharedPreferencesHelper;
import com.example.appqlct.model.User;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * ProfileFragment - Hiển thị và chỉnh sửa thông tin người dùng
 * Cho phép đổi avatar, cập nhật thông tin, đăng xuất
 */
public class ProfileFragment extends Fragment {
    private ImageView ivAvatar;
    private EditText etName, etEmail, etPhone, etGender, etDateOfBirth;
    private TextView tvRole;
    private Button btnUpdate, btnLogout, btnChangePassword;
    private FirebaseHelper firebaseHelper;
    private SharedPreferencesHelper prefsHelper;
    private User currentUser;
    private static final int PICK_IMAGE = 1;
    
    // Date formatter
    private SimpleDateFormat dateFormatter;
    private Calendar calendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        initHelpers();
        loadUserInfo();

        return view;
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.ivAvatar);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etGender = view.findViewById(R.id.etGender);
        etDateOfBirth = view.findViewById(R.id.etDateOfBirth);
        tvRole = view.findViewById(R.id.tvRole);
        btnUpdate = view.findViewById(R.id.btnUpdate);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);

        ivAvatar.setOnClickListener(v -> pickImage());
        btnUpdate.setOnClickListener(v -> updateProfile());
        btnLogout.setOnClickListener(v -> logout());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        
        // Chọn giới tính
        etGender.setOnClickListener(v -> showGenderDialog());
        
        // Chọn ngày sinh
        etDateOfBirth.setOnClickListener(v -> showDatePicker());
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        prefsHelper = new SharedPreferencesHelper(requireContext());
        
        // Khởi tạo DateFormatter và Calendar
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        calendar = Calendar.getInstance();
    }

    /**
     * Load thông tin user từ Firestore
     */
    private void loadUserInfo() {
        String userId = prefsHelper.getUserId();
        
        firebaseHelper.getUser(userId, new FirebaseHelper.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                if (!isAdded() || getContext() == null) return;
                currentUser = user;
                displayUserInfo(user);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                String userId = prefsHelper.getUserId();
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.error));
            }
        });
    }

    /**
     * Hiển thị thông tin user
     */
    private void displayUserInfo(User user) {
        // Hiển thị tên
        if (user.getName() != null) {
            etName.setText(user.getName());
        } else {
            etName.setText("");
        }
        
        // Hiển thị email
        if (user.getEmail() != null) {
            etEmail.setText(user.getEmail());
        } else {
            etEmail.setText("");
        }
        
        // Hiển thị số điện thoại (xử lý null safety)
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            etPhone.setText(user.getPhone());
        } else {
            etPhone.setText("");
        }
        
        // Hiển thị giới tính
        if (user.getGender() != null && !user.getGender().isEmpty()) {
            etGender.setText(user.getGender());
        } else {
            etGender.setText("");
        }
        
        // Hiển thị ngày sinh
        if (user.getDateOfBirth() != null && !user.getDateOfBirth().isEmpty()) {
            etDateOfBirth.setText(user.getDateOfBirth());
        } else {
            etDateOfBirth.setText("");
        }
        
        // Hiển thị vai trò
        if (user.getRole() != null) {
            tvRole.setText(user.getRole().equals("admin") ? getString(R.string.admin) : getString(R.string.user));
        } else {
            tvRole.setText(getString(R.string.user));
        }

        // Load avatar nếu có
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            // Thêm timestamp để force reload và tắt cache
            String avatarUrlWithTimestamp = user.getAvatarUrl() + "?t=" + System.currentTimeMillis();
            Picasso.get()
                    .load(avatarUrlWithTimestamp)
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.user);
        }
    }

    /**
     * Chọn ảnh từ gallery
     */
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            ivAvatar.setImageURI(imageUri);
            // TODO: Upload image to Firebase Storage và cập nhật avatarUrl
        }
    }

    /**
     * Cập nhật thông tin profile
     */
    private void updateProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String gender = etGender.getText().toString().trim();
        String dateOfBirth = etDateOfBirth.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError(getString(R.string.required_field));
            return;
        }

        // Đảm bảo currentUser không null
        if (currentUser == null) {
            String userId = prefsHelper.getUserId();
            NotificationHelper.addErrorNotification(getContext(), userId, 
                    "Không thể tải thông tin người dùng");
            loadUserInfo(); // Reload user info
            return;
        }

        // Cập nhật thông tin
        currentUser.setName(name);
        currentUser.setPhone(phone != null ? phone : "");
        currentUser.setGender(gender != null ? gender : "");
        currentUser.setDateOfBirth(dateOfBirth != null ? dateOfBirth : "");

        firebaseHelper.saveUser(currentUser, task -> {
            if (!isAdded() || getContext() == null) return;
            String userId = prefsHelper.getUserId();
            if (task.isSuccessful()) {
                NotificationHelper.addSuccessNotification(getContext(), userId, 
                        getString(R.string.update_success));
                // Reload để đảm bảo hiển thị đúng
                loadUserInfo();
            } else {
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.update_error));
            }
        });
    }
    
    /**
     * Hiển thị dialog chọn giới tính
     */
    private void showGenderDialog() {
        String[] genders = {
            getString(R.string.male),
            getString(R.string.female),
            getString(R.string.other)
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.select_gender));
        builder.setItems(genders, (dialog, which) -> {
            String selectedGender = genders[which];
            etGender.setText(selectedGender);
        });
        builder.show();
    }
    
    /**
     * Hiển thị DatePickerDialog để chọn ngày sinh
     */
    private void showDatePicker() {
        // Nếu đã có ngày sinh, parse để hiển thị trong DatePicker
        String currentDate = etDateOfBirth.getText().toString().trim();
        if (!currentDate.isEmpty()) {
            try {
                calendar.setTime(dateFormatter.parse(currentDate));
            } catch (Exception e) {
                // Nếu parse lỗi, dùng ngày hiện tại
                calendar = Calendar.getInstance();
            }
        }
        
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, selectedYear, selectedMonth, selectedDay) -> {
                calendar.set(selectedYear, selectedMonth, selectedDay);
                String formattedDate = dateFormatter.format(calendar.getTime());
                etDateOfBirth.setText(formattedDate);
            },
            year, month, day
        );
        
        // Giới hạn ngày sinh không được sau ngày hiện tại
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    /**
     * Hiển thị dialog đổi mật khẩu
     */
    private void showChangePasswordDialog() {
        // Kiểm tra xem user có thể đổi mật khẩu không (không áp dụng cho Google/Facebook login)
        if (!firebaseHelper.canChangePassword()) {
            String userId = prefsHelper.getUserId();
            NotificationHelper.addInfoNotification(getContext(), userId, 
                    getString(R.string.cannot_change_password_social));
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        
        TextInputEditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmNewPassword = dialogView.findViewById(R.id.etConfirmNewPassword);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        builder.setTitle(getString(R.string.change_password_title));
        builder.setPositiveButton(getString(R.string.save), null); // Set null để tự xử lý
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String oldPassword = etOldPassword.getText().toString().trim();
                String newPassword = etNewPassword.getText().toString().trim();
                String confirmPassword = etConfirmNewPassword.getText().toString().trim();

                // Validation
                if (oldPassword.isEmpty()) {
                    etOldPassword.setError(getString(R.string.required_field));
                    return;
                }

                if (newPassword.isEmpty()) {
                    etNewPassword.setError(getString(R.string.required_field));
                    return;
                }

                if (newPassword.length() < 6) {
                    etNewPassword.setError(getString(R.string.password_too_short));
                    return;
                }

                if (confirmPassword.isEmpty()) {
                    etConfirmNewPassword.setError(getString(R.string.required_field));
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    etConfirmNewPassword.setError(getString(R.string.passwords_do_not_match));
                    return;
                }

                if (oldPassword.equals(newPassword)) {
                    etNewPassword.setError(getString(R.string.same_password));
                    return;
                }

                // Đổi mật khẩu
                changePassword(oldPassword, newPassword, dialog);
            });
        });

        dialog.show();
    }

    /**
     * Thực hiện đổi mật khẩu
     */
    private void changePassword(String oldPassword, String newPassword, AlertDialog dialog) {
        firebaseHelper.changePassword(oldPassword, newPassword, new FirebaseHelper.OnPasswordChangeListener() {
            @Override
            public void onSuccess() {
                if (!isAdded() || getContext() == null) return;
                dialog.dismiss();
                String userId = prefsHelper.getUserId();
                NotificationHelper.addSuccessNotification(getContext(), userId, 
                        getString(R.string.password_changed_success));
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                String userId = prefsHelper.getUserId();
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.password_change_error) + ": " + error);
            }
        });
    }

    /**
     * Đăng xuất
     */
    private void logout() {
        prefsHelper.clearSession();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}

