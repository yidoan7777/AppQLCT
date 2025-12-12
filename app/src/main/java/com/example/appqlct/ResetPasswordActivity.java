package com.example.appqlct;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appqlct.helper.OTPHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity để nhập mã OTP và đặt lại mật khẩu
 */
public class ResetPasswordActivity extends AppCompatActivity {
    private static final String TAG = "ResetPasswordActivity";
    
    private TextInputLayout tilOTP, tilNewPassword, tilConfirmPassword;
    private EditText etOTP, etNewPassword, etConfirmPassword;
    private Button btnVerifyOTP, btnResetPassword;
    private TextView tvResendOTP, tvEmail;
    private ProgressBar progressBar;
    
    private String email;
    private OTPHelper otpHelper;
    private FirebaseAuth auth;
    private FirebaseFunctions functions;
    private boolean isOTPVerified = false;
    
    /**
     * Normalize email (lowercase, trim)
     */
    private String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        
        // Lấy email từ Intent và normalize
        String emailFromIntent = getIntent().getStringExtra("email");
        email = normalizeEmail(emailFromIntent);
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Log.d(TAG, "ResetPasswordActivity started for email: " + email);
        
        // Khởi tạo
        auth = FirebaseAuth.getInstance();
        otpHelper = new OTPHelper();
        functions = FirebaseFunctions.getInstance();
        
        // Khởi tạo UI
        initViews();
        setupClickListeners();
        
