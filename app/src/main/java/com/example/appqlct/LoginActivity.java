package com.example.appqlct;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.helper.NetworkHelper;
import com.example.appqlct.helper.SharedPreferencesHelper;
import com.example.appqlct.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * LoginActivity - Xử lý đăng nhập và đăng ký
 * Sau khi đăng nhập thành công, kiểm tra role trong Firestore:
 * - Nếu role == "admin" -> Chuyển đến giao diện Admin
 * - Nếu role == "user" -> Chuyển đến giao diện User
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    // UI Components
    private EditText etEmail, etPassword, etName, etDateOfBirth, etConfirmPassword;
    private TextInputLayout tilName, tilDateOfBirth, tilConfirmPassword;
    private LinearLayout tilGender;
    private Button btnLogin, btnRegister, btnGoogleSignIn;
    private TextView tvSwitchMode, tvSwitchModeRegister, tvForgotPassword, tvWelcome;
    private View switchModeLayout, switchModeLayoutRegister;
    private ProgressBar progressBar;
    private View dividerLayout, socialButtonsLayout;
    private MaterialCardView cardGenderMale, cardGenderFemale;
    private String selectedGender = "";

    // Firebase
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private SharedPreferencesHelper prefsHelper;
    
    // Social Login
    private GoogleSignInClient googleSignInClient;

    // Mode: true = Login, false = Register
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();
        prefsHelper = new SharedPreferencesHelper(this);

        // Kiểm tra nếu đã đăng nhập, chuyển thẳng đến MainActivity
        if (prefsHelper.isLoggedIn()) {
            navigateToMainActivity();
            return;
        }

        // Khởi tạo Social Login
        initSocialLogin();

        // Khởi tạo UI
        initViews();
        setupClickListeners();

        // Mặc định hiển thị màn hình đăng nhập
        switchToLoginMode();
        
        // Thêm animation khi khởi động
        animateOnStart();
    }
    
    /**
     * Animation khi khởi động activity
     */
    private void animateOnStart() {
        View cardForm = findViewById(R.id.cardForm);
        
        if (cardForm != null) {
            cardForm.setAlpha(0f);
            cardForm.setTranslationY(50f);
            cardForm.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }
    
    /**
     * Khởi tạo Google Sign-In
     */
    private void initSocialLogin() {
        try {
            String webClientId = getString(R.string.default_web_client_id);
            
            // Kiểm tra web client ID có hợp lệ không
            if (webClientId == null || webClientId.isEmpty() || webClientId.contains("YOUR_")) {
                Log.e(TAG, "Web Client ID chưa được cấu hình đúng trong strings.xml");
                return;
            }
            
            // Google Sign-In
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khởi tạo Google Sign-In", e);
        }
    }

    /**
     * Khởi tạo các view components
     */
    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tilName = findViewById(R.id.tilName);
        tilGender = findViewById(R.id.tilGender);
        tilDateOfBirth = findViewById(R.id.tilDateOfBirth);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
        tvSwitchModeRegister = findViewById(R.id.tvSwitchModeRegister);
        switchModeLayout = findViewById(R.id.switchModeLayout);
        switchModeLayoutRegister = findViewById(R.id.switchModeLayoutRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvWelcome = findViewById(R.id.tvWelcome);
        progressBar = findViewById(R.id.progressBar);
        dividerLayout = findViewById(R.id.dividerLayout);
        socialButtonsLayout = findViewById(R.id.socialButtonsLayout);
        cardGenderMale = findViewById(R.id.cardGenderMale);
        cardGenderFemale = findViewById(R.id.cardGenderFemale);
    }

    /**
     * Thiết lập các click listeners
     */
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
        btnRegister.setOnClickListener(v -> handleRegister());
        if (tvSwitchMode != null) {
            tvSwitchMode.setOnClickListener(v -> toggleMode());
        }
        if (tvSwitchModeRegister != null) {
            tvSwitchModeRegister.setOnClickListener(v -> switchToLoginMode());
        }
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        
        // Social Login buttons
        btnGoogleSignIn.setOnClickListener(v -> handleGoogleSignIn());
        
        // Chọn giới tính - Card selection
        if (cardGenderMale != null) {
            cardGenderMale.setOnClickListener(v -> selectGender(getString(R.string.male)));
        }
        if (cardGenderFemale != null) {
            cardGenderFemale.setOnClickListener(v -> selectGender(getString(R.string.female)));
        }
    }

    /**
     * Chuyển đổi giữa chế độ đăng nhập và đăng ký
     */
    private void toggleMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            switchToLoginMode();
        } else {
            switchToRegisterMode();
        }
    }

    /**
     * Hiển thị giao diện đăng nhập
     */
    private void switchToLoginMode() {
        isLoginMode = true;
        
        // Update title
        if (tvWelcome != null) {
            tvWelcome.setText(getString(R.string.welcome_back));
            // Điều chỉnh margin top khi ẩn nút back
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) tvWelcome.getLayoutParams();
            if (params != null) {
                params.topMargin = (int) (48 * getResources().getDisplayMetrics().density);
                tvWelcome.setLayoutParams(params);
            }
        }
        
        // Animation hide register fields
        animateViewOut(tilName, () -> tilName.setVisibility(View.GONE));
        animateViewOut(tilGender, () -> tilGender.setVisibility(View.GONE));
        animateViewOut(tilDateOfBirth, () -> tilDateOfBirth.setVisibility(View.GONE));
        animateViewOut(tilConfirmPassword, () -> tilConfirmPassword.setVisibility(View.GONE));
        animateViewOut(btnRegister, () -> btnRegister.setVisibility(View.GONE));
        
        // Animation show login fields
        btnLogin.setVisibility(View.VISIBLE);
        btnLogin.setAlpha(0f);
        btnLogin.animate().alpha(1f).setDuration(300).start();
        
        tvForgotPassword.setVisibility(View.VISIBLE);
        tvForgotPassword.setAlpha(0f);
        tvForgotPassword.animate().alpha(1f).setDuration(300).start();
        
        // Hide register switch mode layout
        if (switchModeLayoutRegister != null) {
            switchModeLayoutRegister.setVisibility(View.GONE);
        }
        
        // Show login switch mode layout (below Google) and divider and social buttons in login mode
        if (switchModeLayout != null) {
            switchModeLayout.setVisibility(View.VISIBLE);
            switchModeLayout.setAlpha(0f);
            switchModeLayout.animate().alpha(1f).setDuration(300).start();
        }
        if (dividerLayout != null) {
            dividerLayout.setVisibility(View.VISIBLE);
        }
        if (socialButtonsLayout != null) {
            socialButtonsLayout.setVisibility(View.VISIBLE);
        }
        
        // Clear register fields
        etName.setText("");
        selectedGender = "";
        etDateOfBirth.setText("");
        etConfirmPassword.setText("");
        tilName.setError(null);
        tilDateOfBirth.setError(null);
        tilConfirmPassword.setError(null);
        resetGenderSelection();
    }

    /**
     * Hiển thị giao diện đăng ký
     */
    private void switchToRegisterMode() {
        isLoginMode = false;
        
        // Update title
        if (tvWelcome != null) {
            tvWelcome.setText(getString(R.string.welcome_new_user));
        }
        
        // Animation hide login fields
        animateViewOut(btnLogin, () -> btnLogin.setVisibility(View.GONE));
        animateViewOut(tvForgotPassword, () -> tvForgotPassword.setVisibility(View.GONE));
        
        // Animation show register fields
        tilName.setVisibility(View.VISIBLE);
        animateViewIn(tilName);
        
        tilGender.setVisibility(View.VISIBLE);
        animateViewIn(tilGender);
        
        tilDateOfBirth.setVisibility(View.VISIBLE);
        animateViewIn(tilDateOfBirth);
        
        tilConfirmPassword.setVisibility(View.VISIBLE);
        animateViewIn(tilConfirmPassword);
        
        btnRegister.setVisibility(View.VISIBLE);
        btnRegister.setAlpha(0f);
        btnRegister.animate().alpha(1f).setDuration(300).start();
        
        // Hide login switch mode layout (below Google)
        if (switchModeLayout != null) {
            switchModeLayout.setVisibility(View.GONE);
        }
        
        // Show register switch mode layout (below Register button)
        if (switchModeLayoutRegister != null) {
            switchModeLayoutRegister.setVisibility(View.VISIBLE);
            switchModeLayoutRegister.setAlpha(0f);
            switchModeLayoutRegister.animate().alpha(1f).setDuration(300).start();
        }
        
        // Hide divider and social buttons in register mode to save space
        if (dividerLayout != null) {
            dividerLayout.setVisibility(View.GONE);
        }
        if (socialButtonsLayout != null) {
            socialButtonsLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * Animation fade out và ẩn view
     */
    private void animateViewOut(View view, Runnable onComplete) {
        if (view == null) return;
        view.animate()
                .alpha(0f)
                .translationY(-10f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                        view.setTranslationY(0f);
                        view.animate().setListener(null);
                    }
                })
                .start();
    }
    
    /**
     * Animation fade in và hiển thị view
     */
    private void animateViewIn(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(10f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Xử lý đăng nhập
     */
    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (!validateInput(email, password, false)) {
            return;
        }

        // Kiểm tra kết nối mạng trước khi đăng nhập
        if (!NetworkHelper.isNetworkAvailable(this)) {
            showNetworkErrorDialogForLogin();
            return;
        }

        showLoading(true);

        // Đăng nhập với Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        showLoading(false);
                        
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            
                            if (firebaseUser != null) {
                                // Lấy thông tin user từ Firestore và kiểm tra role
                                checkUserRoleAndNavigate(firebaseUser.getUid());
                            }
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            
                            // Xử lý lỗi
                            Exception exception = task.getException();
                            String errorMessage = getString(R.string.login_error);
                            
                            if (exception != null && exception.getMessage() != null) {
                                String errorMsg = exception.getMessage();
                                
                                // Xử lý các lỗi phổ biến
                                if (errorMsg.contains("USER_NOT_FOUND") || errorMsg.contains("There is no user record")) {
                                    errorMessage = getString(R.string.email_not_registered);
                                    etEmail.setError(getString(R.string.email_not_registered_error));
                                } else if (errorMsg.contains("INVALID_EMAIL") || errorMsg.contains("The email address is badly formatted")) {
                                    errorMessage = getString(R.string.invalid_email_error);
                                    etEmail.setError(getString(R.string.invalid_email));
                                } else if (errorMsg.contains("WRONG_PASSWORD") || errorMsg.contains("The password is invalid")) {
                                    errorMessage = getString(R.string.wrong_old_password);
                                    etPassword.setError(getString(R.string.wrong_old_password));
                                } else if (errorMsg.contains("NETWORK") || errorMsg.contains("network") || 
                                           errorMsg.contains("NetworkError") || errorMsg.contains("network_error") ||
                                           errorMsg.contains("Unable to resolve host") || errorMsg.contains("Failed to connect")) {
                                    // Hiển thị dialog network error với nút Retry
                                    showNetworkErrorDialogForLogin();
                                    return; // Return sớm để không hiển thị Toast
                                } else {
                                    errorMessage = getString(R.string.error_occurred, errorMsg);
                                }
                            } else if (exception != null) {
                                // Nếu không có message, kiểm tra class name
                                String exceptionClass = exception.getClass().getSimpleName();
                                if (exceptionClass.contains("Network") || exceptionClass.contains("Socket")) {
                                    showNetworkErrorDialogForLogin();
                                    return;
                                }
                            }
                            
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Xử lý đăng ký
     */
    private void handleRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String gender = selectedGender;
        String dateOfBirth = etDateOfBirth.getText().toString().trim();

        // Validation
        if (!validateInput(email, password, true)) {
            return;
        }

        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.required_field));
            etName.requestFocus();
            return;
        } else {
            tilName.setError(null);
        }

        if (TextUtils.isEmpty(selectedGender)) {
            // Show error on gender selection
            if (tilGender != null) {
                View child = tilGender.getChildAt(0);
                if (child instanceof LinearLayout) {
                    LinearLayout layout = (LinearLayout) child;
                    View textView = layout.getChildAt(0);
                    if (textView instanceof TextView) {
                        ((TextView) textView).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                }
            }
            Toast.makeText(this, getString(R.string.required_field), Toast.LENGTH_SHORT).show();
            return;
        } else {
            // Clear error
            if (tilGender != null) {
                View child = tilGender.getChildAt(0);
                if (child instanceof LinearLayout) {
                    LinearLayout layout = (LinearLayout) child;
                    View textView = layout.getChildAt(0);
                    if (textView instanceof TextView) {
                        ((TextView) textView).setTextColor(getResources().getColor(R.color.text_secondary));
                    }
                }
            }
        }

        if (TextUtils.isEmpty(dateOfBirth)) {
            tilDateOfBirth.setError(getString(R.string.required_field));
            etDateOfBirth.requestFocus();
            return;
        } else {
            tilDateOfBirth.setError(null);
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.required_field));
            etConfirmPassword.requestFocus();
            return;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.passwords_do_not_match));
            etConfirmPassword.requestFocus();
            return;
        } else {
            tilConfirmPassword.setError(null);
        }

        // Kiểm tra kết nối mạng trước khi đăng ký
        if (!NetworkHelper.isNetworkAvailable(this)) {
            showNetworkErrorDialogForRegister();
            return;
        }

        showLoading(true);

        // Tạo tài khoản mới với Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            
                            if (firebaseUser != null) {
                                // Tạo user mới trong Firestore với role mặc định là "user"
                                createUserInFirestore(firebaseUser.getUid(), email, name, gender, dateOfBirth);
                            }
                        } else {
                            showLoading(false);
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            
                            // Xử lý lỗi
                            Exception exception = task.getException();
                            String errorMessage = getString(R.string.register_error);
                            
                            if (exception != null && exception.getMessage() != null) {
                                String errorMsg = exception.getMessage();
                                
                                // Xử lý các lỗi phổ biến
                                if (errorMsg.contains("EMAIL_ALREADY_IN_USE") || errorMsg.contains("The email address is already in use")) {
                                    errorMessage = getString(R.string.email_not_registered); // Có thể thay bằng string phù hợp hơn
                                    etEmail.setError(getString(R.string.email_not_registered_error));
                                } else if (errorMsg.contains("INVALID_EMAIL") || errorMsg.contains("The email address is badly formatted")) {
                                    errorMessage = getString(R.string.invalid_email_error);
                                    etEmail.setError(getString(R.string.invalid_email));
                                } else if (errorMsg.contains("WEAK_PASSWORD") || errorMsg.contains("The given password is invalid")) {
                                    errorMessage = getString(R.string.password_too_short);
                                    etPassword.setError(getString(R.string.password_too_short));
                                } else if (errorMsg.contains("NETWORK") || errorMsg.contains("network") || 
                                           errorMsg.contains("NetworkError") || errorMsg.contains("network_error") ||
                                           errorMsg.contains("Unable to resolve host") || errorMsg.contains("Failed to connect")) {
                                    // Hiển thị dialog network error với nút Retry
                                    showNetworkErrorDialogForRegister();
                                    return; // Return sớm để không hiển thị Toast
                                } else {
                                    errorMessage = getString(R.string.error_occurred, errorMsg);
                                }
                            } else if (exception != null) {
                                // Nếu không có message, kiểm tra class name
                                String exceptionClass = exception.getClass().getSimpleName();
                                if (exceptionClass.contains("Network") || exceptionClass.contains("Socket")) {
                                    showNetworkErrorDialogForRegister();
                                    return;
                                }
                            }
                            
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Tạo user mới trong Firestore với role mặc định là "user"
     */
    private void createUserInFirestore(String uid, String email, String name, String gender, String dateOfBirth) {
        User newUser = new User(uid, email, name, "user", 0.0, gender, dateOfBirth); // role mặc định = "user"
        
        // Set avatar mặc định (empty string sẽ hiển thị avatar mặc định trong UI)
        newUser.setAvatarUrl("");
        
        firebaseHelper.saveUser(newUser, task -> {
            showLoading(false);
            
            if (task.isSuccessful()) {
                Toast.makeText(LoginActivity.this, 
                        getString(R.string.register_success), 
                        Toast.LENGTH_SHORT).show();
                
                // Lưu session và chuyển đến MainActivity
                prefsHelper.saveUserSession(uid, email, name, "user");
                navigateToMainActivity();
            } else {
                Toast.makeText(LoginActivity.this, 
                        getString(R.string.register_error), 
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error creating user in Firestore", task.getException());
            }
        });
    }

    /**
     * Kiểm tra role của user trong Firestore và điều hướng tương ứng
     * Đây là phần quan trọng để phân luồng User/Admin
     */
    private void checkUserRoleAndNavigate(String uid) {
        firebaseHelper.getUser(uid, new FirebaseHelper.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                // Lưu thông tin user vào SharedPreferences
                prefsHelper.saveUserSession(
                        user.getUid(),
                        user.getEmail(),
                        user.getName(),
                        user.getRole()
                );

                // Kiểm tra role và điều hướng
                // Nếu role == "admin" -> Chuyển đến giao diện Admin
                // Nếu role == "user" -> Chuyển đến giao diện User
                if ("admin".equals(user.getRole())) {
                    Log.d(TAG, "User is admin, navigating to admin interface");
                } else {
                    Log.d(TAG, "User is regular user, navigating to user interface");
                }

                // Chuyển đến MainActivity (sẽ tự động hiển thị menu phù hợp dựa trên role)
                navigateToMainActivity();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Log.e(TAG, "Error getting user role: " + error);
                
                // Kiểm tra xem lỗi có phải do mạng không
                if (error != null && (error.contains("NETWORK") || error.contains("network") || 
                    error.contains("NetworkError") || error.contains("network_error") ||
                    error.contains("Unable to resolve host") || error.contains("Failed to connect") ||
                    error.contains("Connection") || error.contains("connection"))) {
                    // Lỗi mạng, hiển thị dialog với nút Retry
                    showNetworkErrorDialogForLogin();
                    return;
                }
                
                Toast.makeText(LoginActivity.this, 
                        getString(R.string.error) + ": " + (error != null ? error : getString(R.string.unknown_error)), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Điều hướng đến MainActivity
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Validation input
     */
    private boolean validateInput(String email, String password, boolean isRegister) {
        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.required_field));
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.invalid_email));
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.required_field));
            isValid = false;
        } else if (password.length() < 6) {
            etPassword.setError(getString(R.string.password_too_short));
            isValid = false;
        }

        return isValid;
    }

    /**
     * Hiển thị/ẩn loading
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnRegister.setEnabled(!show);
    }
    
    /**
     * Chọn giới tính từ card selection
     */
    private void selectGender(String gender) {
        selectedGender = gender;
        
        // Reset both cards
        resetGenderSelection();
        
        // Highlight selected card
        if (gender.equals(getString(R.string.male)) && cardGenderMale != null) {
            cardGenderMale.setStrokeWidth(4);
            cardGenderMale.setStrokeColor(getResources().getColor(R.color.green_500));
            cardGenderMale.setCardElevation(4f);
        } else if (gender.equals(getString(R.string.female)) && cardGenderFemale != null) {
            cardGenderFemale.setStrokeWidth(4);
            cardGenderFemale.setStrokeColor(getResources().getColor(R.color.green_500));
            cardGenderFemale.setCardElevation(4f);
        }
        
        // Clear error - find TextView in LinearLayout
        if (tilGender != null) {
            View child = tilGender.getChildAt(0);
            if (child instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) child;
                View textView = layout.getChildAt(0);
                if (textView instanceof TextView) {
                    ((TextView) textView).setTextColor(getResources().getColor(R.color.text_secondary));
                }
            }
        }
    }
    
    /**
     * Reset gender selection cards
     */
    private void resetGenderSelection() {
        if (cardGenderMale != null) {
            cardGenderMale.setStrokeWidth(2);
            cardGenderMale.setStrokeColor(getResources().getColor(android.R.color.darker_gray));
            cardGenderMale.setCardElevation(2f);
        }
        if (cardGenderFemale != null) {
            cardGenderFemale.setStrokeWidth(2);
            cardGenderFemale.setStrokeColor(getResources().getColor(android.R.color.darker_gray));
            cardGenderFemale.setCardElevation(2f);
        }
    }
    
    
    /**
     * Xử lý quên mật khẩu - Gửi link reset password qua email
     */
    private void handleForgotPassword() {
        handleForgotPasswordInternal();
    }
    
    /**
     * Internal method để gửi email reset password (có thể được gọi lại khi retry)
     */
    private void handleForgotPasswordInternal() {
        String email = etEmail.getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.required_field));
            Toast.makeText(this, getString(R.string.enter_email_to_reset), Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.invalid_email));
            return;
        }
        
        // Kiểm tra kết nối mạng trước khi gửi email
        if (!NetworkHelper.isNetworkAvailable(this)) {
            showNetworkErrorDialogForForgotPassword();
            return;
        }
        
        showLoading(true);
        
        // Gửi link reset password qua email bằng Firebase Auth
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        showLoading(false);
                        
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Password reset email sent successfully to: " + email);
                            
                            // Hiển thị thông báo thành công
                            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                            builder.setTitle(getString(R.string.password_reset_email_sent_title));
                            builder.setMessage(getString(R.string.password_reset_email_sent_message, email));
                            builder.setPositiveButton(getString(R.string.confirm), null);
                            builder.show();
                            
                            Toast.makeText(LoginActivity.this, 
                                    getString(R.string.password_reset_link_sent), 
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Xử lý lỗi
                            Exception exception = task.getException();
                            Log.e(TAG, "Error sending password reset email", exception);
                            
                            String errorMessage = getString(R.string.password_reset_email_error);
                            if (exception != null && exception.getMessage() != null) {
                                String errorMsg = exception.getMessage();
                                
                                // Xử lý các lỗi phổ biến
                                if (errorMsg.contains("USER_NOT_FOUND")) {
                                    errorMessage = getString(R.string.email_not_registered);
                                    etEmail.setError(getString(R.string.email_not_registered_error));
                                } else if (errorMsg.contains("INVALID_EMAIL")) {
                                    errorMessage = getString(R.string.invalid_email_error);
                                    etEmail.setError(getString(R.string.invalid_email));
                                } else if (errorMsg.contains("NETWORK") || errorMsg.contains("network")) {
                                    // Hiển thị dialog network error với nút Retry
                                    showNetworkErrorDialogForForgotPassword();
                                    return; // Return sớm để không hiển thị Toast
                                } else {
                                    errorMessage = getString(R.string.error_occurred, errorMsg);
                                }
                            }
                            
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    
    /**
     * Xử lý đăng nhập với Google
     */
    private void handleGoogleSignIn() {
        if (googleSignInClient == null) {
            Toast.makeText(this, getString(R.string.google_sign_in_not_configured), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Kiểm tra kết nối mạng trước khi đăng nhập
        if (!NetworkHelper.isNetworkAvailable(this)) {
            showNetworkErrorDialogForGoogleSignIn();
            return;
        }
        
        try {
            showLoading(true);
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        } catch (SecurityException e) {
            showLoading(false);
            Log.e(TAG, "SecurityException khi khởi động Google Sign-In", e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Unknown calling package")) {
                // Lỗi SHA-1 fingerprint chưa được thêm vào Firebase
                showSecurityExceptionDialog();
            } else {
                Toast.makeText(this, getString(R.string.security_error, errorMsg != null ? errorMsg : getString(R.string.unknown)), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            showLoading(false);
            Log.e(TAG, "Lỗi khi khởi động Google Sign-In", e);
            Toast.makeText(this, getString(R.string.error_occurred, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Hiển thị dialog hướng dẫn fix SecurityException
     */
    private void showSecurityExceptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.google_sign_in_config_error));
        builder.setMessage(getString(R.string.google_sign_in_config_error_message));
        builder.setPositiveButton(getString(R.string.confirm), null);
        builder.setNeutralButton("Mở Firebase Console", (dialog, which) -> {
            // Mở Firebase Console trong browser
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                    android.net.Uri.parse("https://console.firebase.google.com/project/yidoan-3d606/settings/general"));
                startActivity(browserIntent);
            } catch (Exception e) {
                Log.e(TAG, "Không thể mở browser", e);
            }
        });
        builder.show();
    }
    
    /**
     * Hiển thị dialog lỗi mạng với nút Retry cho Forgot Password
     */
    private void showNetworkErrorDialogForForgotPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.network_error_title));
        builder.setMessage(getString(R.string.network_error_message));
        builder.setCancelable(false);
        
        builder.setPositiveButton(getString(R.string.retry), (dialog, which) -> {
            // Kiểm tra lại kết nối mạng
            if (NetworkHelper.isNetworkAvailable(this)) {
                // Có mạng, thử lại gửi email reset password
                handleForgotPasswordInternal();
            } else {
                // Vẫn chưa có mạng, hiển thị lại dialog
                showNetworkErrorDialogForForgotPassword();
            }
        });
        
        builder.setNeutralButton(getString(R.string.check_network_settings), (dialog, which) -> {
            // Mở cài đặt mạng
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Không thể mở cài đặt mạng", e);
                // Fallback: mở cài đặt chung
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                } catch (Exception ex) {
                    Log.e(TAG, "Không thể mở cài đặt", ex);
                }
            }
        });
        
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
        });
        
        builder.show();
    }
    
    /**
     * Hiển thị dialog lỗi mạng với nút Retry cho Login
     */
    private void showNetworkErrorDialogForLogin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.network_error_title));
        builder.setMessage(getString(R.string.network_error_message));
        builder.setCancelable(false);
        
        builder.setPositiveButton(getString(R.string.retry), (dialog, which) -> {
            // Kiểm tra lại kết nối mạng
            if (NetworkHelper.isNetworkAvailable(this)) {
                // Có mạng, thử lại đăng nhập
                handleLogin();
            } else {
                // Vẫn chưa có mạng, hiển thị lại dialog
                showNetworkErrorDialogForLogin();
            }
        });
        
        builder.setNeutralButton(getString(R.string.check_network_settings), (dialog, which) -> {
            // Mở cài đặt mạng
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Không thể mở cài đặt mạng", e);
                // Fallback: mở cài đặt chung
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                } catch (Exception ex) {
                    Log.e(TAG, "Không thể mở cài đặt", ex);
                }
            }
        });
        
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
        });
        
        builder.show();
    }
    
    /**
     * Hiển thị dialog lỗi mạng với nút Retry cho Register
     */
    private void showNetworkErrorDialogForRegister() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.network_error_title));
        builder.setMessage(getString(R.string.network_error_message));
        builder.setCancelable(false);
        
        builder.setPositiveButton(getString(R.string.retry), (dialog, which) -> {
            // Kiểm tra lại kết nối mạng
            if (NetworkHelper.isNetworkAvailable(this)) {
                // Có mạng, thử lại đăng ký
                handleRegister();
            } else {
                // Vẫn chưa có mạng, hiển thị lại dialog
                showNetworkErrorDialogForRegister();
            }
        });
        
        builder.setNeutralButton(getString(R.string.check_network_settings), (dialog, which) -> {
            // Mở cài đặt mạng
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Không thể mở cài đặt mạng", e);
                // Fallback: mở cài đặt chung
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                } catch (Exception ex) {
                    Log.e(TAG, "Không thể mở cài đặt", ex);
                }
            }
        });
        
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
        });
        
        builder.show();
    }
    
    /**
     * Hiển thị dialog lỗi mạng với nút Retry cho Google Sign-In
     */
    private void showNetworkErrorDialogForGoogleSignIn() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.network_error_title));
        builder.setMessage(getString(R.string.network_error_message));
        builder.setCancelable(false);
        
        builder.setPositiveButton(getString(R.string.retry), (dialog, which) -> {
            // Kiểm tra lại kết nối mạng
            if (NetworkHelper.isNetworkAvailable(this)) {
                // Có mạng, thử lại Google Sign-In
                handleGoogleSignIn();
            } else {
                // Vẫn chưa có mạng, hiển thị lại dialog
                showNetworkErrorDialogForGoogleSignIn();
            }
        });
        
        builder.setNeutralButton(getString(R.string.check_network_settings), (dialog, which) -> {
            // Mở cài đặt mạng
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Không thể mở cài đặt mạng", e);
                // Fallback: mở cài đặt chung
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                } catch (Exception ex) {
                    Log.e(TAG, "Không thể mở cài đặt", ex);
                }
            }
        });
        
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
        });
        
        builder.show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Google Sign-In callback
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google sign in success: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                showLoading(false);
                Log.w(TAG, "Google sign in failed", e);
                
                // Xử lý các lỗi cụ thể
                String errorMessage = getString(R.string.google_sign_in_error);
                int statusCode = e.getStatusCode();
                
                switch (statusCode) {
                    case 10: // DEVELOPER_ERROR
                        // Hiển thị dialog hướng dẫn thay vì Toast
                        showSecurityExceptionDialog();
                        return; // Return sớm để không hiển thị Toast
                    case 7: // NETWORK_ERROR
                        // Hiển thị dialog network error với nút Retry
                        showNetworkErrorDialogForGoogleSignIn();
                        return; // Return sớm để không hiển thị Toast
                    case 8: // INTERNAL_ERROR
                        errorMessage = getString(R.string.internal_error);
                        break;
                    case 12500: // SIGN_IN_CANCELLED
                        errorMessage = getString(R.string.sign_in_cancelled);
                        break;
                    default:
                        errorMessage = getString(R.string.google_sign_in_error) + " (Code: " + statusCode + ")";
                        if (e.getMessage() != null && e.getMessage().contains("DEVELOPER_ERROR")) {
                            // Hiển thị dialog cho DEVELOPER_ERROR
                            showSecurityExceptionDialog();
                            return;
                        }
                        break;
                }
                
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            } catch (SecurityException e) {
                showLoading(false);
                Log.e(TAG, "SecurityException trong onActivityResult", e);
                showSecurityExceptionDialog();
            }
        }
    }
    
    /**
     * Xác thực với Firebase bằng Google ID Token
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Kiểm tra xem user đã tồn tại trong Firestore chưa
                                checkAndCreateUserForSocialLogin(firebaseUser);
                            }
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            
                            // Xử lý lỗi
                            Exception exception = task.getException();
                            String errorMessage = getString(R.string.google_sign_in_error);
                            
                            if (exception != null && exception.getMessage() != null) {
                                String errorMsg = exception.getMessage();
                                
                                // Xử lý lỗi mạng
                                if (errorMsg.contains("NETWORK") || errorMsg.contains("network") || 
                                    errorMsg.contains("NetworkError") || errorMsg.contains("network_error") ||
                                    errorMsg.contains("Unable to resolve host") || errorMsg.contains("Failed to connect") ||
                                    errorMsg.contains("Connection") || errorMsg.contains("connection")) {
                                    // Hiển thị dialog network error với nút Retry
                                    showNetworkErrorDialogForGoogleSignIn();
                                    return; // Return sớm để không hiển thị Toast
                                } else {
                                    errorMessage = getString(R.string.google_sign_in_error) + ": " + errorMsg;
                                }
                            } else if (exception != null) {
                                // Nếu không có message, kiểm tra class name
                                String exceptionClass = exception.getClass().getSimpleName();
                                if (exceptionClass.contains("Network") || exceptionClass.contains("Socket") ||
                                    exceptionClass.contains("Connection")) {
                                    showNetworkErrorDialogForGoogleSignIn();
                                    return;
                                } else {
                                    errorMessage = getString(R.string.google_sign_in_error);
                                }
                            }
                            
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    
    /**
     * Kiểm tra và tạo user trong Firestore nếu chưa tồn tại (cho Social Login)
     */
    private void checkAndCreateUserForSocialLogin(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        String displayName = firebaseUser.getDisplayName();
        final String name = (displayName != null && !displayName.isEmpty()) 
                ? displayName 
                : (email != null ? email.split("@")[0] : "User");
        
        firebaseHelper.getUser(uid, new FirebaseHelper.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                // User đã tồn tại, lưu session và điều hướng
                prefsHelper.saveUserSession(
                        user.getUid(),
                        user.getEmail(),
                        user.getName(),
                        user.getRole()
                );
                navigateToMainActivity();
            }

            @Override
            public void onError(String error) {
                // Kiểm tra xem lỗi có phải do mạng không
                if (error != null && (error.contains("NETWORK") || error.contains("network") || 
                    error.contains("NetworkError") || error.contains("network_error") ||
                    error.contains("Unable to resolve host") || error.contains("Failed to connect") ||
                    error.contains("Connection") || error.contains("connection"))) {
                    // Lỗi mạng, hiển thị dialog
                    showNetworkErrorDialogForGoogleSignIn();
                    return;
                }
                
                // User chưa tồn tại, tạo mới với role mặc định là "user"
                User newUser = new User(uid, email, name, "user", 0.0, "", "");
                // Set avatar mặc định (empty string sẽ hiển thị avatar mặc định trong UI)
                newUser.setAvatarUrl("");
                firebaseHelper.saveUser(newUser, task -> {
                    if (task.isSuccessful()) {
                        prefsHelper.saveUserSession(uid, email, name, "user");
                        navigateToMainActivity();
                    } else {
                        showLoading(false);
                        
                        // Kiểm tra lỗi mạng khi tạo user
                        Exception exception = task.getException();
                        if (exception != null && exception.getMessage() != null) {
                            String errorMsg = exception.getMessage();
                            if (errorMsg.contains("NETWORK") || errorMsg.contains("network") || 
                                errorMsg.contains("NetworkError") || errorMsg.contains("network_error") ||
                                errorMsg.contains("Unable to resolve host") || errorMsg.contains("Failed to connect") ||
                                errorMsg.contains("Connection") || errorMsg.contains("connection")) {
                                showNetworkErrorDialogForGoogleSignIn();
                                return;
                            }
                        }
                        
                        Toast.makeText(LoginActivity.this, 
                                getString(R.string.error) + ": " + (error != null ? error : getString(R.string.unknown_error)), 
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error creating user in Firestore", task.getException());
                    }
                });
            }
        });
    }
    
}

