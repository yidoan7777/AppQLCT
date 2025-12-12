# AppQLCT - Ứng dụng Quản lý Chi tiêu Cá nhân

Ứng dụng Android để quản lý chi tiêu cá nhân với Firebase backend. Giúp bạn theo dõi, quản lý và phân tích chi tiêu một cách hiệu quả.

## 📱 Tính năng

### 👤 Tính năng cho User
- **Xác thực đa dạng**: Đăng nhập/Đăng ký với Email và Google Sign-In
- **Quản lý giao dịch**: Thêm, sửa, xóa giao dịch Thu/Chi với đầy đủ thông tin
- **Quản lý ngân sách**: Thiết lập và theo dõi ngân sách theo tháng
- **Báo cáo và thống kê**: 
  - Biểu đồ thống kê chi tiêu theo tháng
  - Báo cáo theo danh mục
  - Xem chi tiết giao dịch theo danh mục
- **Quản lý danh mục**: Xem và sử dụng các danh mục chi tiêu có sẵn
- **Chi tiêu định kỳ**: Quản lý các khoản chi tiêu lặp lại
- **Thông báo thông minh**: 
  - Cảnh báo khi chi tiêu đạt 80% ngân sách
  - Cảnh báo khi vượt ngân sách
- **Hồ sơ cá nhân**: Quản lý thông tin tài khoản, đổi mật khẩu
- **Gửi phản hồi**: Gửi ý kiến đóng góp cho admin

### 🔧 Tính năng cho Admin
- **Quản lý người dùng**: Xem danh sách, thống kê và quản lý người dùng
- **Quản lý danh mục**: Thêm, sửa, xóa danh mục chi tiêu
- **Báo cáo tổng hợp**: 
  - Báo cáo tổng quan hệ thống
  - Báo cáo theo từng người dùng
- **Bảo trì hệ thống**: Các công cụ quản trị hệ thống

## 🛠️ Công nghệ sử dụng

- **Language**: Java 11
- **Framework**: Android SDK
- **Backend**: 
  - Firebase Authentication (Email/Password, Google Sign-In)
  - Cloud Firestore (Database)
  - Cloud Functions
- **UI Components**: 
  - Material Design Components
  - Navigation Drawer
  - ViewPager2
  - MPAndroidChart (Biểu đồ)
- **Libraries**:
  - Picasso (Image loading)
  - CircleImageView (Avatar)
  - Gson (JSON parsing)
- **Build Tool**: Gradle
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 36
- **Version**: 1.0

## 📦 Vị trí file APK

File APK sau khi build sẽ được tạo tại:
```
app/build/outputs/apk/release/app-release.apk
```

