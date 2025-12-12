package com.example.appqlct.fragment.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.adapter.UserAdapter;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * UserListFragment - Fragment hiển thị danh sách User hoặc Admin
 * Được sử dụng trong TabLayout của AdminUserManageFragment
 */
public class UserListFragment extends Fragment {
    private static final String ARG_ROLE = "role";
    
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private UserAdapter adapter;
    private List<User> userList;
    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private String filterRole; // "user" hoặc "admin"

    /**
     * Tạo instance mới của fragment với role filter
     * @param role "user" hoặc "admin"
     * @return UserListFragment instance
     */
    public static UserListFragment newInstance(String role) {
        UserListFragment fragment = new UserListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROLE, role);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filterRole = getArguments().getString(ARG_ROLE, "user");
        } else {
            filterRole = "user";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        initViews(view);
        initHelpers();
        setupRecyclerView();
        loadUsers();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload danh sách khi quay lại tab để đảm bảo dữ liệu được cập nhật
        if (recyclerView != null) {
            loadUsers();
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmpty = view.findViewById(R.id.tvEmpty);
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        userList = new ArrayList<>();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(
            userList, 
            this::showUserDetails,
            this::showEditUserDialog,
            this::showDeleteUserDialog,
            currentUserId
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Public method để refresh danh sách từ fragment cha
     */
    public void refreshUsers() {
        loadUsers();
    }

    /**
     * Load danh sách users theo role filter
     */
    private void loadUsers() {
        firebaseHelper.getAllUsers(new FirebaseHelper.OnUsersLoadedListener() {
            @Override
            public void onUsersLoaded(List<User> users) {
                if (!isAdded() || getContext() == null) return;
                userList.clear();
                // Lọc theo role
                for (User user : users) {
                    if (user.getRole() != null && user.getRole().equals(filterRole)) {
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
                
                if (userList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                    Toast.makeText(getContext(), getString(R.string.error_occurred, error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Hiển thị chi tiết user
     */
    private void showUserDetails(User user) {
        if (!isAdded() || getContext() == null) return;
        
        String details = getString(R.string.name_label, user.getName()) +
                        getString(R.string.email_label, user.getEmail()) +
                        getString(R.string.role, user.getRole().equals("admin") ? getString(R.string.admin) : getString(R.string.user)) +
                        getString(R.string.budget_limit_label, String.format(Locale.getDefault(), "%,.0f VND", user.getBudgetLimit())) +
                        (user.getPhone() != null && !user.getPhone().isEmpty() ? getString(R.string.phone_label, user.getPhone()) : "") +
                        (user.getGender() != null && !user.getGender().isEmpty() ? getString(R.string.gender_label, user.getGender()) : "") +
                        (user.getDateOfBirth() != null && !user.getDateOfBirth().isEmpty() ? getString(R.string.date_of_birth) + ": " + user.getDateOfBirth() : "");
        
        new AlertDialog.Builder(getContext())
                .setTitle("Chi tiết người dùng")
                .setMessage(details)
                .setPositiveButton("Đóng", null)
                .setNeutralButton("Chỉnh sửa", (dialog, which) -> showEditUserDialog(user))
                .show();
    }

    /**
     * Hiển thị dialog chỉnh sửa user
     */
    private void showEditUserDialog(User user) {
        if (!isAdded() || getContext() == null) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_user, null);
        EditText etName = dialogView.findViewById(R.id.etName);
        Spinner spinnerRole = dialogView.findViewById(R.id.spinnerRole);
        TextView tvEmail = dialogView.findViewById(R.id.tvEmail);
        TextView tvPhone = dialogView.findViewById(R.id.tvPhone);
        TextView tvGender = dialogView.findViewById(R.id.tvGender);
        TextView tvDateOfBirth = dialogView.findViewById(R.id.tvDateOfBirth);
        
        // Hiển thị thông tin đầy đủ
        etName.setText(user.getName());
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : getString(R.string.not_updated));
        
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            tvPhone.setText(user.getPhone());
        } else {
            tvPhone.setText("Chưa cập nhật");
        }
        
        if (user.getGender() != null && !user.getGender().isEmpty()) {
            tvGender.setText(user.getGender());
        } else {
            tvGender.setText("Chưa cập nhật");
        }
        
        if (user.getDateOfBirth() != null && !user.getDateOfBirth().isEmpty()) {
            tvDateOfBirth.setText(user.getDateOfBirth());
        } else {
            tvDateOfBirth.setText("Chưa cập nhật");
        }
        
        // Setup spinner role
        List<String> roles = new ArrayList<>();
        roles.add("user");
        roles.add("admin");
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);
        int roleIndex = roles.indexOf(user.getRole());
        if (roleIndex >= 0) {
            spinnerRole.setSelection(roleIndex);
        }
        
        new AlertDialog.Builder(getContext())
                .setTitle("Chỉnh sửa người dùng")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String role = spinnerRole.getSelectedItem().toString();
                    
                    if (TextUtils.isEmpty(name)) {
                        etName.setError("Vui lòng nhập tên");
                        return;
                    }
                    
                    // Không cho phép admin tự đổi role của mình thành user
                    if (user.getUid().equals(currentUserId) && !role.equals("admin")) {
                        Toast.makeText(getContext(), "Bạn không thể tự đổi vai trò của mình", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    updateUser(user, name, role);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Cập nhật thông tin user
     */
    private void updateUser(User user, String name, String role) {
        String oldRole = user.getRole();
        user.setName(name);
        user.setRole(role);
        
        firebaseHelper.saveUser(user, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!isAdded() || getContext() == null) return;
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    loadUsers(); // Reload danh sách
                    
                    // Nếu role thay đổi, user sẽ biến mất khỏi tab hiện tại
                    // và xuất hiện ở tab kia khi user chuyển tab
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + 
                            (task.getException() != null ? task.getException().getMessage() : ""), 
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Hiển thị dialog xác nhận xóa user
     */
    private void showDeleteUserDialog(User user) {
        if (!isAdded() || getContext() == null) return;
        
        // Không cho phép xóa chính mình
        if (user.getUid().equals(currentUserId)) {
            Toast.makeText(getContext(), "Bạn không thể xóa chính mình", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.delete_user_title))
                .setMessage(getString(R.string.delete_user) + ": \"" + user.getName() + "\"?\n\n" +
                           "This action cannot be undone!")
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteUser(user))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    /**
     * Xóa user
     */
    private void deleteUser(User user) {
        firebaseHelper.deleteUser(user.getUid(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!isAdded() || getContext() == null) return;
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), getString(R.string.delete_user_success), Toast.LENGTH_SHORT).show();
                    loadUsers(); // Reload danh sách
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + 
                            (task.getException() != null ? task.getException().getMessage() : ""), 
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

