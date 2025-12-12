package com.example.appqlct.fragment.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.adapter.CategoryAdapter;
import com.example.appqlct.helper.CategoryInitializer;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.model.Category;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * CategoryManageFragment - Quản lý danh mục (chỉ Admin)
 * Cho phép thêm, sửa, xóa categories với type (income/expense) và icon
 */
public class CategoryManageFragment extends Fragment {
    private RecyclerView recyclerView;
    private EditText etCategoryName;
    private Button btnAdd;
    private Button btnInitDefault;
    private CategoryAdapter adapter;
    private List<Category> categoryList;
    private FirebaseHelper firebaseHelper;
    private CategoryInitializer categoryInitializer;
    private Category editingCategory; // Category đang được chỉnh sửa

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Kiểm tra quyền admin trước khi hiển thị fragment
        if (!checkAdminPermission()) {
            return null; // Không hiển thị fragment nếu không phải admin
        }
        
        View view = inflater.inflate(R.layout.fragment_category_manage, container, false);

        initViews(view);
        initHelpers();
        setupRecyclerView();
        loadCategories();

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
        recyclerView = view.findViewById(R.id.recyclerView);
        etCategoryName = view.findViewById(R.id.etCategoryName);
        btnAdd = view.findViewById(R.id.btnAdd);
        btnInitDefault = view.findViewById(R.id.btnInitDefault);

        btnAdd.setOnClickListener(v -> {
            if (editingCategory != null) {
                updateCategory();
            } else {
                addCategory();
            }
        });

        btnInitDefault.setOnClickListener(v -> initializeDefaultCategories());
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        categoryInitializer = new CategoryInitializer();
        categoryList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new CategoryAdapter(categoryList, category -> {
            // Xử lý khi click vào category để chỉnh sửa
            editCategory(category);
        }, category -> {
            // Xử lý khi xóa category
            deleteCategory(category);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Load danh sách categories từ Firestore
     */
    private void loadCategories() {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                if (!isAdded() || getContext() == null) return;
                categoryList.clear();
                categoryList.addAll(categories);
                adapter.notifyDataSetChanged();
                
                // Nếu không có danh mục nào, tự động khởi tạo danh mục mặc định
                if (categories.isEmpty()) {
                    autoInitializeDefaultCategories();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), getString(R.string.error) + ": " + error, 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Tự động khởi tạo danh mục mặc định khi collection đang trống
     */
    private void autoInitializeDefaultCategories() {
        if (!isAdded() || getContext() == null) return;
        
        categoryInitializer.initializeDefaultCategories(new CategoryInitializer.OnInitializationCompleteListener() {
            @Override
            public void onSuccess(String message) {
                if (!isAdded() || getContext() == null) return;
                // Reload danh sách categories sau khi khởi tạo
                loadCategories();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Không hiển thị lỗi khi tự động khởi tạo, chỉ log
            }
        });
    }

    /**
     * Thêm category mới
     */
    private void addCategory() {
        String name = etCategoryName.getText().toString().trim();
        
        if (name.isEmpty()) {
            etCategoryName.setError(getString(R.string.required_field));
            return;
        }

        // Luôn set type là "expense" vì chỉ có chi tiêu
        Category category = new Category(null, name, "", "expense");
        firebaseHelper.addCategory(category, task -> {
            if (!isAdded() || getContext() == null) return;
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), getString(R.string.add_category_success), Toast.LENGTH_SHORT).show();
                clearForm();
                loadCategories();
            } else {
                Toast.makeText(getContext(), getString(R.string.error) + ": " + 
                        (task.getException() != null ? task.getException().getMessage() : ""), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Chỉnh sửa category
     */
    private void editCategory(Category category) {
        editingCategory = category;
        etCategoryName.setText(category.getName());
        btnAdd.setText(getString(R.string.save));
    }

    /**
     * Cập nhật category
     */
    private void updateCategory() {
        if (editingCategory == null) {
            return;
        }

        String name = etCategoryName.getText().toString().trim();
        
        if (name.isEmpty()) {
            etCategoryName.setError(getString(R.string.required_field));
            return;
        }

        editingCategory.setName(name);
        // Luôn giữ type là "expense" vì chỉ có chi tiêu
        editingCategory.setType("expense");
        
        firebaseHelper.updateCategory(editingCategory, task -> {
            if (!isAdded() || getContext() == null) return;
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), getString(R.string.update_category_success), Toast.LENGTH_SHORT).show();
                clearForm();
                loadCategories();
            } else {
                Toast.makeText(getContext(), getString(R.string.error) + ": " + 
                        (task.getException() != null ? task.getException().getMessage() : ""), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Xóa category
     */
    private void deleteCategory(Category category) {
        if (!isAdded() || getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.delete))
                .setMessage(getString(R.string.delete_category_confirm, category.getName()))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    firebaseHelper.deleteCategory(category.getId(), task -> {
                        if (!isAdded() || getContext() == null) return;
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), getString(R.string.delete_category_success), 
                                    Toast.LENGTH_SHORT).show();
                            loadCategories();
                        } else {
                            Toast.makeText(getContext(), getString(R.string.error) + ": " + 
                                    (task.getException() != null ? task.getException().getMessage() : ""), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    /**
     * Xóa form và reset về trạng thái thêm mới
     */
    private void clearForm() {
        etCategoryName.setText("");
        editingCategory = null;
        btnAdd.setText(getString(R.string.add_category));
    }

    /**
     * Khởi tạo các danh mục mặc định
     */
    private void initializeDefaultCategories() {
        if (!isAdded() || getContext() == null) return;
        
        // Hiển thị dialog xác nhận
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.initialize_default_categories_title))
                .setMessage(getString(R.string.initialize_default_categories_message))
                .setPositiveButton(getString(R.string.agree), (dialog, which) -> {
                    // Disable button để tránh click nhiều lần
                    btnInitDefault.setEnabled(false);
                    btnInitDefault.setText(getString(R.string.initializing));
                    
                    categoryInitializer.initializeDefaultCategories(new CategoryInitializer.OnInitializationCompleteListener() {
                        @Override
                        public void onSuccess(String message) {
                            if (!isAdded() || getContext() == null) return;
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            btnInitDefault.setEnabled(true);
                            btnInitDefault.setText(getString(R.string.initialize_default_categories));
                            // Reload danh sách categories
                            loadCategories();
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded() || getContext() == null) return;
                            Toast.makeText(getContext(), getString(R.string.error_occurred, error), Toast.LENGTH_SHORT).show();
                            btnInitDefault.setEnabled(true);
                            btnInitDefault.setText(getString(R.string.initialize_default_categories));
                        }
                    });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

}
