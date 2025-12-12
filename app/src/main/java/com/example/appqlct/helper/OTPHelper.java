package com.example.appqlct.helper;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Helper class để quản lý OTP (One-Time Password) cho tính năng quên mật khẩu
 */
public class OTPHelper {
    private static final String TAG = "OTPHelper";
    private static final String COLLECTION_OTP = "otp_codes";
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_TIME = 10 * 60 * 1000; // 10 phút (milliseconds)
    
    private FirebaseFirestore db;
    
    public OTPHelper() {
        db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Tạo mã OTP ngẫu nhiên 6 chữ số
     */
    private String generateOTP() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
    
    /**
     * Normalize email để đảm bảo consistency (lowercase, trim)
     */
    private String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }
    
    /**
     * Normalize OTP code (trim, remove spaces)
     */
    private String normalizeOTPCode(String otpCode) {
        if (otpCode == null) return null;
        return otpCode.trim().replaceAll("\\s+", "");
    }
    
    /**
     * Tạo và lưu mã OTP vào Firestore cho email
     * @param email Email của user
     * @param listener Callback khi hoàn thành
     */
    public void createOTP(String email, OnOTPCreatedListener listener) {
        // Normalize email để đảm bảo consistency
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            listener.onError("Email không hợp lệ");
            return;
        }
        
        String otpCode = generateOTP();
        long expiryTime = System.currentTimeMillis() + OTP_EXPIRY_TIME;
        
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("email", normalizedEmail);
        otpData.put("code", otpCode);
        otpData.put("createdAt", new Date());
        otpData.put("expiresAt", new Date(expiryTime));
        otpData.put("used", false);
        
        Log.d(TAG, "Creating OTP for email: " + normalizedEmail + ", OTP: " + otpCode);
        
        // Lưu OTP vào Firestore với document ID là email đã normalize
        db.collection(COLLECTION_OTP)
                .document(normalizedEmail)
                .set(otpData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "OTP created successfully for email: " + normalizedEmail + ", OTP: " + otpCode);
                        listener.onOTPCreated(otpCode);
                    } else {
                        Log.e(TAG, "Error creating OTP for email: " + normalizedEmail, task.getException());
                        listener.onError(task.getException() != null ? 
                                task.getException().getMessage() : "Lỗi khi tạo mã OTP");
                    }
                });
    }
    
    /**
     * Xác thực mã OTP
     * @param email Email của user
     * @param otpCode Mã OTP cần xác thực
     * @param listener Callback khi hoàn thành
     */
    public void verifyOTP(String email, String otpCode, OnOTPVerifiedListener listener) {
        // Normalize email và OTP code để đảm bảo consistency
        String normalizedEmail = normalizeEmail(email);
        String normalizedOTP = normalizeOTPCode(otpCode);
        
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            listener.onError("Email không hợp lệ");
            return;
        }
        
        if (normalizedOTP == null || normalizedOTP.isEmpty()) {
            listener.onError("Mã OTP không hợp lệ");
            return;
        }
        
        Log.d(TAG, "Verifying OTP for email: " + normalizedEmail + ", OTP entered: " + normalizedOTP);
        
        db.collection(COLLECTION_OTP)
                .document(normalizedEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String storedCode = document.getString("code");
                            Boolean used = document.getBoolean("used");
                            Date expiresAt = document.getDate("expiresAt");
                            String storedEmail = document.getString("email");
                            
                            Log.d(TAG, "OTP document found - storedCode: " + storedCode + 
                                    ", used: " + used + ", expiresAt: " + expiresAt + 
                                    ", storedEmail: " + storedEmail);
                            
                            // Kiểm tra mã đã được sử dụng chưa
                            if (used != null && used) {
                                Log.w(TAG, "OTP already used for email: " + normalizedEmail);
                                listener.onError("Mã OTP đã được sử dụng");
                                return;
                            }
                            
                            // Kiểm tra mã đã hết hạn chưa
                            if (expiresAt != null && expiresAt.before(new Date())) {
                                Log.w(TAG, "OTP expired for email: " + normalizedEmail + 
                                        ", expiresAt: " + expiresAt + ", now: " + new Date());
                                listener.onError("Mã OTP đã hết hạn");
                                return;
                            }
                            
                            // Normalize stored code để so sánh
                            String normalizedStoredCode = normalizeOTPCode(storedCode);
                            
                            // Kiểm tra mã OTP có đúng không (so sánh đã normalize)
                            if (normalizedStoredCode != null && normalizedStoredCode.equals(normalizedOTP)) {
                                Log.d(TAG, "OTP verified successfully for email: " + normalizedEmail);
                                // Đánh dấu mã đã được sử dụng
                                Map<String, Object> updateData = new HashMap<>();
                                updateData.put("used", true);
                                db.collection(COLLECTION_OTP)
                                        .document(normalizedEmail)
                                        .update(updateData)
                                        .addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                Log.d(TAG, "OTP marked as used for email: " + normalizedEmail);
                                                listener.onOTPVerified();
                                            } else {
                                                Log.e(TAG, "Error updating OTP status", updateTask.getException());
                                                listener.onError("Lỗi khi cập nhật trạng thái OTP");
                                            }
                                        });
                            } else {
                                Log.w(TAG, "OTP mismatch for email: " + normalizedEmail + 
                                        ", stored: " + normalizedStoredCode + 
                                        ", entered: " + normalizedOTP);
                                listener.onError("Mã OTP không đúng");
                            }
                        } else {
                            Log.w(TAG, "OTP document not found for email: " + normalizedEmail);
                            listener.onError("Không tìm thấy mã OTP cho email này. Vui lòng yêu cầu mã mới.");
                        }
                    } else {
                        Log.e(TAG, "Error verifying OTP for email: " + normalizedEmail, task.getException());
                        listener.onError(task.getException() != null ? 
                                task.getException().getMessage() : "Lỗi khi xác thực mã OTP");
                    }
                });
    }
    
    /**
     * Xóa mã OTP đã hết hạn (có thể gọi định kỳ để dọn dẹp)
     */
    public void cleanupExpiredOTPs() {
        // Có thể implement sau nếu cần
    }
    
    public interface OnOTPCreatedListener {
        void onOTPCreated(String otpCode);
        void onError(String error);
    }
    
    public interface OnOTPVerifiedListener {
        void onOTPVerified();
        void onError(String error);
    }
}

