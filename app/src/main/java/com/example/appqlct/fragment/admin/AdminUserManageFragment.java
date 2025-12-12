package com.example.appqlct.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.appqlct.R;
import com.example.appqlct.helper.SharedPreferencesHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * AdminUserManageFragment - Quản lý danh sách người dùng (chỉ Admin)
 * Chia thành 2 tab: User và Admin
 */
public class AdminUserManageFragment extends Fragment {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private UserPagerAdapter pagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Kiểm tra quyền admin trước khi hiển thị fragment
        if (!checkAdminPermission()) {
            return null; // Không hiển thị fragment nếu không phải admin
        }
        
        View view = inflater.inflate(R.layout.fragment_admin_user_manage, container, false);

        initViews(view);
        setupViewPager();

        return view;
    }
    
    /**
     * Kiểm tra quyền admin
     * @return true nếu là admin, false nếu không phải
     */
    private boolean checkAdminPermission() {
        if (getContext() == null) return false;
        
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
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
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
    }

    private void setupViewPager() {
        pagerAdapter = new UserPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(getString(R.string.user));
            } else {
                tab.setText(getString(R.string.admin));
            }
        }).attach();
    }

    /**
     * Adapter cho ViewPager2
     */
    private static class UserPagerAdapter extends FragmentStateAdapter {
        public UserPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return UserListFragment.newInstance("user");
            } else {
                return UserListFragment.newInstance("admin");
            }
        }

        @Override
        public int getItemCount() {
            return 2; // 2 tab: User và Admin
        }
    }
}

