package com.example.appqlct.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.model.User;

import java.util.List;

/**
 * UserAdapter - Adapter cho RecyclerView hiển thị danh sách users (Admin)
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private List<User> users;
    private OnUserClickListener listener;
    private OnUserEditListener editListener;
    private OnUserDeleteListener deleteListener;
    private String currentUserId; // UID của admin hiện tại để không cho xóa chính mình

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public interface OnUserEditListener {
        void onUserEdit(User user);
    }

    public interface OnUserDeleteListener {
        void onUserDelete(User user);
    }

    public UserAdapter(List<User> users, OnUserClickListener listener, 
                      OnUserEditListener editListener, OnUserDeleteListener deleteListener, String currentUserId) {
        this.users = users;
        this.listener = listener;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvEmail, tvRole;
        private ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(users.get(getAdapterPosition()));
                }
            });

            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (editListener != null) {
                        editListener.onUserEdit(users.get(getAdapterPosition()));
                    }
                });
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onUserDelete(users.get(getAdapterPosition()));
                    }
                });
            }
        }

        void bind(User user) {
            tvName.setText(user.getName());
            tvEmail.setText(user.getEmail());
            String roleText = user.getRole().equals("admin") ? itemView.getContext().getString(R.string.admin) : itemView.getContext().getString(R.string.user);
            tvRole.setText(roleText);
            
            // Ẩn nút xóa nếu là chính admin hiện tại
            if (btnDelete != null) {
                btnDelete.setVisibility(user.getUid().equals(currentUserId) ? View.GONE : View.VISIBLE);
            }
        }
    }
}

