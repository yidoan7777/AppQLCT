package com.example.appqlct.fragment.user;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.adapter.NotificationAdapter;
import com.example.appqlct.helper.NotificationHelper;
import com.example.appqlct.model.Notification;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment hiển thị tất cả thông báo
 */
public class NotificationFragment extends Fragment {
    private static final String TAG = "NotificationFragment";
    
    private RecyclerView rvNotifications;
    private LinearLayout llEmpty;
    private TextView tvClearAll;
    private NotificationHelper notificationHelper;
    private NotificationAdapter adapter;
    private List<Notification> notifications;
    
    // BroadcastReceiver để lắng nghe thông báo mới
    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NotificationHelper.ACTION_NOTIFICATION_ADDED.equals(intent.getAction())) {
                loadNotifications();
            }
        }
    };
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationHelper = new NotificationHelper(requireContext());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        loadNotifications();
        
        // Xử lý nút "Xóa tất cả"
        tvClearAll.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirm))
                    .setMessage(getString(R.string.delete_all_notifications_confirm))
                    .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                        notificationHelper.deleteAllNotifications();
                        loadNotifications();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Đăng ký BroadcastReceiver
        IntentFilter filter = new IntentFilter(NotificationHelper.ACTION_NOTIFICATION_ADDED);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(notificationReceiver, filter);
        }
        // Refresh notificationHelper để lấy userId mới (khi đăng nhập user khác)
        notificationHelper = new NotificationHelper(requireContext());
        // Load lại thông báo khi resume
        loadNotifications();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Hủy đăng ký BroadcastReceiver
        try {
            requireContext().unregisterReceiver(notificationReceiver);
        } catch (Exception e) {
            // Ignore nếu receiver chưa được đăng ký
        }
    }
    
    private void initViews(View view) {
        rvNotifications = view.findViewById(R.id.rvNotifications);
        llEmpty = view.findViewById(R.id.llEmpty);
        tvClearAll = view.findViewById(R.id.tvClearAll);
    }
    
    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
    }
    
    private void loadNotifications() {
        // Refresh userId mỗi lần load để đảm bảo lấy đúng user hiện tại
        notificationHelper = new NotificationHelper(requireContext());
        List<Notification> allNotifications = notificationHelper.getNotifications();
        
        // CHỈ lọc lấy các thông báo cảnh báo ngân sách (budget_warning)
        notifications = new ArrayList<>();
        if (allNotifications != null) {
            for (Notification notification : allNotifications) {
                if ("budget_warning".equals(notification.getType())) {
                    notifications.add(notification);
                }
            }
        }
        
        Log.d(TAG, "Budget warning notifications count: " + (notifications != null ? notifications.size() : 0));
        
        if (notifications == null || notifications.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            llEmpty.setVisibility(View.VISIBLE);
            if (adapter != null) {
                adapter.updateNotifications(new ArrayList<>());
            }
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            llEmpty.setVisibility(View.GONE);
            
            // Setup adapter
            if (adapter == null) {
                adapter = new NotificationAdapter(notifications);
                rvNotifications.setAdapter(adapter);
                
                // Xử lý click vào thông báo
                adapter.setOnNotificationClickListener(new NotificationAdapter.OnNotificationClickListener() {
                    @Override
                    public void onNotificationClick(Notification notification) {
                        // Đánh dấu đã đọc
                        notificationHelper.markAsRead(notification.getId());
                        notification.setRead(true);
                        adapter.notifyDataSetChanged();
                        
                        // Hiển thị chi tiết thông báo (nếu là budget warning)
                        if ("budget_warning".equals(notification.getType())) {
                            // Có thể mở dialog hoặc chuyển đến trang budget
                            // Tạm thời chỉ đánh dấu đã đọc
                        }
                    }
                    
                    @Override
                    public void onNotificationDelete(Notification notification) {
                        notificationHelper.deleteNotification(notification.getId());
                        notifications.remove(notification);
                        adapter.updateNotifications(notifications);
                        
                        if (notifications.isEmpty()) {
                            rvNotifications.setVisibility(View.GONE);
                            llEmpty.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } else {
                adapter.updateNotifications(notifications);
            }
        }
    }
}

