package com.example.appqlct.helper;

import android.content.Context;
import android.widget.Toast;

/**
 * Helper class để quản lý Toast messages và tránh hiển thị trùng lặp
 */
public class ToastHelper {
    private static Toast currentToast = null;
    
    /**
     * Hiển thị Toast message, tự động cancel Toast cũ nếu đang hiển thị
     */
    public static void showToast(Context context, String message, int duration) {
        // Cancel Toast cũ nếu đang hiển thị
        if (currentToast != null) {
            currentToast.cancel();
        }
        
        // Tạo và hiển thị Toast mới
        currentToast = Toast.makeText(context.getApplicationContext(), message, duration);
        currentToast.show();
    }
    
    /**
     * Hiển thị Toast message ngắn
     */
    public static void showShort(Context context, String message) {
        showToast(context, message, Toast.LENGTH_SHORT);
    }
    
    /**
     * Hiển thị Toast message dài
     */
    public static void showLong(Context context, String message) {
        showToast(context, message, Toast.LENGTH_LONG);
    }
    
    /**
     * Cancel Toast hiện tại nếu đang hiển thị
     */
    public static void cancel() {
        if (currentToast != null) {
            currentToast.cancel();
            currentToast = null;
        }
    }
}

