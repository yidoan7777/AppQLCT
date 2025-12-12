package com.example.appqlct.helper;

import com.example.appqlct.model.Category;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class để khởi tạo các danh mục mặc định cho ứng dụng
 */
public class CategoryInitializer {
    private static final String COLLECTION_CATEGORIES = "categories";
    private FirebaseFirestore db;

    public CategoryInitializer() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Khởi tạo các danh mục mặc định
     * Xóa tất cả danh mục cũ và chỉ thêm 5 danh mục mới
     * @param listener Callback để xử lý kết quả
     */
    public void initializeDefaultCategories(OnInitializationCompleteListener listener) {
        // Đầu tiên, xóa tất cả danh mục hiện có
        db.collection(COLLECTION_CATEGORIES)
                .get()
                .addOnCompleteListener(deleteTask -> {
                    if (deleteTask.isSuccessful()) {
                        QuerySnapshot snapshot = deleteTask.getResult();
                        List<Task<Void>> deleteTasks = new ArrayList<>();
                        
                        // Xóa tất cả danh mục hiện có
                        for (QueryDocumentSnapshot document : snapshot) {
                            Task<Void> deleteTask2 = document.getReference().delete();
                            deleteTasks.add(deleteTask2);
                        }
                        
                        // Đợi tất cả các task xóa hoàn thành, sau đó thêm danh mục mới
                        if (deleteTasks.isEmpty()) {
                            // Không có danh mục nào để xóa, thêm danh mục mới luôn
                            addDefaultCategories(listener);
                        } else {
                            Tasks.whenAllComplete(deleteTasks)
                                    .addOnCompleteListener(deleteCompleteTask -> {
                                        if (deleteCompleteTask.isSuccessful()) {
                                            // Sau khi xóa xong, thêm danh mục mới
                                            addDefaultCategories(listener);
                                        } else {
                                            listener.onError(deleteCompleteTask.getException() != null ? 
                                                    deleteCompleteTask.getException().getMessage() : "Error deleting old categories");
                                        }
                                    });
                        }
                    } else {
                        listener.onError(deleteTask.getException() != null ? 
                                deleteTask.getException().getMessage() : "Error getting categories list");
                    }
                });
    }
    
    /**
     * Thêm 5 danh mục mặc định mới
     */
    private void addDefaultCategories(OnInitializationCompleteListener listener) {
        List<Category> defaultCategories = getDefaultCategories();
        List<Task<DocumentReference>> addTasks = new ArrayList<>();
        
        for (Category category : defaultCategories) {
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("name", category.getName());
            categoryData.put("icon", category.getIcon());
            categoryData.put("type", category.getType());

            Task<DocumentReference> addTask = db.collection(COLLECTION_CATEGORIES)
                    .add(categoryData);
            addTasks.add(addTask);
        }

        // Đợi tất cả các task thêm hoàn thành
        Tasks.whenAllComplete(addTasks)
                .addOnCompleteListener(addTask -> {
                    if (addTask.isSuccessful()) {
                        listener.onSuccess("Initialized " + defaultCategories.size() + " default categories");
                    } else {
                        listener.onError(addTask.getException() != null ? 
                                addTask.getException().getMessage() : "Unknown error");
                    }
                });
    }

    /**
     * Khởi tạo chỉ các danh mục Thu nhập (nếu chưa có)
     * @param listener Callback để xử lý kết quả
     */
    public void initializeIncomeCategories(OnInitializationCompleteListener listener) {
        // No default income categories
        listener.onSuccess("No default income categories to initialize");
    }

    /**
     * Lấy danh sách các danh mục mặc định
     */
    private List<Category> getDefaultCategories() {
        List<Category> categories = new ArrayList<>();

        // Danh mục Chi tiêu
        categories.addAll(getExpenseCategories());

        return categories;
    }

    /**
     * Lấy danh sách các danh mục Chi tiêu mặc định
     */
    private List<Category> getExpenseCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category(null, "Food & Dining", "🍔", "expense"));
        categories.add(new Category(null, "Transportation", "🚗", "expense"));
        categories.add(new Category(null, "Education", "📚", "expense"));
        categories.add(new Category(null, "Utilities", "💡", "expense"));
        categories.add(new Category(null, "Entertainment", "🎬", "expense"));
        return categories;
    }

    /**
     * Interface để xử lý kết quả khởi tạo
     */
    public interface OnInitializationCompleteListener {
        void onSuccess(String message);
        void onError(String error);
    }
}