        // Hiển thị màn hình nhập OTP ban đầu
        showOTPInputScreen();
    }
    
    private void initViews() {
        tilOTP = findViewById(R.id.tilOTP);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etOTP = findViewById(R.id.etOTP);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnVerifyOTP = findViewById(R.id.btnVerifyOTP);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvResendOTP = findViewById(R.id.tvResendOTP);
        tvEmail = findViewById(R.id.tvEmail);
        progressBar = findViewById(R.id.progressBar);
        
        // Hiển thị email đang reset
        if (tvEmail != null && email != null) {
            tvEmail.setText("Email: " + email);
        }
        
        // Ẩn các trường mật khẩu ban đầu
        tilNewPassword.setVisibility(View.GONE);
        tilConfirmPassword.setVisibility(View.GONE);
        btnResetPassword.setVisibility(View.GONE);
    }
    
    private void setupClickListeners() {
        btnVerifyOTP.setOnClickListener(v -> handleVerifyOTP());
        btnResetPassword.setOnClickListener(v -> handleResetPassword());
        tvResendOTP.setOnClickListener(v -> handleResendOTP());
    }
    
    /**
     * Hiển thị màn hình nhập OTP
     */
    private void showOTPInputScreen() {
        tilOTP.setVisibility(View.VISIBLE);
        tilNewPassword.setVisibility(View.GONE);
        tilConfirmPassword.setVisibility(View.GONE);
        btnVerifyOTP.setVisibility(View.VISIBLE);
        btnResetPassword.setVisibility(View.GONE);
        tvResendOTP.setVisibility(View.VISIBLE);
    }
    
    /**
     * Hiển thị màn hình nhập mật khẩu mới (sau khi OTP đã được xác thực)
     */
    private void showPasswordInputScreen() {
        tilOTP.setVisibility(View.GONE);
        tilNewPassword.setVisibility(View.VISIBLE);
        tilConfirmPassword.setVisibility(View.VISIBLE);
        btnVerifyOTP.setVisibility(View.GONE);
        btnResetPassword.setVisibility(View.VISIBLE);
        tvResendOTP.setVisibility(View.GONE);
    }
    
    /**
     * Xử lý xác thực mã OTP
     */
    private void handleVerifyOTP() {
        String otp = etOTP.getText().toString().trim();
        
        if (TextUtils.isEmpty(otp)) {
            tilOTP.setError(getString(R.string.required_field));
            etOTP.requestFocus();
            return;
        }
        
        // Remove spaces và normalize OTP code
        otp = otp.replaceAll("\\s+", "");
        
        if (otp.length() != 6) {
            tilOTP.setError(getString(R.string.otp_invalid_length));
            etOTP.requestFocus();
            return;
        }
        
        tilOTP.setError(null);
        showLoading(true);
        
        Log.d(TAG, "Verifying OTP for email: " + email + ", OTP length: " + otp.length());
        
        otpHelper.verifyOTP(email, otp, new OTPHelper.OnOTPVerifiedListener() {
            @Override
            public void onOTPVerified() {
                showLoading(false);
                isOTPVerified = true;
                Toast.makeText(ResetPasswordActivity.this, 
                        getString(R.string.otp_verified_success), 
                        Toast.LENGTH_SHORT).show();
                showPasswordInputScreen();
            }
            
            @Override
            public void onError(String error) {
                showLoading(false);
                tilOTP.setError(error);
                Toast.makeText(ResetPasswordActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Xử lý đặt lại mật khẩu
     * Sau khi xác thực OTP thành công, đặt lại mật khẩu trực tiếp trong app
     */
    private void handleResetPassword() {
        if (!isOTPVerified) {
            Toast.makeText(this, getString(R.string.please_verify_otp_first), Toast.LENGTH_SHORT).show();
            return;
        }
        
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        // Validation
        if (TextUtils.isEmpty(newPassword)) {
            tilNewPassword.setError(getString(R.string.required_field));
            etNewPassword.requestFocus();
            return;
        } else {
            tilNewPassword.setError(null);
        }
        
        if (newPassword.length() < 6) {
            tilNewPassword.setError(getString(R.string.password_too_short));
            etNewPassword.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.required_field));
            etConfirmPassword.requestFocus();
            return;
        } else {
            tilConfirmPassword.setError(null);
        }
        
        if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.passwords_do_not_match));
            etConfirmPassword.requestFocus();
            return;
        }
        
        showLoading(true);
        
        // Gọi Cloud Function để reset password sau khi verify OTP thành công
        // Cloud Function sẽ sử dụng Admin SDK để reset password mà không cần action code
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("newPassword", newPassword);
        
        functions.getHttpsCallable("resetPasswordAfterOTP")
                .call(data)
                .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                    @Override
                    public void onComplete(Task<HttpsCallableResult> task) {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            // Password đã được reset thành công
                            HttpsCallableResult result = task.getResult();
                            Map<String, Object> resultData = (Map<String, Object>) result.getData();
                            
                            Toast.makeText(ResetPasswordActivity.this, 
                                    "Mật khẩu đã được đặt lại thành công!", 
                                    Toast.LENGTH_LONG).show();
                            
                            // Quay về màn hình đăng nhập
                            finish();
                        } else {
                            // Xử lý lỗi
                            Exception exception = task.getException();
                            Log.e(TAG, "Error resetting password via Cloud Function", exception);
                            
                            String errorMessage = "Lỗi khi đặt lại mật khẩu";
                            if (exception != null && exception.getMessage() != null) {
                                errorMessage = exception.getMessage();
                            }
                            
                            Toast.makeText(ResetPasswordActivity.this, 
                                    errorMessage, 
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    
    /**
     * Xử lý gửi lại mã OTP
     */
    private void handleResendOTP() {
        showLoading(true);
        
        otpHelper.createOTP(email, new OTPHelper.OnOTPCreatedListener() {
            @Override
            public void onOTPCreated(String otpCode) {
                showLoading(false);
                Toast.makeText(ResetPasswordActivity.this, 
                        getString(R.string.otp_resent_success), 
                        Toast.LENGTH_SHORT).show();
                
                // Gửi email chứa mã OTP
                sendOTPEmail(email, otpCode);
            }
            
            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(ResetPasswordActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Gửi email chứa mã OTP
     */
    private void sendOTPEmail(String email, String otpCode) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("email", email);
            data.put("otpCode", otpCode);
            
            functions.getHttpsCallable("sendOTPEmail")
                    .call(data)
                    .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                        @Override
                        public void onComplete(Task<HttpsCallableResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "OTP email sent successfully");
                                Toast.makeText(ResetPasswordActivity.this, 
                                        "Mã OTP đã được gửi đến email " + email, 
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Log.e(TAG, "Error sending OTP email", task.getException());
                                // Nếu Cloud Functions chưa được deploy, log OTP để test
                                Log.d(TAG, "OTP Code for " + email + ": " + otpCode);
                                Toast.makeText(ResetPasswordActivity.this, 
                                        "Mã OTP đã được tạo. Vui lòng kiểm tra email hoặc log để lấy mã OTP (cho testing).", 
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error calling Cloud Function", e);
            // Nếu Cloud Functions chưa được deploy, log OTP để test
            Log.d(TAG, "OTP Code for " + email + ": " + otpCode);
            Toast.makeText(this, 
                    "Mã OTP đã được tạo. Vui lòng kiểm tra email hoặc log để lấy mã OTP (cho testing).", 
                    Toast.LENGTH_LONG).show();
        }
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnVerifyOTP.setEnabled(!show);
        btnResetPassword.setEnabled(!show);
        tvResendOTP.setEnabled(!show);
    }
}

