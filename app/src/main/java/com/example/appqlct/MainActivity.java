package com.example.appqlct;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.List;

import com.example.appqlct.fragment.admin.AdminReportFragment;
import com.example.appqlct.fragment.admin.AdminUserManageFragment;
import com.example.appqlct.fragment.admin.CategoryManageFragment;
import com.example.appqlct.fragment.admin.MaintenanceFragment;
import com.example.appqlct.fragment.user.BudgetFragment;
import com.example.appqlct.fragment.user.CategoryFragment;
import com.example.appqlct.fragment.user.DashboardFragment;
import com.example.appqlct.fragment.user.FeedbackFragment;
import com.example.appqlct.fragment.user.NotificationFragment;
import com.example.appqlct.fragment.user.ProfileFragment;
import com.example.appqlct.fragment.user.RecurringExpensesFragment;
import com.example.appqlct.fragment.user.ReportFragment;
import com.example.appqlct.fragment.user.TransactionListFragment;
import com.example.appqlct.helper.CategoryInitializer;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.helper.NotificationHelper;
import com.example.appqlct.helper.SharedPreferencesHelper;
import com.example.appqlct.model.Notification;
import com.example.appqlct.model.User;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

    /**
     * MainActivity - Activity chính chứa Navigation Drawer
     * Logic ẩn/hiện menu item dựa trên Role:
     * - User: Chỉ hiển thị menu dành cho User
     * - Admin: Chỉ hiển thị menu Admin (ẩn menu User)
     */
