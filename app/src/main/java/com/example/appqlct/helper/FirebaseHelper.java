package com.example.appqlct.helper;

import android.util.Log;

import com.example.appqlct.model.Budget;
import com.example.appqlct.model.Category;
import com.example.appqlct.model.Feedback;
import com.example.appqlct.model.Transaction;
import com.example.appqlct.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class để quản lý tất cả các thao tác với Firebase Firestore
 * Bao gồm: CRUD operations cho User, Transaction, Category, Feedback
 */
public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Collection names
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TRANSACTIONS = "transactions";
    private static final String COLLECTION_CATEGORIES = "categories";
    private static final String COLLECTION_FEEDBACK = "feedback";
    private static final String COLLECTION_CONFIG = "config";
    private static final String COLLECTION_BUDGETS = "budgets";

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // ========== USER OPERATIONS ==========

    /**
     * Lấy thông tin user từ Firestore dựa trên UID
     */
    public void getUser(String uid, OnUserLoadedListener listener) {
        db.collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            try {
                                User user = document.toObject(User.class);
                                if (user != null) {
                                    user.setUid(document.getId());
                                    
                                    // Đảm bảo các field được parse đúng (xử lý null)
                                    if (user.getPhone() == null) {
                                        user.setPhone("");
                                    }
                                    if (user.getEmail() == null) {
                                        user.setEmail("");
                                    }
                                    if (user.getName() == null) {
                                        user.setName("");
                                    }
                                    if (user.getRole() == null) {
                                        user.setRole("user");
                                    }
                                    if (user.getAvatarUrl() == null) {
                                        user.setAvatarUrl("");
                                    }
                                    if (user.getGender() == null) {
                                        user.setGender("");
                                    }
                                    if (user.getDateOfBirth() == null) {
                                        user.setDateOfBirth("");
                                    }
                                    
                                    listener.onUserLoaded(user);
                                } else {
                                    listener.onError("Failed to parse user data");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing user: " + uid, e);
                                listener.onError("Failed to parse user data: " + e.getMessage());
                            }
                        } else {
                            listener.onError("User not found");
                        }
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Tạo hoặc cập nhật user trong Firestore
     */
    public void saveUser(User user, OnCompleteListener<Void> listener) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("phone", user.getPhone() != null ? user.getPhone() : "");
        userData.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        userData.put("role", user.getRole());
        userData.put("budgetLimit", user.getBudgetLimit());
        userData.put("gender", user.getGender() != null ? user.getGender() : "");
        userData.put("dateOfBirth", user.getDateOfBirth() != null ? user.getDateOfBirth() : "");

        db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(userData)
                .addOnCompleteListener(listener);
    }

    /**
     * Lấy tất cả users (dành cho Admin)
     */
    public void getAllUsers(OnUsersLoadedListener listener) {
        db.collection(COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            user.setUid(document.getId());
                            users.add(user);
                        }
                        listener.onUsersLoaded(users);
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Xóa user (dành cho Admin)
     */
    public void deleteUser(String uid, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_USERS)
                .document(uid)
                .delete()
                .addOnCompleteListener(listener);
    }

    /**
     * Đổi mật khẩu cho user hiện tại
     * @param oldPassword Mật khẩu cũ
     * @param newPassword Mật khẩu mới
     * @param listener Callback để xử lý kết quả
     */
    public void changePassword(String oldPassword, String newPassword, OnPasswordChangeListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        
        if (user == null) {
            listener.onError("User not logged in");
            return;
        }

        // Kiểm tra xem user có đăng nhập bằng email/password không
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            listener.onError("Không thể xác định email người dùng");
            return;
        }

        // Kiểm tra provider - chỉ cho phép đổi mật khẩu nếu có provider "password"
        boolean hasPasswordProvider = false;
        for (com.google.firebase.auth.UserInfo provider : user.getProviderData()) {
            if (provider.getProviderId().equals("password")) {
                hasPasswordProvider = true;
                break;
            }
        }
        
        if (!hasPasswordProvider) {
            listener.onError("Không thể đổi mật khẩu cho tài khoản đăng nhập xã hội");
            return;
        }

        // Re-authenticate user với mật khẩu cũ
        AuthCredential credential = EmailAuthProvider.getCredential(email, oldPassword);
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Nếu re-authenticate thành công, đổi mật khẩu
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        listener.onSuccess();
                                    } else {
                                        String errorMessage = updateTask.getException() != null ? 
                                                updateTask.getException().getMessage() : "Unknown error";
                                        listener.onError(errorMessage);
                                    }
                                });
                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Old password is incorrect";
                        if (errorMessage.contains("wrong-password") || errorMessage.contains("invalid-credential")) {
                            listener.onError("Old password is incorrect");
                        } else {
                            listener.onError(errorMessage);
                        }
                    }
                });
    }
    
    /**
     * Kiểm tra xem user có thể đổi mật khẩu không (chỉ cho email/password provider)
     */
    public boolean canChangePassword() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            return false;
        }
        
        // Kiểm tra provider - chỉ cho phép nếu có provider "password"
        // (có thể có nhiều providers nhưng chỉ cần có "password" là có thể đổi mật khẩu)
        for (com.google.firebase.auth.UserInfo provider : user.getProviderData()) {
            String providerId = provider.getProviderId();
            if (providerId.equals("password")) {
                return true;
            }
        }
        return false;
    }

    // ========== TRANSACTION OPERATIONS ==========

    /**
     * Thêm transaction mới
     */
    public void addTransaction(Transaction transaction, OnCompleteListener<DocumentReference> listener) {
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("userId", transaction.getUserId());
        transactionData.put("amount", transaction.getAmount());
        transactionData.put("category", transaction.getCategory());
        transactionData.put("note", transaction.getNote());
        transactionData.put("date", transaction.getDate());
        transactionData.put("type", transaction.getType());
        transactionData.put("isRecurring", transaction.isRecurring());
        if (transaction.getRecurringTransactionId() != null) {
            transactionData.put("recurringTransactionId", transaction.getRecurringTransactionId());
        }
        if (transaction.getRecurringStartMonth() != null) {
            transactionData.put("recurringStartMonth", transaction.getRecurringStartMonth());
        }
        if (transaction.getRecurringEndMonth() != null) {
            transactionData.put("recurringEndMonth", transaction.getRecurringEndMonth());
        }

        db.collection(COLLECTION_TRANSACTIONS)
                .add(transactionData)
                .addOnCompleteListener(listener);
    }

    /**
     * Lấy tất cả transactions của một user
     */
    public void getUserTransactions(String userId, OnTransactionsLoadedListener listener) {
        Log.d("FirebaseHelper", "getUserTransactions - userId: " + userId);
        db.collection(COLLECTION_TRANSACTIONS)
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseHelper", "getUserTransactions - userId: " + userId + ", số documents: " + task.getResult().size());
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Parse userId thủ công từ document để đảm bảo đúng
                                Object userIdObj = document.get("userId");
                                String docUserId = null;
                                if (userIdObj != null) {
                                    docUserId = userIdObj.toString();
                                }
                                
                                // DEBUG: Log userId từ document
                                Log.d("FirebaseHelper", String.format("Transaction Document ID: %s, userId từ document: %s, expected userId: %s", 
                                    document.getId(), docUserId, userId));
                                
                                // Nếu userId từ document khác với userId được query, bỏ qua (an toàn)
                                if (docUserId == null || !docUserId.equals(userId)) {
                                    Log.w("FirebaseHelper", String.format("SKIP transaction - Document ID: %s, userId không khớp: expected=%s, actual=%s", 
                                        document.getId(), userId, docUserId));
                                    continue;
                                }
                                
                                Transaction transaction = document.toObject(Transaction.class);
                                transaction.setId(document.getId());
                                // Đảm bảo userId được set đúng
                                transaction.setUserId(docUserId);
                                
                                // Convert Firestore Timestamp to Date
                                Object dateObj = document.get("date");
                                if (dateObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setDate(((com.google.firebase.Timestamp) dateObj).toDate());
                                }
                                
                                // Parse isRecurring
                                Object recurringObj = document.get("isRecurring");
                                if (recurringObj instanceof Boolean) {
                                    transaction.setRecurring((Boolean) recurringObj);
                                } else {
                                    transaction.setRecurring(false);
                                }
                                
                                // Parse recurringTransactionId
                                Object recurringTransactionIdObj = document.get("recurringTransactionId");
                                if (recurringTransactionIdObj != null) {
                                    transaction.setRecurringTransactionId(recurringTransactionIdObj.toString());
                                }
                                
                                // Parse recurringStartMonth và recurringEndMonth
                                Object recurringStartMonthObj = document.get("recurringStartMonth");
                                if (recurringStartMonthObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setRecurringStartMonth(((com.google.firebase.Timestamp) recurringStartMonthObj).toDate());
                                }
                                
                                Object recurringEndMonthObj = document.get("recurringEndMonth");
                                if (recurringEndMonthObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setRecurringEndMonth(((com.google.firebase.Timestamp) recurringEndMonthObj).toDate());
                                }
                                
                                // DEBUG: Log transaction được thêm
                                Log.d("FirebaseHelper", String.format("ADD transaction - Document ID: %s, userId=%s, amount=%.0f, type=%s, date=%s", 
                                    document.getId(), transaction.getUserId(), transaction.getAmount(), 
                                    transaction.getType(), transaction.getDate() != null ? transaction.getDate().toString() : "null"));
                                
                                transactions.add(transaction);
                            } catch (Exception e) {
                                Log.e("FirebaseHelper", "Error parsing transaction document: " + document.getId(), e);
                            }
                        }
                        Log.d("FirebaseHelper", "getUserTransactions - userId: " + userId + ", tổng số transactions hợp lệ: " + transactions.size());
                        listener.onTransactionsLoaded(transactions);
                    } else {
                        Log.e("FirebaseHelper", "getUserTransactions - Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Lấy các chi tiêu định kỳ của user
     */
    public void getUserRecurringTransactions(String userId, OnTransactionsLoadedListener listener) {
        Log.d("FirebaseHelper", "getUserRecurringTransactions - userId: " + userId);
        // Không dùng orderBy để tránh cần composite index, sẽ sort trong memory
        db.collection(COLLECTION_TRANSACTIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRecurring", true)
                .whereEqualTo("type", "expense")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseHelper", "getUserRecurringTransactions - userId: " + userId + ", số documents: " + task.getResult().size());
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Object userIdObj = document.get("userId");
                                String docUserId = null;
                                if (userIdObj != null) {
                                    docUserId = userIdObj.toString();
                                }
                                
                                if (docUserId == null || !docUserId.equals(userId)) {
                                    continue;
                                }
                                
                                Transaction transaction = document.toObject(Transaction.class);
                                transaction.setId(document.getId());
                                transaction.setUserId(docUserId);
                                
                                Object dateObj = document.get("date");
                                if (dateObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setDate(((com.google.firebase.Timestamp) dateObj).toDate());
                                }
                                
                                // Đảm bảo isRecurring được set đúng
                                Object recurringObj = document.get("isRecurring");
                                if (recurringObj instanceof Boolean) {
                                    transaction.setRecurring((Boolean) recurringObj);
                                } else {
                                    transaction.setRecurring(false);
                                }
                                
                                // Parse recurringTransactionId
                                Object recurringTransactionIdObj = document.get("recurringTransactionId");
                                if (recurringTransactionIdObj != null) {
                                    transaction.setRecurringTransactionId(recurringTransactionIdObj.toString());
                                }
                                
                                // Parse recurringStartMonth và recurringEndMonth
                                Object recurringStartMonthObj = document.get("recurringStartMonth");
                                if (recurringStartMonthObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setRecurringStartMonth(((com.google.firebase.Timestamp) recurringStartMonthObj).toDate());
                                }
                                
                                Object recurringEndMonthObj = document.get("recurringEndMonth");
                                if (recurringEndMonthObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setRecurringEndMonth(((com.google.firebase.Timestamp) recurringEndMonthObj).toDate());
                                }
                                
                                transactions.add(transaction);
                            } catch (Exception e) {
                                Log.e("FirebaseHelper", "Error parsing recurring transaction document: " + document.getId(), e);
                            }
                        }
                        Log.d("FirebaseHelper", "getUserRecurringTransactions - userId: " + userId + ", tổng số transactions hợp lệ: " + transactions.size());
                        listener.onTransactionsLoaded(transactions);
                    } else {
                        Log.e("FirebaseHelper", "getUserRecurringTransactions - Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                        listener.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    /**
     * Lấy transactions trong tháng hiện tại (dành cho báo cáo)
     */
    public void getMonthlyTransactions(String userId, Date startDate, Date endDate, 
                                       OnTransactionsLoadedListener listener) {
        Log.d("FirebaseHelper", String.format("getMonthlyTransactions - userId: %s, startDate: %s, endDate: %s", 
            userId, startDate, endDate));
        db.collection(COLLECTION_TRANSACTIONS)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseHelper", String.format("getMonthlyTransactions - userId: %s, số documents: %d", 
                            userId, task.getResult().size()));
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // Parse userId thủ công từ document để đảm bảo đúng
                                Object userIdObj = document.get("userId");
                                String docUserId = null;
                                if (userIdObj != null) {
                                    docUserId = userIdObj.toString();
                                }
                                
                                // Nếu userId từ document khác với userId được query, bỏ qua (an toàn)
                                if (docUserId == null || !docUserId.equals(userId)) {
                                    Log.w("FirebaseHelper", String.format("SKIP monthly transaction - Document ID: %s, userId không khớp: expected=%s, actual=%s", 
                                        document.getId(), userId, docUserId));
                                    continue;
                                }
                                
                                Transaction transaction = document.toObject(Transaction.class);
                                transaction.setId(document.getId());
                                // Đảm bảo userId được set đúng
                                transaction.setUserId(docUserId);
                                
                                Object dateObj = document.get("date");
                                if (dateObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setDate(((com.google.firebase.Timestamp) dateObj).toDate());
                                }
                                
                                // Parse isRecurring
                                Object recurringObj = document.get("isRecurring");
                                if (recurringObj instanceof Boolean) {
                                    transaction.setRecurring((Boolean) recurringObj);
                                } else {
                                    transaction.setRecurring(false);
                                }
                                
                                // Parse recurringTransactionId
                                Object recurringTransactionIdObj = document.get("recurringTransactionId");
                                if (recurringTransactionIdObj != null) {
                                    transaction.setRecurringTransactionId(recurringTransactionIdObj.toString());
                                }
                                
                                // Parse recurringStartMonth và recurringEndMonth
                                Object recurringStartMonthObj = document.get("recurringStartMonth");
                                if (recurringStartMonthObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setRecurringStartMonth(((com.google.firebase.Timestamp) recurringStartMonthObj).toDate());
                                }
                                
                                Object recurringEndMonthObj = document.get("recurringEndMonth");
                                if (recurringEndMonthObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setRecurringEndMonth(((com.google.firebase.Timestamp) recurringEndMonthObj).toDate());
                                }
                                
                                transactions.add(transaction);
                            } catch (Exception e) {
                                Log.e("FirebaseHelper", "Error parsing monthly transaction document: " + document.getId(), e);
                            }
                        }
                        Log.d("FirebaseHelper", String.format("getMonthlyTransactions - userId: %s, tổng số transactions hợp lệ: %d", 
                            userId, transactions.size()));
                        listener.onTransactionsLoaded(transactions);
                    } else {
                        Log.e("FirebaseHelper", "getMonthlyTransactions - Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Lắng nghe real-time changes cho transactions trong tháng (dành cho báo cáo)
     * Trả về ListenerRegistration để có thể hủy listener khi không cần nữa
     */
    public ListenerRegistration listenMonthlyTransactions(String userId, Date startDate, Date endDate, 
                                                          OnTransactionsLoadedListener listener) {
        return db.collection(COLLECTION_TRANSACTIONS)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    
                    if (querySnapshot != null) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            try {
                                // Parse userId thủ công từ document để đảm bảo đúng
                                Object userIdObj = document.get("userId");
                                String docUserId = null;
                                if (userIdObj != null) {
                                    docUserId = userIdObj.toString();
                                }
                                
                                // Nếu userId từ document khác với userId được query, bỏ qua (an toàn)
                                if (docUserId == null || !docUserId.equals(userId)) {
                                    Log.w("FirebaseHelper", String.format("SKIP listen monthly transaction - Document ID: %s, userId không khớp: expected=%s, actual=%s", 
                                        document.getId(), userId, docUserId));
                                    continue;
                                }
                                
                                Transaction transaction = document.toObject(Transaction.class);
                                transaction.setId(document.getId());
                                // Đảm bảo userId được set đúng
                                transaction.setUserId(docUserId);
                                
                                Object dateObj = document.get("date");
                                if (dateObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setDate(((com.google.firebase.Timestamp) dateObj).toDate());
                                }
                                
                                // Parse isRecurring
                                Object recurringObj = document.get("isRecurring");
                                if (recurringObj instanceof Boolean) {
                                    transaction.setRecurring((Boolean) recurringObj);
                                } else {
                                    transaction.setRecurring(false);
                                }
                                
                                // Parse recurringTransactionId
                                Object recurringTransactionIdObj = document.get("recurringTransactionId");
                                if (recurringTransactionIdObj != null) {
                                    transaction.setRecurringTransactionId(recurringTransactionIdObj.toString());
                                }
                                
                                // Parse recurringStartMonth và recurringEndMonth
                                Object recurringStartMonthObj = document.get("recurringStartMonth");
                                if (recurringStartMonthObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setRecurringStartMonth(((com.google.firebase.Timestamp) recurringStartMonthObj).toDate());
                                }
                                
                                Object recurringEndMonthObj = document.get("recurringEndMonth");
                                if (recurringEndMonthObj instanceof com.google.firebase.Timestamp) {
                                    transaction.setRecurringEndMonth(((com.google.firebase.Timestamp) recurringEndMonthObj).toDate());
                                }
                                
                                transactions.add(transaction);
                            } catch (Exception e) {
                                Log.e("FirebaseHelper", "Error parsing listen monthly transaction document: " + document.getId(), e);
                            }
                        }
                        listener.onTransactionsLoaded(transactions);
                    }
                });
    }

    /**
     * Cập nhật transaction
     */
    public void updateTransaction(Transaction transaction, OnCompleteListener<Void> listener) {
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("userId", transaction.getUserId());
        transactionData.put("amount", transaction.getAmount());
        transactionData.put("category", transaction.getCategory());
        transactionData.put("note", transaction.getNote());
        transactionData.put("date", transaction.getDate());
        transactionData.put("type", transaction.getType());
        transactionData.put("isRecurring", transaction.isRecurring());
        if (transaction.getRecurringTransactionId() != null) {
            transactionData.put("recurringTransactionId", transaction.getRecurringTransactionId());
        }
        if (transaction.getRecurringStartMonth() != null) {
            transactionData.put("recurringStartMonth", transaction.getRecurringStartMonth());
        }
        if (transaction.getRecurringEndMonth() != null) {
            transactionData.put("recurringEndMonth", transaction.getRecurringEndMonth());
        }

        db.collection(COLLECTION_TRANSACTIONS)
                .document(transaction.getId())
                .set(transactionData)
                .addOnCompleteListener(listener);
    }

    /**
     * Xóa transaction
     */
    public void deleteTransaction(String transactionId, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_TRANSACTIONS)
                .document(transactionId)
                .delete()
                .addOnCompleteListener(listener);
    }

    /**
     * Xóa các transactions cũ hơn một ngày cụ thể (dành cho Maintenance)
     */
    public void deleteOldTransactions(Date beforeDate, OnDeleteOldTransactionsListener listener) {
        db.collection(COLLECTION_TRANSACTIONS)
                .whereLessThan("date", beforeDate)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot.isEmpty()) {
                            listener.onDeleted(0);
                            return;
                        }
                        
                        // Xóa từng transaction (Firestore không hỗ trợ batch delete tốt với query lớn)
                        int totalCount = snapshot.size();
                        final int[] deletedCount = {0};
                        final int[] errorCount = {0};
                        
                        for (QueryDocumentSnapshot document : snapshot) {
                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        synchronized (deletedCount) {
                                            deletedCount[0]++;
                                            if (deletedCount[0] + errorCount[0] >= totalCount) {
                                                if (errorCount[0] > 0) {
                                                    listener.onError("Đã xóa " + deletedCount[0] + "/" + totalCount + " giao dịch");
                                                } else {
                                                    listener.onDeleted(deletedCount[0]);
                                                }
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        synchronized (errorCount) {
                                            errorCount[0]++;
                                            if (deletedCount[0] + errorCount[0] >= totalCount) {
                                                listener.onError(String.format("Deleted %d/%d transactions. Error: %s", deletedCount[0], totalCount, e.getMessage()));
                                            }
                                        }
                                    });
                        }
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Lấy tất cả transactions (dành cho Admin báo cáo tổng hợp)
     */
    public void getAllTransactions(OnTransactionsLoadedListener listener) {
        db.collection(COLLECTION_TRANSACTIONS)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            Object dateObj = document.get("date");
                            if (dateObj instanceof com.google.firebase.Timestamp) {
                                transaction.setDate(((com.google.firebase.Timestamp) dateObj).toDate());
                            }
                            transactions.add(transaction);
                        }
                        listener.onTransactionsLoaded(transactions);
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Lấy tất cả transactions trong khoảng thời gian (dành cho Admin báo cáo)
     */
    public void getAllTransactions(Date startDate, Date endDate, OnTransactionsLoadedListener listener) {
        db.collection(COLLECTION_TRANSACTIONS)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            Object dateObj = document.get("date");
                            if (dateObj instanceof com.google.firebase.Timestamp) {
                                transaction.setDate(((com.google.firebase.Timestamp) dateObj).toDate());
                            }
                            transactions.add(transaction);
                        }
                        listener.onTransactionsLoaded(transactions);
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    // ========== CATEGORY OPERATIONS ==========

    /**
     * Lấy tất cả categories
     */
    public void getAllCategories(OnCategoriesLoadedListener listener) {
        db.collection(COLLECTION_CATEGORIES)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Category> categories = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Category category = document.toObject(Category.class);
                                if (category != null) {
                                    // Đảm bảo có đầy đủ thông tin
                                    if (category.getName() != null && category.getType() != null) {
                                        category.setId(document.getId());
                                        // Đảm bảo icon không null
                                        if (category.getIcon() == null) {
                                            category.setIcon("");
                                        }
                                        categories.add(category);
                                    }
                                }
                            } catch (Exception e) {
                                // Bỏ qua các document không hợp lệ, tiếp tục với document tiếp theo
                            }
                        }
                        // Sắp xếp: income trước, expense sau, sau đó sắp xếp theo tên
                        categories.sort((c1, c2) -> {
                            int typeCompare = c1.getType().compareTo(c2.getType());
                            if (typeCompare != 0) {
                                return typeCompare;
                            }
                            return c1.getName().compareTo(c2.getName());
                        });
                        listener.onCategoriesLoaded(categories);
                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Unknown error";
                        listener.onError(errorMessage);
                    }
                });
    }

    /**
     * Thêm category mới (dành cho Admin)
     */
    public void addCategory(Category category, OnCompleteListener<DocumentReference> listener) {
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("name", category.getName());
        categoryData.put("icon", category.getIcon());
        categoryData.put("type", category.getType());

        db.collection(COLLECTION_CATEGORIES)
                .add(categoryData)
                .addOnCompleteListener(listener);
    }

    /**
     * Cập nhật category (dành cho Admin)
     */
    public void updateCategory(Category category, OnCompleteListener<Void> listener) {
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("name", category.getName());
        categoryData.put("icon", category.getIcon());
        categoryData.put("type", category.getType());

        db.collection(COLLECTION_CATEGORIES)
                .document(category.getId())
                .set(categoryData)
                .addOnCompleteListener(listener);
    }

    /**
     * Xóa category (dành cho Admin)
     */
    public void deleteCategory(String categoryId, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_CATEGORIES)
                .document(categoryId)
                .delete()
                .addOnCompleteListener(listener);
    }

    // ========== FEEDBACK OPERATIONS ==========

    /**
     * Thêm feedback mới
     */
    public void addFeedback(Feedback feedback, OnCompleteListener<DocumentReference> listener) {
        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("userId", feedback.getUserId());
        feedbackData.put("rating", feedback.getRating());
        feedbackData.put("content", feedback.getContent());
        feedbackData.put("date", feedback.getDate());
        feedbackData.put("status", feedback.getStatus() != null ? feedback.getStatus() : "pending");

        db.collection(COLLECTION_FEEDBACK)
                .add(feedbackData)
                .addOnCompleteListener(listener);
    }

    // ========== CONFIG OPERATIONS (Maintenance) ==========

    /**
     * Lấy trạng thái bảo trì hệ thống
     */
    public void getMaintenanceStatus(OnMaintenanceStatusListener listener) {
        db.collection(COLLECTION_CONFIG)
                .document("maintenance")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        boolean isMaintenance = false;
                        if (document.exists()) {
                            Boolean status = document.getBoolean("enabled");
                            isMaintenance = status != null && status;
                        }
                        listener.onStatusLoaded(isMaintenance);
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Cập nhật trạng thái bảo trì (dành cho Admin)
     */
    public void setMaintenanceStatus(boolean enabled, OnCompleteListener<Void> listener) {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", enabled);
        config.put("updatedAt", new Date());

        db.collection(COLLECTION_CONFIG)
                .document("maintenance")
                .set(config)
                .addOnCompleteListener(listener);
    }

    // ========== BUDGET OPERATIONS ==========

    /**
     * Thêm ngân sách mới
     */
    public void addBudget(Budget budget, OnCompleteListener<DocumentReference> listener) {
        Map<String, Object> budgetData = new HashMap<>();
        budgetData.put("userId", budget.getUserId());
        budgetData.put("categoryName", budget.getCategoryName());
        budgetData.put("amount", budget.getAmount());
        budgetData.put("month", budget.getMonth());
        budgetData.put("year", budget.getYear());
        budgetData.put("createdAt", budget.getCreatedAt() != null ? budget.getCreatedAt() : new Date());
        budgetData.put("updatedAt", budget.getUpdatedAt() != null ? budget.getUpdatedAt() : new Date());

        db.collection(COLLECTION_BUDGETS)
                .add(budgetData)
                .addOnCompleteListener(listener);
    }

    /**
     * Lấy tất cả ngân sách của một user
     * Lưu ý: Query này có thể cần composite index trong Firestore
     * Firebase sẽ tự động đề xuất tạo index khi cần
     */
    public void getUserBudgets(String userId, OnBudgetsLoadedListener listener) {
        db.collection(COLLECTION_BUDGETS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseHelper", "getUserBudgets - userId: " + userId + ", số documents: " + task.getResult().size());
                        List<Budget> budgets = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Budget budget = document.toObject(Budget.class);
                                budget.setId(document.getId());
                                
                                // DEBUG: Log raw data từ Firestore
                                Log.d("FirebaseHelper", String.format("Document ID: %s, raw month type: %s, raw year type: %s, raw userId: %s", 
                                    document.getId(), 
                                    document.get("month") != null ? document.get("month").getClass().getSimpleName() : "null",
                                    document.get("year") != null ? document.get("year").getClass().getSimpleName() : "null",
                                    document.get("userId") != null ? document.get("userId").toString() : "null"));
                                
                                // Parse userId thủ công từ document để đảm bảo đúng
                                Object userIdObj = document.get("userId");
                                String docUserId = null;
                                if (userIdObj != null) {
                                    docUserId = userIdObj.toString();
                                }
                                // Nếu userId từ document khác với userId được query, bỏ qua (an toàn)
                                if (docUserId == null || !docUserId.equals(userId)) {
                                    Log.w("FirebaseHelper", String.format("SKIP budget - Document ID: %s, userId không khớp: expected=%s, actual=%s", 
                                        document.getId(), userId, docUserId));
                                    continue;
                                }
                                budget.setUserId(docUserId);
                                
                                // Parse month và year thủ công từ document để đảm bảo đúng format
                                // Firestore có thể trả về Long hoặc Integer
                                int month = 0;
                                Object monthObj = document.get("month");
                                if (monthObj != null) {
                                    if (monthObj instanceof Long) {
                                        month = ((Long) monthObj).intValue();
                                    } else if (monthObj instanceof Integer) {
                                        month = (Integer) monthObj;
                                    } else if (monthObj instanceof Number) {
                                        month = ((Number) monthObj).intValue();
                                    } else {
                                        // Nếu không parse được, thử dùng giá trị từ budget object
                                        month = budget.getMonth();
                                    }
                                } else {
                                    // Nếu monthObj là null, thử dùng giá trị từ budget object
                                    month = budget.getMonth();
                                }
                                
                                // DEBUG: Log giá trị month sau khi parse
                                Log.d("FirebaseHelper", String.format("Document ID: %s, parsed month: %d", document.getId(), month));
                                
                                // Kiểm tra month hợp lệ (1-12) - bắt buộc phải có
                                if (month < 1 || month > 12) {
                                    // Bỏ qua budget không hợp lệ
                                    Log.w("FirebaseHelper", String.format("SKIP budget - Document ID: %s, month không hợp lệ: %d", document.getId(), month));
                                    continue;
                                }
                                budget.setMonth(month);
                                
                                int year = 0;
                                Object yearObj = document.get("year");
                                if (yearObj != null) {
                                    if (yearObj instanceof Long) {
                                        year = ((Long) yearObj).intValue();
                                    } else if (yearObj instanceof Integer) {
                                        year = (Integer) yearObj;
                                    } else if (yearObj instanceof Number) {
                                        year = ((Number) yearObj).intValue();
                                    } else {
                                        // Nếu không parse được, thử dùng giá trị từ budget object
                                        year = budget.getYear();
                                    }
                                } else {
                                    // Nếu yearObj là null, thử dùng giá trị từ budget object
                                    year = budget.getYear();
                                }
                                
                                // DEBUG: Log giá trị year sau khi parse
                                Log.d("FirebaseHelper", String.format("Document ID: %s, parsed year: %d", document.getId(), year));
                                
                                // Kiểm tra year hợp lệ (2000-2100) - bắt buộc phải có
                                if (year < 2000 || year > 2100) {
                                    // Bỏ qua budget không hợp lệ
                                    Log.w("FirebaseHelper", String.format("SKIP budget - Document ID: %s, year không hợp lệ: %d", document.getId(), year));
                                    continue;
                                }
                                budget.setYear(year);
                            
                                // Convert Firestore Timestamp to Date
                                Object createdAtObj = document.get("createdAt");
                                if (createdAtObj instanceof com.google.firebase.Timestamp) {
                                    budget.setCreatedAt(((com.google.firebase.Timestamp) createdAtObj).toDate());
                                }
                                Object updatedAtObj = document.get("updatedAt");
                                if (updatedAtObj instanceof com.google.firebase.Timestamp) {
                                    budget.setUpdatedAt(((com.google.firebase.Timestamp) updatedAtObj).toDate());
                                }
                                
                                // DEBUG: Log budget hợp lệ được thêm vào list
                                Log.d("FirebaseHelper", String.format("ADD budget - ID: %s, userId: %s, category: %s, amount: %.0f, month: %d, year: %d", 
                                    budget.getId(), budget.getUserId(), budget.getCategoryName(), budget.getAmount(), budget.getMonth(), budget.getYear()));
                                
                                budgets.add(budget);
                            } catch (Exception e) {
                                // Bỏ qua budget nếu có lỗi khi parse
                                Log.e("FirebaseHelper", "ERROR parsing budget - Document ID: " + document.getId(), e);
                                continue;
                            }
                        }
                        
                        // DEBUG: Log tổng số budgets hợp lệ
                        Log.d("FirebaseHelper", "Tổng số budgets hợp lệ: " + budgets.size());
                        // Sắp xếp theo năm và tháng (descending) trong code
                        budgets.sort((b1, b2) -> {
                            int yearCompare = Integer.compare(b2.getYear(), b1.getYear());
                            if (yearCompare != 0) return yearCompare;
                            return Integer.compare(b2.getMonth(), b1.getMonth());
                        });
                        listener.onBudgetsLoaded(budgets);
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Lấy tất cả ngân sách (dành cho Admin)
     */
    public void getAllBudgets(OnBudgetsLoadedListener listener) {
        db.collection(COLLECTION_BUDGETS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Budget> budgets = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Budget budget = document.toObject(Budget.class);
                                budget.setId(document.getId());
                                
                                // Parse dates
                                Object createdAtObj = document.get("createdAt");
                                if (createdAtObj instanceof com.google.firebase.Timestamp) {
                                    budget.setCreatedAt(((com.google.firebase.Timestamp) createdAtObj).toDate());
                                }
                                Object updatedAtObj = document.get("updatedAt");
                                if (updatedAtObj instanceof com.google.firebase.Timestamp) {
                                    budget.setUpdatedAt(((com.google.firebase.Timestamp) updatedAtObj).toDate());
                                }
                                
                                // Parse month và year
                                Object monthObj = document.get("month");
                                if (monthObj != null) {
                                    if (monthObj instanceof Long) {
                                        budget.setMonth(((Long) monthObj).intValue());
                                    } else if (monthObj instanceof Integer) {
                                        budget.setMonth((Integer) monthObj);
                                    }
                                }
                                
                                Object yearObj = document.get("year");
                                if (yearObj != null) {
                                    if (yearObj instanceof Long) {
                                        budget.setYear(((Long) yearObj).intValue());
                                    } else if (yearObj instanceof Integer) {
                                        budget.setYear((Integer) yearObj);
                                    }
                                }
                                
                                budgets.add(budget);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing budget: " + document.getId(), e);
                            }
                        }
                        listener.onBudgetsLoaded(budgets);
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Lấy ngân sách của user cho tháng/năm cụ thể
     */
    public void getUserBudgets(String userId, int month, int year, OnBudgetsLoadedListener listener) {
        db.collection(COLLECTION_BUDGETS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("month", month)
                .whereEqualTo("year", year)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Budget> budgets = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Budget budget = document.toObject(Budget.class);
                            budget.setId(document.getId());
                            Object createdAtObj = document.get("createdAt");
                            if (createdAtObj instanceof com.google.firebase.Timestamp) {
                                budget.setCreatedAt(((com.google.firebase.Timestamp) createdAtObj).toDate());
                            }
                            Object updatedAtObj = document.get("updatedAt");
                            if (updatedAtObj instanceof com.google.firebase.Timestamp) {
                                budget.setUpdatedAt(((com.google.firebase.Timestamp) updatedAtObj).toDate());
                            }
                            budgets.add(budget);
                        }
                        listener.onBudgetsLoaded(budgets);
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Cập nhật ngân sách
     */
    public void updateBudget(Budget budget, OnCompleteListener<Void> listener) {
        Map<String, Object> budgetData = new HashMap<>();
        budgetData.put("userId", budget.getUserId());
        budgetData.put("categoryName", budget.getCategoryName());
        budgetData.put("amount", budget.getAmount());
        budgetData.put("month", budget.getMonth());
        budgetData.put("year", budget.getYear());
        budgetData.put("updatedAt", new Date());
        // Giữ nguyên createdAt
        if (budget.getCreatedAt() != null) {
            budgetData.put("createdAt", budget.getCreatedAt());
        }

        db.collection(COLLECTION_BUDGETS)
                .document(budget.getId())
                .set(budgetData)
                .addOnCompleteListener(listener);
    }

    /**
     * Xóa ngân sách
     */
    public void deleteBudget(String budgetId, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_BUDGETS)
                .document(budgetId)
                .delete()
                .addOnCompleteListener(listener);
    }

    // ========== RECURRING EXPENSE OPERATIONS ==========

    /**
     * Tạo các giao dịch chi tiêu tự động cho từng tháng trong khoảng thời gian định kỳ
     * @param recurringTransaction Giao dịch định kỳ gốc
     * @param listener Callback khi hoàn thành
     */
    public void generateMonthlyTransactionsFromRecurring(Transaction recurringTransaction, OnCompleteListener<Void> listener) {
        if (recurringTransaction == null || !recurringTransaction.isRecurring() || 
            recurringTransaction.getRecurringStartMonth() == null || 
            recurringTransaction.getRecurringEndMonth() == null ||
            recurringTransaction.getId() == null) {
            if (listener != null) {
                listener.onComplete(Tasks.forException(new Exception("Invalid recurring transaction")));
            }
            return;
        }

        java.util.Calendar startCal = java.util.Calendar.getInstance();
        startCal.setTime(recurringTransaction.getRecurringStartMonth());
        startCal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        startCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        startCal.set(java.util.Calendar.MINUTE, 0);
        startCal.set(java.util.Calendar.SECOND, 0);
        startCal.set(java.util.Calendar.MILLISECOND, 0);

        java.util.Calendar endCal = java.util.Calendar.getInstance();
        endCal.setTime(recurringTransaction.getRecurringEndMonth());
        endCal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        endCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        endCal.set(java.util.Calendar.MINUTE, 0);
        endCal.set(java.util.Calendar.SECOND, 0);
        endCal.set(java.util.Calendar.MILLISECOND, 0);

        // Kiểm tra tất cả transactions của user với cùng category và type trong khoảng thời gian
        // để tránh tạo trùng transaction khi có nhiều recurring transactions cùng category và cùng tháng bắt đầu
        com.google.firebase.Timestamp startTimestamp = new com.google.firebase.Timestamp(startCal.getTime());
        com.google.firebase.Timestamp endTimestamp = new com.google.firebase.Timestamp(endCal.getTime());
        
        // Tính toán endTimestamp cho tháng cuối cùng (ngày cuối cùng của tháng)
        java.util.Calendar lastDayCal = (java.util.Calendar) endCal.clone();
        lastDayCal.add(java.util.Calendar.MONTH, 1);
        lastDayCal.add(java.util.Calendar.DAY_OF_MONTH, -1);
        lastDayCal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        lastDayCal.set(java.util.Calendar.MINUTE, 59);
        lastDayCal.set(java.util.Calendar.SECOND, 59);
        lastDayCal.set(java.util.Calendar.MILLISECOND, 999);
        com.google.firebase.Timestamp endTimestampInclusive = new com.google.firebase.Timestamp(lastDayCal.getTime());
        
        db.collection(COLLECTION_TRANSACTIONS)
                .whereEqualTo("userId", recurringTransaction.getUserId())
                .whereEqualTo("type", recurringTransaction.getType())
                .whereEqualTo("category", recurringTransaction.getCategory())
                .whereGreaterThanOrEqualTo("date", startTimestamp)
                .whereLessThanOrEqualTo("date", endTimestampInclusive)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Tạo Set chứa các tháng đã có transactions với cùng category (format: "YYYY-MM")
                    java.util.Set<String> existingMonths = new java.util.HashSet<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Object dateObj = document.get("date");
                        if (dateObj instanceof com.google.firebase.Timestamp) {
                            Date transactionDate = ((com.google.firebase.Timestamp) dateObj).toDate();
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            cal.setTime(transactionDate);
                            String monthKey = String.format("%04d-%02d", cal.get(java.util.Calendar.YEAR), 
                                    cal.get(java.util.Calendar.MONTH) + 1);
                            existingMonths.add(monthKey);
                        }
                    }

                    List<Task<DocumentReference>> tasks = new ArrayList<>();
                    java.util.Calendar currentCal = (java.util.Calendar) startCal.clone();

                    // Tạo giao dịch cho từng tháng (chỉ tạo nếu chưa có transaction nào trong tháng đó với cùng category)
                    while (!currentCal.after(endCal)) {
                        String monthKey = String.format("%04d-%02d", currentCal.get(java.util.Calendar.YEAR), 
                                currentCal.get(java.util.Calendar.MONTH) + 1);
                        
                        // Chỉ tạo transaction nếu tháng này chưa có transaction nào với cùng category
                        // Điều này tránh tạo trùng transaction khi có nhiều recurring transactions cùng category và cùng tháng bắt đầu
                        if (!existingMonths.contains(monthKey)) {
                            Transaction monthlyTransaction = new Transaction(
                                    null, // id sẽ được tạo bởi Firestore
                                    recurringTransaction.getUserId(),
                                    recurringTransaction.getAmount(),
                                    recurringTransaction.getCategory(),
                                    recurringTransaction.getNote() != null ? recurringTransaction.getNote() : "",
                                    currentCal.getTime(),
                                    recurringTransaction.getType(), // Sử dụng type từ recurring transaction
                                    false // Không phải giao dịch định kỳ, đây là giao dịch thực tế
                            );
                            monthlyTransaction.setRecurringTransactionId(recurringTransaction.getId());

                            Map<String, Object> transactionData = new HashMap<>();
                            transactionData.put("userId", monthlyTransaction.getUserId());
                            transactionData.put("amount", monthlyTransaction.getAmount());
                            transactionData.put("category", monthlyTransaction.getCategory());
                            transactionData.put("note", monthlyTransaction.getNote());
                            transactionData.put("date", monthlyTransaction.getDate());
                            transactionData.put("type", monthlyTransaction.getType());
                            transactionData.put("isRecurring", false);
                            transactionData.put("recurringTransactionId", recurringTransaction.getId());

                            Task<DocumentReference> task = db.collection(COLLECTION_TRANSACTIONS).add(transactionData);
                            tasks.add(task);
                        }

                        // Chuyển sang tháng tiếp theo
                        currentCal.add(java.util.Calendar.MONTH, 1);
                    }

                    // Chờ tất cả các task hoàn thành
                    if (tasks.isEmpty()) {
                        // Không có transaction nào cần tạo
                        if (listener != null) {
                            listener.onComplete(Tasks.forResult(null));
                        }
                    } else {
                        Task<List<Task<?>>> allTasks = Tasks.whenAllComplete(tasks);
                        allTasks.addOnCompleteListener(task -> {
                            if (listener != null) {
                                if (task.isSuccessful()) {
                                    listener.onComplete(Tasks.forResult(null));
                                } else {
                                    listener.onComplete(Tasks.forException(task.getException()));
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing transactions: " + e.getMessage());
                    if (listener != null) {
                        listener.onComplete(Tasks.forException(e));
                    }
                });
    }

    /**
     * Xóa tất cả các giao dịch được tạo từ một giao dịch định kỳ
     * @param recurringTransactionId ID của giao dịch định kỳ gốc
     * @param listener Callback khi hoàn thành
     */
    public void deleteTransactionsFromRecurring(String recurringTransactionId, OnCompleteListener<Void> listener) {
        if (recurringTransactionId == null || recurringTransactionId.isEmpty()) {
            if (listener != null) {
                listener.onComplete(Tasks.forResult(null));
            }
            return;
        }

        db.collection(COLLECTION_TRANSACTIONS)
                .whereEqualTo("recurringTransactionId", recurringTransactionId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot.isEmpty()) {
                            if (listener != null) {
                                listener.onComplete(Tasks.forResult(null));
                            }
                            return;
                        }

                        // Xóa tất cả các giao dịch
                        List<Task<Void>> deleteTasks = new ArrayList<>();
                        for (QueryDocumentSnapshot document : snapshot) {
                            deleteTasks.add(document.getReference().delete());
                        }

                        Task<List<Task<?>>> allDeleteTasks = Tasks.whenAllComplete(deleteTasks);
                        allDeleteTasks.addOnCompleteListener(deleteTask -> {
                            if (listener != null) {
                                if (deleteTask.isSuccessful()) {
                                    listener.onComplete(Tasks.forResult(null));
                                } else {
                                    listener.onComplete(Tasks.forException(deleteTask.getException()));
                                }
                            }
                        });
                    } else {
                        if (listener != null) {
                            listener.onComplete(Tasks.forException(task.getException()));
                        }
                    }
                });
    }

    // ========== INTERFACES ==========

    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
        void onError(String error);
    }

    public interface OnUsersLoadedListener {
        void onUsersLoaded(List<User> users);
        void onError(String error);
    }

    public interface OnTransactionsLoadedListener {
        void onTransactionsLoaded(List<Transaction> transactions);
        void onError(String error);
    }

    public interface OnCategoriesLoadedListener {
        void onCategoriesLoaded(List<Category> categories);
        void onError(String error);
    }

    public interface OnMaintenanceStatusListener {
        void onStatusLoaded(boolean isMaintenance);
        void onError(String error);
    }

    public interface OnBudgetsLoadedListener {
        void onBudgetsLoaded(List<Budget> budgets);
        void onError(String error);
    }

    public interface OnDeleteOldTransactionsListener {
        void onDeleted(int count);
        void onError(String error);
    }

    public interface OnPasswordChangeListener {
        void onSuccess();
        void onError(String error);
    }
}

