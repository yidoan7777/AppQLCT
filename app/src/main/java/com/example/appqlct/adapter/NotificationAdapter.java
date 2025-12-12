package com.example.appqlct.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.model.Notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter cho RecyclerView hiển thị danh sách thông báo
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
        void onNotificationDelete(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private FrameLayout iconContainer;
        private ImageView imgIcon;
        private TextView tvTitle;
        private TextView tvMessage;
        private TextView tvTime;
        private View viewUnread;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            viewUnread = itemView.findViewById(R.id.viewUnread);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNotificationClick(notifications.get(position));
                }
            });
        }

        void bind(Notification notification) {
            tvTitle.setText(notification.getTitle());
            tvMessage.setText(notification.getMessage());
            
            // Set màu sắc và icon dựa trên type
            String type = notification.getType();
            int iconBgResId;
            int iconResId;
            int iconTintColor;
            
            if ("budget_warning".equals(type)) {
                iconBgResId = R.drawable.bg_circle_warning;
                iconResId = R.drawable.ic_notification_warning;
                iconTintColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
            } else if ("success".equals(type) || "feedback_success".equals(type)) {
                iconBgResId = R.drawable.bg_circle_success;
                iconResId = R.drawable.ic_notification_success;
                iconTintColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
            } else if ("error".equals(type)) {
                iconBgResId = R.drawable.bg_circle_error;
                iconResId = R.drawable.ic_notification_warning;
                iconTintColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
            } else {
                // Default: info
                iconBgResId = R.drawable.bg_circle_info;
                iconResId = R.drawable.ic_notification;
                iconTintColor = ContextCompat.getColor(itemView.getContext(), R.color.white);
            }
            
            // Set background cho icon container
            iconContainer.setBackgroundResource(iconBgResId);
            
            // Set icon
            imgIcon.setImageResource(iconResId);
            imgIcon.setColorFilter(iconTintColor);
            
            // Hiển thị thời gian
            tvTime.setText(formatTime(notification.getCreatedAt()));
            
            // Hiển thị/ẩn indicator chưa đọc
            viewUnread.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
        }

        private String formatTime(Date date) {
            if (date == null) return "";
            
            Date now = new Date();
            long diff = now.getTime() - date.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                if (days == 1) return "Hôm qua";
                if (days < 7) return days + " ngày trước";
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return sdf.format(date);
            } else if (hours > 0) {
                return hours + " giờ trước";
            } else if (minutes > 0) {
                return minutes + " phút trước";
            } else {
                return "Vừa xong";
            }
        }
    }
}