public class MainActivity extends AppCompatActivity 
        implements NavigationView.OnNavigationItemSelectedListener {
    
    private static final String TAG = "MainActivity";

    // UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private View headerView;
    private TextView tvUserName, tvUserEmail;
    private CircleImageView imgAvatar;

    // Helpers
    private SharedPreferencesHelper prefsHelper;
    private FirebaseHelper firebaseHelper;
    private FirebaseAuth auth;
    private CategoryInitializer categoryInitializer;
    private NotificationHelper notificationHelper;
    
    // User role
    private String userRole = "user";
    
    // Budget notification
    private boolean hasBudgetWarning = false;
    private boolean hasShown80PercentWarning = false; // Đánh dấu đã hiển thị thông báo 80% cho tháng này
    private double currentBudget = 0;
    private double currentExpense = 0;
    private double currentPercentage = 0;
    // Track last notification để tránh tạo thông báo trùng lặp
    private double lastNotifiedBudget = 0;
    private double lastNotifiedExpense = 0;
    private AlertDialog budgetDialog = null; // Track dialog để tránh hiển thị nhiều lần
    
    // Notification badge
    private MenuItem notificationMenuItem;
    private TextView badgeTextView;
    
    // BroadcastReceiver để lắng nghe thông báo mới
    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NotificationHelper.ACTION_NOTIFICATION_ADDED.equals(intent.getAction())) {
                updateNotificationBadge();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo
        initViews();
        initHelpers();
        setupToolbar();
        setupNavigationDrawer();
        
        // Kiểm tra đăng nhập
        checkAuthentication();
        
        // Load user info và setup menu dựa trên role
        loadUserInfoAndSetupMenu();
        
        // Tự động khởi tạo danh mục mặc định nếu chưa có (sau khi Firebase đã sẵn sàng)
        // Sử dụng post để đảm bảo Firebase đã được khởi tạo
        toolbar.post(() -> initializeDefaultCategoriesIfNeeded());
        
        // Load fragment mặc định
        loadDefaultFragment();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh notificationHelper để lấy userId mới (khi đăng nhập user khác)
        notificationHelper = new NotificationHelper(this);
        // Cập nhật badge thông báo
        updateNotificationBadge();
        
        // Đăng ký BroadcastReceiver để lắng nghe thông báo mới
        IntentFilter filter = new IntentFilter(NotificationHelper.ACTION_NOTIFICATION_ADDED);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(notificationReceiver, filter);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Hủy đăng ký BroadcastReceiver
        try {
            unregisterReceiver(notificationReceiver);
        } catch (Exception e) {
            // Ignore nếu receiver chưa được đăng ký
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Khởi tạo các view components
     */
    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        headerView = navigationView.getHeaderView(0);
        tvUserName = headerView.findViewById(R.id.tvUserName);
        tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
        imgAvatar = headerView.findViewById(R.id.imgAvatar);
    }

    /**
     * Khởi tạo các helper classes
     */
    private void initHelpers() {
        prefsHelper = new SharedPreferencesHelper(this);
        firebaseHelper = new FirebaseHelper();
        auth = FirebaseAuth.getInstance();
        categoryInitializer = new CategoryInitializer();
        notificationHelper = new NotificationHelper(this);
    }

    /**
     * Thiết lập Toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
    }
    

    /**
     * Thiết lập Navigation Drawer
     */
    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    /**
     * Kiểm tra xem user đã đăng nhập chưa
     */
    private void checkAuthentication() {
        if (!prefsHelper.isLoggedIn() || auth.getCurrentUser() == null) {
            // Chưa đăng nhập, chuyển đến LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Load thông tin user và thiết lập menu dựa trên role
     * Đây là phần quan trọng để phân quyền User/Admin
     */
    private void loadUserInfoAndSetupMenu() {
        userRole = prefsHelper.getUserRole();
        
        // Hiển thị thông tin user trong header
        tvUserName.setText(prefsHelper.getUserName());
        tvUserEmail.setText(prefsHelper.getUserEmail());
        
        // Load avatar từ Firestore
        loadUserAvatar();
        
        // Thiết lập menu dựa trên role
        setupMenuByRole();
    }
    
    /**
     * Load avatar từ Firestore và hiển thị trong navigation header
     */
    private void loadUserAvatar() {
        String userId = prefsHelper.getUserId();
        
        firebaseHelper.getUser(userId, new FirebaseHelper.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    // Load avatar từ URL bằng Picasso với timestamp để force reload
                    String avatarUrlWithTimestamp = user.getAvatarUrl() + "?t=" + System.currentTimeMillis();
                    Picasso.get()
                            .load(avatarUrlWithTimestamp)
                            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                            .networkPolicy(NetworkPolicy.NO_CACHE)
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .into(imgAvatar);
                } else {
                    // Hiển thị avatar mặc định
                    imgAvatar.setImageResource(R.drawable.user);
                }
            }

            @Override
            public void onError(String error) {
                // Nếu có lỗi, hiển thị avatar mặc định
                imgAvatar.setImageResource(R.drawable.user);
            }
        });
    }

    /**
     * Thiết lập menu navigation dựa trên role của user
     * - Nếu role == "admin": Chỉ hiển thị menu Admin (ẩn menu User)
     * - Nếu role == "user": Chỉ hiển thị menu User
     */
    private void setupMenuByRole() {
        Menu menu = navigationView.getMenu();
        boolean isAdmin = "admin".equals(userRole);
        
        // Menu User - chỉ hiển thị nếu KHÔNG phải admin
        menu.findItem(R.id.nav_dashboard).setVisible(!isAdmin);
        menu.findItem(R.id.nav_transactions).setVisible(!isAdmin);
        menu.findItem(R.id.nav_budget).setVisible(!isAdmin);
        menu.findItem(R.id.nav_report).setVisible(!isAdmin);
        menu.findItem(R.id.nav_profile).setVisible(!isAdmin);
        menu.findItem(R.id.nav_feedback).setVisible(!isAdmin);
        menu.findItem(R.id.nav_categories).setVisible(!isAdmin);
        menu.findItem(R.id.nav_recurring_expenses).setVisible(!isAdmin);
        notificationMenuItem = menu.findItem(R.id.nav_notifications);
        notificationMenuItem.setVisible(!isAdmin);
        
        // Menu Admin - chỉ hiển thị nếu là admin
        menu.findItem(R.id.nav_admin_users).setVisible(isAdmin);
        menu.findItem(R.id.nav_admin_categories).setVisible(isAdmin);
        menu.findItem(R.id.nav_admin_report).setVisible(isAdmin);
        menu.findItem(R.id.nav_maintenance).setVisible(isAdmin);
        
        // Nếu là admin, thay đổi màu toolbar để phân biệt
        if (isAdmin) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.admin_color));
            getWindow().setStatusBarColor(getResources().getColor(R.color.admin_color));
        }
        
        // Setup badge cho notification menu item (chỉ cho user)
        if (!isAdmin) {
            setupNotificationBadge();
        }
    }
    
    /**
     * Thiết lập badge cho menu item notification
     * Thêm badge vào view con của menu item mà không thay đổi cấu trúc
     */
    private void setupNotificationBadge() {
        if (notificationMenuItem == null) {
            return;
        }
        
        // Sử dụng post để đảm bảo menu đã được render
        navigationView.post(() -> {
            // Tìm View của menu item notification
            View menuItemView = navigationView.findViewById(R.id.nav_notifications);
            if (menuItemView != null && badgeTextView == null) {
                // Tìm view con chứa text và icon (thường là LinearLayout bên trong)
                ViewGroup menuItemContainer = findMenuItemContainer(menuItemView);
                
                if (menuItemContainer != null) {
                    // Tạo badge TextView
                    badgeTextView = new TextView(this);
                    badgeTextView.setId(View.generateViewId());
                    badgeTextView.setBackgroundResource(R.drawable.badge_background);
                    badgeTextView.setTextColor(getResources().getColor(android.R.color.white));
                    badgeTextView.setTextSize(10);
                    badgeTextView.setPadding(6, 2, 6, 2);
                    badgeTextView.setGravity(android.view.Gravity.CENTER);
                    badgeTextView.setMinWidth(24);
                    badgeTextView.setMinHeight(24);
                    badgeTextView.setClickable(false);
                    badgeTextView.setFocusable(false);
                    
                    // Thêm badge vào container
                    if (menuItemContainer instanceof android.widget.LinearLayout) {
                        android.widget.LinearLayout.LayoutParams badgeParams = 
                            new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                        badgeParams.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END;
                        badgeParams.setMargins(8, 0, 0, 0);
                        menuItemContainer.addView(badgeTextView, badgeParams);
                    } else {
                        // Fallback: thêm với layout params mặc định
                        ViewGroup.LayoutParams badgeParams = 
                            new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                        menuItemContainer.addView(badgeTextView, badgeParams);
                    }
                }
                
                // Cập nhật badge
                updateNotificationBadge();
            } else if (badgeTextView != null) {
                // Nếu badge đã tồn tại, chỉ cập nhật
                updateNotificationBadge();
            }
        });
    }
    
    /**
     * Tìm container view chứa text và icon của menu item
     */
    private ViewGroup findMenuItemContainer(View menuItemView) {
        if (menuItemView instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) menuItemView;
            // Tìm LinearLayout con chứa text và icon
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof android.widget.LinearLayout) {
                    return (android.widget.LinearLayout) child;
                }
            }
            // Nếu không tìm thấy LinearLayout, trả về chính container
            return container;
        }
        return null;
    }
    
    /**
     * Cập nhật badge thông báo với số lượng budget_warning notifications chưa đọc
     */
    private void updateNotificationBadge() {
        if (badgeTextView == null || notificationMenuItem == null || "admin".equals(userRole)) {
            return;
        }
        
        // Refresh notificationHelper để lấy userId mới
        notificationHelper = new NotificationHelper(this);
        int unreadCount = notificationHelper.getBudgetWarningUnreadCount();
        
        if (unreadCount > 0) {
            badgeTextView.setText(String.valueOf(unreadCount));
            badgeTextView.setVisibility(View.VISIBLE);
        } else {
            badgeTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Load fragment mặc định dựa trên role
     * - User: DashboardFragment
     * - Admin: AdminUserManageFragment
     */
    private void loadDefaultFragment() {
        Fragment defaultFragment;
        int titleResId;
        
        if ("admin".equals(userRole)) {
            // Admin: Load fragment quản lý người dùng mặc định
            defaultFragment = new AdminUserManageFragment();
            titleResId = R.string.nav_admin_users;
        } else {
            // User: Load DashboardFragment
            defaultFragment = new DashboardFragment();
            titleResId = R.string.nav_dashboard;
            
            // Set callback để nhận thông báo từ DashboardFragment
            if (defaultFragment instanceof DashboardFragment) {
                ((DashboardFragment) defaultFragment).setBudgetWarningListener(
                    new DashboardFragment.BudgetWarningListener() {
                        @Override
                        public void onBudgetWarning(double budget, double expense, double percentage) {
                            // Cập nhật trạng thái hiển thị
                            hasBudgetWarning = true;
                            currentBudget = budget;
                            currentExpense = expense;
                            currentPercentage = percentage;
                            
                            // Kiểm tra flag trước để tránh tạo nhiều notification khi callback được gọi nhiều lần liên tiếp
                            if (hasShown80PercentWarning) {
                                return; // Đã tạo notification rồi, không tạo lại
                            }
                            
                            // Kiểm tra xem đã có notification chưa đọc cho tháng này chưa
                            // Nếu đã có notification chưa đọc, không tạo mới
                            String userId = prefsHelper.getUserId();
                            List<Notification> notifications = notificationHelper.getNotifications();
                            Calendar currentMonth = Calendar.getInstance();
                            int currentYear = currentMonth.get(Calendar.YEAR);
                            int currentMonthValue = currentMonth.get(Calendar.MONTH);
                            
                            boolean hasUnreadNotificationThisMonth = false;
                            for (Notification notif : notifications) {
                                if ("budget_warning".equals(notif.getType()) && 
                                    userId.equals(notif.getUserId()) && 
                                    !notif.isRead() &&
                                    notif.getCreatedAt() != null) {
                                    
                                    Calendar notifMonth = Calendar.getInstance();
                                    notifMonth.setTime(notif.getCreatedAt());
                                    int notifYear = notifMonth.get(Calendar.YEAR);
                                    int notifMonthValue = notifMonth.get(Calendar.MONTH);
                                    
                                    // Nếu notification được tạo trong cùng tháng và chưa đọc, đã có rồi
                                    if (notifYear == currentYear && notifMonthValue == currentMonthValue) {
                                        hasUnreadNotificationThisMonth = true;
                                        break;
                                    }
                                }
                            }
                            
                            // Chỉ tạo notification nếu chưa có notification chưa đọc cho tháng này
                            if (!hasUnreadNotificationThisMonth) {
                                hasShown80PercentWarning = true; // Đánh dấu đã hiển thị TRƯỚC KHI tạo notification
                                lastNotifiedBudget = budget;
                                lastNotifiedExpense = expense;
                                // Tạo thông báo mới
                                createBudgetNotification(budget, expense, percentage);
                                // Cập nhật badge
                                updateNotificationBadge();
                            } else {
                                // Đã có notification chưa đọc rồi, chỉ cập nhật flag
                                hasShown80PercentWarning = true;
                            }
                        }
                        
                        @Override
                        public void onBudgetWarningCleared() {
                            // Xóa tất cả notification budget_warning chưa đọc của tháng này
                            // Để khi chi tiêu tăng lên 80% lại, có thể tạo notification mới
                            if (notificationHelper == null) {
                                notificationHelper = new NotificationHelper(MainActivity.this);
                            }
                            notificationHelper.deleteUnreadBudgetWarningsThisMonth();
                            // Cập nhật badge
                            updateNotificationBadge();
                            resetBudgetWarning();
                        }
                    }
                );
            }
        }
        
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, defaultFragment)
                .commit();
        
        // Set title
        getSupportActionBar().setTitle(titleResId);
    }

    /**
     * Xử lý khi click vào item trong Navigation Drawer
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment selectedFragment = null;
        String title = "";

        // Xác định fragment nào cần load dựa trên menu item
        // Kiểm tra quyền truy cập: Admin không thể truy cập menu User
        boolean isAdmin = "admin".equals(userRole);
        
        if (id == R.id.nav_dashboard) {
            if (isAdmin) return false; // Admin không được truy cập
            selectedFragment = new DashboardFragment();
            title = getString(R.string.nav_dashboard);
        } else if (id == R.id.nav_transactions) {
            if (isAdmin) return false; // Admin không được truy cập
            selectedFragment = new TransactionListFragment();
            title = getString(R.string.nav_transactions);
        } else if (id == R.id.nav_budget) {
            if (isAdmin) return false; // Admin không được truy cập
            selectedFragment = new BudgetFragment();
            title = getString(R.string.nav_budget);
        } else if (id == R.id.nav_report) {
            if (isAdmin) return false; // Admin không được truy cập
            selectedFragment = new ReportFragment();
            title = getString(R.string.nav_report);
        } else if (id == R.id.nav_profile) {
            if (isAdmin) return false; // Admin không được truy cập
            selectedFragment = new ProfileFragment();
            title = getString(R.string.nav_profile);
        } else if (id == R.id.nav_feedback) {
            if (isAdmin) return false; // Admin không được truy cập
            selectedFragment = new FeedbackFragment();
            title = getString(R.string.nav_feedback);
        } else if (id == R.id.nav_categories) {
            if (isAdmin) return false; // Admin không được truy cập
            selectedFragment = new CategoryFragment();
            title = getString(R.string.nav_categories);
        } else if (id == R.id.nav_recurring_expenses) {
            if (isAdmin) return false; // Admin không được truy cập
            selectedFragment = new RecurringExpensesFragment();
            title = getString(R.string.nav_recurring_expenses);
        } else if (id == R.id.nav_notifications) {
            if (isAdmin) return false; // Admin không được truy cập
            // Đánh dấu tất cả thông báo budget_warning là đã đọc khi click vào menu
            if (notificationHelper == null) {
                notificationHelper = new NotificationHelper(this);
            }
            notificationHelper.markAllBudgetWarningsAsRead();
            // Cập nhật badge ngay lập tức
            updateNotificationBadge();
            selectedFragment = new NotificationFragment();
            title = getString(R.string.notification);
        } else if (id == R.id.nav_admin_users) {
            if (!isAdmin) return false; // Chỉ admin mới được truy cập
            selectedFragment = new AdminUserManageFragment();
            title = getString(R.string.nav_admin_users);
        } else if (id == R.id.nav_admin_categories) {
            if (!isAdmin) return false; // Chỉ admin mới được truy cập
            selectedFragment = new CategoryManageFragment();
            title = getString(R.string.nav_admin_categories);
        } else if (id == R.id.nav_admin_report) {
            if (!isAdmin) return false; // Chỉ admin mới được truy cập
            selectedFragment = new AdminReportFragment();
            title = getString(R.string.nav_admin_report);
        } else if (id == R.id.nav_maintenance) {
            if (!isAdmin) return false; // Chỉ admin mới được truy cập
            selectedFragment = new MaintenanceFragment();
            title = getString(R.string.nav_maintenance);
        } else if (id == R.id.nav_logout) {
            handleLogout();
            return true;
        }

        // Load fragment được chọn
        if (selectedFragment != null) {
            // Reset UI cảnh báo khi chuyển fragment (trừ khi chuyển đến DashboardFragment)
            // KHÔNG reset hasShown80PercentWarning để tránh tạo notification trùng
            if (!(selectedFragment instanceof DashboardFragment)) {
                resetBudgetWarningUI();
            }
            
            // Set callback cho DashboardFragment nếu là user
            if (selectedFragment instanceof DashboardFragment && !"admin".equals(userRole)) {
                ((DashboardFragment) selectedFragment).setBudgetWarningListener(
                    new DashboardFragment.BudgetWarningListener() {
                        @Override
                        public void onBudgetWarning(double budget, double expense, double percentage) {
                            // Cập nhật trạng thái hiển thị
                            hasBudgetWarning = true;
                            currentBudget = budget;
                            currentExpense = expense;
                            currentPercentage = percentage;
                            
                            // Kiểm tra flag trước để tránh tạo nhiều notification khi callback được gọi nhiều lần liên tiếp
                            if (hasShown80PercentWarning) {
                                return; // Đã tạo notification rồi, không tạo lại
                            }
                            
                            // Kiểm tra xem đã có notification chưa đọc cho tháng này chưa
                            // Nếu đã có notification chưa đọc, không tạo mới
                            String userId = prefsHelper.getUserId();
                            List<Notification> notifications = notificationHelper.getNotifications();
                            Calendar currentMonth = Calendar.getInstance();
                            int currentYear = currentMonth.get(Calendar.YEAR);
                            int currentMonthValue = currentMonth.get(Calendar.MONTH);
                            
                            boolean hasUnreadNotificationThisMonth = false;
                            for (Notification notif : notifications) {
                                if ("budget_warning".equals(notif.getType()) && 
                                    userId.equals(notif.getUserId()) && 
                                    !notif.isRead() &&
                                    notif.getCreatedAt() != null) {
                                    
                                    Calendar notifMonth = Calendar.getInstance();
                                    notifMonth.setTime(notif.getCreatedAt());
                                    int notifYear = notifMonth.get(Calendar.YEAR);
                                    int notifMonthValue = notifMonth.get(Calendar.MONTH);
                                    
                                    // Nếu notification được tạo trong cùng tháng và chưa đọc, đã có rồi
                                    if (notifYear == currentYear && notifMonthValue == currentMonthValue) {
                                        hasUnreadNotificationThisMonth = true;
                                        break;
                                    }
                                }
                            }
                            
                            // Chỉ tạo notification nếu chưa có notification chưa đọc cho tháng này
                            if (!hasUnreadNotificationThisMonth) {
                                hasShown80PercentWarning = true; // Đánh dấu đã hiển thị TRƯỚC KHI tạo notification
                                lastNotifiedBudget = budget;
                                lastNotifiedExpense = expense;
                                // Tạo thông báo mới
                                createBudgetNotification(budget, expense, percentage);
                                // Cập nhật badge
                                updateNotificationBadge();
                            } else {
                                // Đã có notification chưa đọc rồi, chỉ cập nhật flag
                                hasShown80PercentWarning = true;
                            }
                        }
                        
                        @Override
                        public void onBudgetWarningCleared() {
                            // Xóa tất cả notification budget_warning chưa đọc của tháng này
                            // Để khi chi tiêu tăng lên 80% lại, có thể tạo notification mới
                            if (notificationHelper == null) {
                                notificationHelper = new NotificationHelper(MainActivity.this);
                            }
                            notificationHelper.deleteUnreadBudgetWarningsThisMonth();
                            // Cập nhật badge
                            updateNotificationBadge();
                            resetBudgetWarning();
                        }
                    }
                );
            }
            
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            getSupportActionBar().setTitle(title);
        }

        // Đóng drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Tự động khởi tạo danh mục mặc định nếu chưa có
     * Chạy ngầm khi app khởi động để đảm bảo luôn có danh mục cho người dùng
     */
    private void initializeDefaultCategoriesIfNeeded() {
        categoryInitializer.initializeDefaultCategories(new CategoryInitializer.OnInitializationCompleteListener() {
            @Override
            public void onSuccess(String message) {
                // Khởi tạo thành công hoặc đã có sẵn danh mục
                // Không cần hiển thị thông báo để không làm phiền người dùng
            }

            @Override
            public void onError(String error) {
                // Lỗi khi khởi tạo, nhưng không chặn app
                // Có thể log lỗi nếu cần
            }
        });
    }

    /**
     * Xử lý đăng xuất
     */
    private void handleLogout() {
        // Đăng xuất khỏi Firebase
        auth.signOut();
        
        // Xóa session
        prefsHelper.clearSession();
        
        // Chuyển đến LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Hiển thị dialog thông báo ngân sách chi tiết
     */
    private void showBudgetNotificationDialog() {
        // Kiểm tra xem dialog đã hiển thị chưa, nếu có thì đóng dialog cũ
        if (budgetDialog != null && budgetDialog.isShowing()) {
            budgetDialog.dismiss();
        }
        
        // Inflate custom layout
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_budget_notification, null);
        
        // Get views
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        android.widget.TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        android.widget.Button btnUnderstand = dialogView.findViewById(R.id.btnUnderstand);
        
        // Format message
        String message = String.format(java.util.Locale.getDefault(),
                getString(R.string.budget_warning_detailed),
                currentPercentage,
                currentBudget,
                currentExpense,
                currentBudget - currentExpense);
        
        tvMessage.setText(message);
        
        // Create dialog
        budgetDialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        // Set button click listener
        btnUnderstand.setOnClickListener(v -> {
            budgetDialog.dismiss();
            budgetDialog = null;
        });
        
        // Set dismiss listener để reset reference
        budgetDialog.setOnDismissListener(dialog -> budgetDialog = null);
        
        // Show dialog
        budgetDialog.show();
        
        // Customize dialog window
        android.view.Window window = budgetDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
    
    /**
     * Tạo thông báo ngân sách mới
     * Chỉ tạo 1 lần duy nhất cho tháng hiện tại để tránh spam
     */
    private void createBudgetNotification(double budget, double expense, double percentage) {
        String userId = prefsHelper.getUserId();
        
        // Kiểm tra xem đã có notification budget_warning chưa đọc cho tháng này chưa
        // Nếu đã có notification chưa đọc, không tạo mới
        List<Notification> notifications = notificationHelper.getNotifications();
        Calendar currentMonth = Calendar.getInstance();
        int currentYear = currentMonth.get(Calendar.YEAR);
        int currentMonthValue = currentMonth.get(Calendar.MONTH);
        
        for (Notification notif : notifications) {
            if ("budget_warning".equals(notif.getType()) && 
                userId.equals(notif.getUserId()) && 
                !notif.isRead() &&
                notif.getCreatedAt() != null) {
                
                Calendar notifMonth = Calendar.getInstance();
                notifMonth.setTime(notif.getCreatedAt());
                int notifYear = notifMonth.get(Calendar.YEAR);
                int notifMonthValue = notifMonth.get(Calendar.MONTH);
                
                // Nếu notification được tạo trong cùng tháng và chưa đọc, không tạo mới
                if (notifYear == currentYear && notifMonthValue == currentMonthValue) {
                    return; // Đã có notification chưa đọc cho tháng này rồi, không tạo mới
                }
            }
        }
        
        // Chưa có notification cho tháng này, tạo mới
        String title = getString(R.string.budget_warning_title);
        String message = String.format(java.util.Locale.getDefault(),
                getString(R.string.budget_warning_notification),
                percentage, budget, expense, budget - expense);
        
        Notification notification = new Notification(
                null, userId, title, message, "budget_warning"
        );
        
        notificationHelper.addNotification(notification);
    }
    
    /**
     * Public method để fragment có thể reset trạng thái cảnh báo
     * Reset hoàn toàn khi chi tiêu giảm xuống dưới 80%
     */
    public void resetBudgetWarning() {
        hasBudgetWarning = false;
        hasShown80PercentWarning = false; // Reset flag khi chi tiêu giảm xuống dưới 80%
        currentBudget = 0;
        currentExpense = 0;
        currentPercentage = 0;
        lastNotifiedBudget = 0;
        lastNotifiedExpense = 0;
        // Đóng dialog nếu đang hiển thị
        if (budgetDialog != null && budgetDialog.isShowing()) {
            budgetDialog.dismiss();
            budgetDialog = null;
        }
    }
    
    /**
     * Reset chỉ UI state, không reset hasShown80PercentWarning
     * Dùng khi chuyển fragment để tránh tạo notification trùng
     */
    private void resetBudgetWarningUI() {
        hasBudgetWarning = false;
        // KHÔNG reset hasShown80PercentWarning ở đây
        currentBudget = 0;
        currentExpense = 0;
        currentPercentage = 0;
        // Đóng dialog nếu đang hiển thị
        if (budgetDialog != null && budgetDialog.isShowing()) {
            budgetDialog.dismiss();
            budgetDialog = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}

