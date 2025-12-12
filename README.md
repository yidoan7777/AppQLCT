# AppQLCT - Ứng dụng Quản lý Chi tiêu Cá nhân

Ứng dụng Android để quản lý chi tiêu cá nhân với Firebase backend.

## 📱 Tính năng

- Đăng nhập/Đăng ký với Email và Google Sign-In
- Quản lý giao dịch (Thu/Chi)
- Quản lý ngân sách theo tháng
- Báo cáo và thống kê chi tiêu
- Quản lý danh mục chi tiêu
- Thông báo khi vượt ngân sách
- Phân quyền Admin/User
- Gửi phản hồi

## 🛠️ Công nghệ sử dụng

- **Language:** Java
- **Framework:** Android SDK
- **Backend:** Firebase (Authentication, Firestore, Cloud Functions)
- **Build Tool:** Gradle
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36

## 📋 Yêu cầu

- Android Studio
- JDK 8+
- Firebase project đã được cấu hình
- Google Services JSON file

## 🚀 Cài đặt

1. Clone repository:
```bash
git clone https://github.com/yourusername/AppQLCT.git
```

2. Mở project trong Android Studio

3. Thêm file `google-services.json` vào thư mục `app/`

4. Build và chạy ứng dụng

## 📦 Build APK

```bash
./gradlew clean assembleRelease
```

APK sẽ được tạo tại: `app/build/outputs/apk/release/app-release.apk`

## 📄 License

MIT License

