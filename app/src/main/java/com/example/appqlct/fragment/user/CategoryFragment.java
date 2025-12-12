package com.example.appqlct.fragment.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appqlct.R;
import com.example.appqlct.adapter.CategoryViewAdapter;
import com.example.appqlct.helper.CategoryInitializer;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.model.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * CategoryFragment - Hiển thị danh sách danh mục cho người dùng
 * Cho phép xem tất cả categories hoặc filter theo type (income/expense)
 */
public class CategoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private Spinner spinnerFilter;
    private CategoryViewAdapter adapter;
    private List<Category> allCategories;
    private List<Category> filteredCategories;
    private FirebaseHelper firebaseHelper;
    private CategoryInitializer categoryInitializer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        initViews(view);
        initHelpers();
        setupRecyclerView();
        setupFilter();
        loadCategories();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        spinnerFilter = view.findViewById(R.id.spinnerFilter);
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        categoryInitializer = new CategoryInitializer();
        allCategories = new ArrayList<>();
        filteredCategories = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new CategoryViewAdapter(filteredCategories);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(adapter);
        
        // Thêm click listener để mở TransactionListFragment với category được chọn
        adapter.setOnCategoryClickListener(category -> {
            openTransactionListForCategory(category);
        });
    }

    private void setupFilter() {
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add(getString(R.string.all));
        filterOptions.add(getString(R.string.expense));

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, filterOptions);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterCategories(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    /**
     * Load danh sách categories từ Firestore
     * Nếu không có danh mục nào, tự động khởi tạo danh mục mặc định
     */
    private void loadCategories() {
        firebaseHelper.getAllCategories(new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                if (!isAdded() || getContext() == null) return;
                
                // Nếu không có danh mục nào, tự động khởi tạo danh mục mặc định
                if (categories.isEmpty()) {
                    initializeDefaultCategories();
                    return;
                }
                
                allCategories.clear();
                allCategories.addAll(categories);
                filterCategories(spinnerFilter.getSelectedItemPosition());
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
     * Tự động khởi tạo danh mục mặc định khi không có danh mục nào
     */
    private void initializeDefaultCategories() {
        if (!isAdded() || getContext() == null) return;
        
        categoryInitializer.initializeDefaultCategories(new CategoryInitializer.OnInitializationCompleteListener() {
            @Override
            public void onSuccess(String message) {
                if (!isAdded() || getContext() == null) return;
                // Sau khi khởi tạo thành công, reload lại danh sách
                loadCategories();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), getString(R.string.error_initializing_categories, error),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Filter categories theo type được chọn
     */
    private void filterCategories(int filterPosition) {
        filteredCategories.clear();

        // Hiển thị tất cả danh mục, không lọc theo type
        filteredCategories.addAll(allCategories);

        adapter.notifyDataSetChanged();
    }
    
    /**
     * Mở CategoryTransactionFragment với category được chọn
     * Chỉ mở cho expense categories
     */
    private void openTransactionListForCategory(Category category) {
        // Chỉ cho phép xem giao dịch của expense categories
        if (category == null || !"expense".equals(category.getType())) {
            Toast.makeText(requireContext(), getString(R.string.can_only_view_expense_category_transactions), 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo CategoryTransactionFragment với category ID
        CategoryTransactionFragment fragment = CategoryTransactionFragment.newInstance(category.getId(), category.getName());
        
        // Replace fragment hiện tại
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null) // Cho phép quay lại bằng nút back
                    .commit();
            
            // Cập nhật title của ActionBar
            if (getActivity() instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setTitle(category.getName());
                }
            }
        }
    }
}

