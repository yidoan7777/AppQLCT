package com.example.appqlct.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.appqlct.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * AdminReportFragment - Báo cáo tổng quan cho Admin
 * Bao gồm 2 tab: Báo cáo tổng và Báo cáo theo người dùng
 */
public class AdminReportFragment extends Fragment {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ReportPagerAdapter pagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Kiểm tra quyền admin trước khi hiển thị fragment
        if (!checkAdminPermission()) {
            return null; // Không hiển thị fragment nếu không phải admin
        }
        
        View view = inflater.inflate(R.layout.fragment_admin_report, container, false);

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
        
        com.example.appqlct.helper.SharedPreferencesHelper prefsHelper = 
                new com.example.appqlct.helper.SharedPreferencesHelper(getContext());
        String userRole = prefsHelper.getUserRole();
        
        if (!"admin".equals(userRole)) {
            // Không phải admin, quay về MainActivity
            if (getActivity() != null) {
                getActivity().onBackPressed();
                android.widget.Toast.makeText(getContext(), getString(R.string.no_permission_access), 
                        android.widget.Toast.LENGTH_SHORT).show();
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
        pagerAdapter = new ReportPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Báo cáo tổng");
            } else {
                tab.setText("Theo người dùng");
            }
        }).attach();
    }

    /**
     * Adapter cho ViewPager2
     */
    private static class ReportPagerAdapter extends FragmentStateAdapter {
        public ReportPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new ReportTotalFragment();
            } else {
                return new ReportByUserFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
