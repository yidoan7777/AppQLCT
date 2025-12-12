package com.example.appqlct.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.appqlct.R;
import com.example.appqlct.model.Notification;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Helper class để quản lý thông báo
 * Lưu trữ thông báo trong SharedPreferences theo userId
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String PREFS_NAME = "notifications_prefs";
    private static final String KEY_NOTIFICATIONS_PREFIX = "notifications_";
    public static final String ACTION_NOTIFICATION_ADDED = "com.example.appqlct.NOTIFICATION_ADDED";
    
    private SharedPreferences prefs;
    private Gson gson;
    private Context context;
    private String currentUserId;
    
    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        // Lấy userId hiện tại
        SharedPreferencesHelper spHelper = new SharedPreferencesHelper(this.context);
        this.currentUserId = spHelper.getUserId();
    }
    
    /**
     * Lấy key để lưu thông báo theo userId
     */
    private String getNotificationsKey(String userId) {
        if (userId == null || userId.isEmpty()) {
            return KEY_NOTIFICATIONS_PREFIX + "default";
        }
        return KEY_NOTIFICATIONS_PREFIX + userId;
    }
    
    /**
     * Static method để thêm thông báo từ bất kỳ đâu (tiện lợi cho Fragment)
     */
    public static void addNotificationStatic(Context context, String userId, String title, String message, String type) {
        NotificationHelper helper = new NotificationHelper(context);
        Notification notification = new Notification(null, userId, title, message, type);
        helper.addNotification(notification);
        
        // Gửi broadcast để MainActivity biết có thông báo mới
        Intent intent = new Intent(ACTION_NOTIFICATION_ADDED);
        context.sendBroadcast(intent);
    }
    
    /**
     * Static method để thêm thông báo thành công
     */
    public static void addSuccessNotification(Context context, String userId, String message) {
        addNotificationStatic(context, userId, context.getString(R.string.success), message, "success");
    }
    
    /**
     * Static method để thêm thông báo lỗi
     */
    public static void addErrorNotification(Context context, String userId, String message) {
        addNotificationStatic(context, userId, context.getString(R.string.error), message, "error");
    }
    
    /**
     * Static method để thêm thông báo thông tin
     */
    public static void addInfoNotification(Context context, String userId, String message) {
        addNotificationStatic(context, userId, context.getString(R.string.notification), message, "info");
    }
    
    /**
     * Thêm thông báo mới
     */
    public void addNotification(Notification notification) {
        String userId = notification.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot add notification: userId is null or empty");
            return;
        }
        
        List<Notification> notifications = getNotificationsByUserId(userId);
        if (notifications == null) {
            notifications = new ArrayList<>();
        }
        
        // Tạo ID nếu chưa có
        if (notification.getId() == null || notification.getId().isEmpty()) {
            notification.setId(String.valueOf(System.currentTimeMillis()));
        }
        
        notifications.add(notification);
        saveNotificationsByUserId(userId, notifications);
        
        // Gửi broadcast để MainActivity biết có thông báo mới
        if (context != null) {
            Intent intent = new Intent(ACTION_NOTIFICATION_ADDED);
            context.sendBroadcast(intent);
        }
    }
    
    /**
     * Lấy tất cả thông báo của user hiện tại (sắp xếp theo thời gian mới nhất)
     */
    public List<Notification> getNotifications() {
        return getNotificationsByUserId(currentUserId);
    }
    
    /**
     * Lấy tất cả thông báo theo userId (sắp xếp theo thời gian mới nhất)
     */
    public List<Notification> getNotificationsByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return new ArrayList<>();
        }
        
        String key = getNotificationsKey(userId);
        String json = prefs.getString(key, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            Type type = new TypeToken<List<Notification>>(){}.getType();
            List<Notification> notifications = gson.fromJson(json, type);
            
            if (notifications == null) {
                return new ArrayList<>();
            }
            
            // Lọc chỉ thông báo của userId này (đảm bảo an toàn)
            List<Notification> filteredNotifications = new ArrayList<>();
            for (Notification notification : notifications) {
                if (userId.equals(notification.getUserId())) {
                    filteredNotifications.add(notification);
                }
            }
            
            // Sắp xếp theo thời gian mới nhất trước
            Collections.sort(filteredNotifications, new Comparator<Notification>() {
                @Override
                public int compare(Notification n1, Notification n2) {
                    Date d1 = n1.getCreatedAt();
                    Date d2 = n2.getCreatedAt();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1); // Mới nhất trước
                }
            });
            
            return filteredNotifications;
        } catch (Exception e) {
            Log.e(TAG, "Error loading notifications for userId: " + userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Lấy số thông báo chưa đọc của user hiện tại
     */
    public int getUnreadCount() {
        return getUnreadCountByUserId(currentUserId);
    }
    
    /**
     * Lấy số thông báo chưa đọc theo userId
     */
    public int getUnreadCountByUserId(String userId) {
        List<Notification> notifications = getNotificationsByUserId(userId);
        if (notifications == null) return 0;
        
        int count = 0;
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Lấy số thông báo budget_warning chưa đọc của user hiện tại
     */
    public int getBudgetWarningUnreadCount() {
        return getBudgetWarningUnreadCountByUserId(currentUserId);
    }
    
    /**
     * Lấy số thông báo budget_warning chưa đọc theo userId
     */
    public int getBudgetWarningUnreadCountByUserId(String userId) {
        List<Notification> notifications = getNotificationsByUserId(userId);
        if (notifications == null) return 0;
        
        int count = 0;
        for (Notification notification : notifications) {
            if ("budget_warning".equals(notification.getType()) && !notification.isRead()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Đánh dấu thông báo là đã đọc
     */
    public void markAsRead(String notificationId) {
        markAsReadByUserId(notificationId, currentUserId);
    }
    
    /**
     * Đánh dấu thông báo là đã đọc theo userId
     */
    public void markAsReadByUserId(String notificationId, String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        List<Notification> notifications = getNotificationsByUserId(userId);
        if (notifications == null) return;
        
        boolean found = false;
        for (Notification notification : notifications) {
            if (notification.getId().equals(notificationId) && userId.equals(notification.getUserId())) {
                notification.setRead(true);
                found = true;
                break;
            }
        }
        
        if (found) {
            saveNotificationsByUserId(userId, notifications);
        }
    }
    
    /**
     * Đánh dấu tất cả thông báo là đã đọc
     */
    public void markAllAsRead() {
        markAllAsReadByUserId(currentUserId);
    }
    
    /**
     * Đánh dấu tất cả thông báo là đã đọc theo userId
     */
    public void markAllAsReadByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        List<Notification> notifications = getNotificationsByUserId(userId);
        if (notifications == null) return;
        
        for (Notification notification : notifications) {
            notification.setRead(true);
        }
        
        saveNotificationsByUserId(userId, notifications);
    }
    
    /**
     * Đánh dấu tất cả thông báo budget_warning là đã đọc của user hiện tại
     */
    public void markAllBudgetWarningsAsRead() {
        markAllBudgetWarningsAsReadByUserId(currentUserId);
    }
    
    /**
     * Đánh dấu tất cả thông báo budget_warning là đã đọc theo userId
     */
    public void markAllBudgetWarningsAsReadByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        List<Notification> notifications = getNotificationsByUserId(userId);
        if (notifications == null) return;
        
        boolean changed = false;
        for (Notification notification : notifications) {
            if ("budget_warning".equals(notification.getType()) && !notification.isRead()) {
                notification.setRead(true);
                changed = true;
            }
        }
        
        if (changed) {
            saveNotificationsByUserId(userId, notifications);
        }
    }
    
    /**
     * Xóa thông báo
     */
    public void deleteNotification(String notificationId) {
        deleteNotificationByUserId(notificationId, currentUserId);
    }
    
    /**
     * Xóa thông báo theo userId
     */
    public void deleteNotificationByUserId(String notificationId, String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        List<Notification> notifications = getNotificationsByUserId(userId);
        if (notifications == null) return;
        
        notifications.removeIf(n -> n.getId().equals(notificationId) && userId.equals(n.getUserId()));
        saveNotificationsByUserId(userId, notifications);
    }
    
    /**
     * Xóa tất cả thông báo của user hiện tại
     */
    public void deleteAllNotifications() {
        deleteAllNotificationsByUserId(currentUserId);
    }
    
    /**
     * Xóa tất cả thông báo theo userId
     */
    public void deleteAllNotificationsByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        saveNotificationsByUserId(userId, new ArrayList<>());
    }
    
    /**
     * Xóa tất cả thông báo budget_warning chưa đọc của tháng hiện tại
     */
    public void deleteUnreadBudgetWarningsThisMonth() {
        deleteUnreadBudgetWarningsThisMonthByUserId(currentUserId);
    }
    
    /**
     * Xóa tất cả thông báo budget_warning chưa đọc của tháng hiện tại theo userId
     */
    public void deleteUnreadBudgetWarningsThisMonthByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        List<Notification> notifications = getNotificationsByUserId(userId);
        if (notifications == null) return;
        
        Calendar currentMonth = Calendar.getInstance();
        int currentYear = currentMonth.get(Calendar.YEAR);
        int currentMonthValue = currentMonth.get(Calendar.MONTH);
        
        boolean changed = false;
        List<Notification> toRemove = new ArrayList<>();
        
        for (Notification notification : notifications) {
            if ("budget_warning".equals(notification.getType()) && 
                userId.equals(notification.getUserId()) && 
                !notification.isRead() &&
                notification.getCreatedAt() != null) {
                
                Calendar notifMonth = Calendar.getInstance();
                notifMonth.setTime(notification.getCreatedAt());
                int notifYear = notifMonth.get(Calendar.YEAR);
                int notifMonthValue = notifMonth.get(Calendar.MONTH);
                
                // Nếu notification được tạo trong cùng tháng và chưa đọc, xóa nó
                if (notifYear == currentYear && notifMonthValue == currentMonthValue) {
                    toRemove.add(notification);
                    changed = true;
                }
            }
        }
        
        if (changed) {
            notifications.removeAll(toRemove);
            saveNotificationsByUserId(userId, notifications);
        }
    }
    
    /**
     * Lưu danh sách thông báo theo userId
     */
    private void saveNotificationsByUserId(String userId, List<Notification> notifications) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot save notifications: userId is null or empty");
            return;
        }
        
        try {
            String key = getNotificationsKey(userId);
            String json = gson.toJson(notifications);
            prefs.edit().putString(key, json).apply();
            Log.d(TAG, "Saved " + notifications.size() + " notifications for userId: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error saving notifications for userId: " + userId, e);
        }
    }
}

