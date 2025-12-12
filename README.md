# AppQLCT - Ứng dụng Quản lý Chi tiêu Cá nhân

[![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)](https://www.android.com/)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=java&logoColor=white)](https://www.java.com/)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)

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

## 📋 Yêu cầu

- **Android Studio**: Arctic Fox trở lên
- **JDK**: 11+
- **Firebase Project**: Đã được cấu hình với:
  - Authentication (Email/Password, Google Sign-In)
  - Cloud Firestore
  - Cloud Functions (nếu có)
- **Google Services**: File `google-services.json` từ Firebase Console

## 🚀 Cài đặt

### 1. Clone repository
```bash
git clone https://github.com/yidoan7777/AppQLCT.git
cd AppQLCT
```

### 2. Cấu hình Firebase

1. Tạo project mới trên [Firebase Console](https://console.firebase.google.com/)
2. Thêm Android app với package name: `com.example.appqlct`
3. Tải file `google-services.json`
4. Đặt file vào thư mục `app/`

### 3. Mở project

1. Mở Android Studio
2. Chọn **File > Open** và chọn thư mục `AppQLCT`
3. Đợi Gradle sync hoàn tất

### 4. Chạy ứng dụng

- Kết nối thiết bị Android hoặc khởi động emulator
- Nhấn **Run** (Shift + F10) hoặc click nút Run

## 📦 Build APK

### Build APK Release
```bash
./gradlew clean assembleRelease
```

APK sẽ được tạo tại: `app/build/outputs/apk/release/Yidoan.apk`

### Build APK Debug
```bash
./gradlew clean assembleDebug
```

APK sẽ được tạo tại: `app/build/outputs/apk/debug/app-debug.apk`

## 📁 Cấu trúc dự án

```
AppQLCT/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/appqlct/
│   │   │   │   ├── adapter/          # RecyclerView Adapters
│   │   │   │   ├── fragment/         # Fragments (User & Admin)
│   │   │   │   ├── helper/           # Helper classes
│   │   │   │   ├── model/            # Data models
│   │   │   │   ├── LoginActivity.java
│   │   │   │   ├── MainActivity.java
│   │   │   │   └── ResetPasswordActivity.java
│   │   │   ├── res/                  # Resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                     # Unit tests
│   ├── build.gradle.kts
│   └── google-services.json          # Firebase config (cần thêm)
├── gradle/
├── website/                          # Website landing page
└── README.md
```

## 🔐 Phân quyền

Ứng dụng hỗ trợ 2 loại tài khoản:

- **User**: Người dùng thông thường, có quyền quản lý chi tiêu cá nhân
- **Admin**: Quản trị viên, có quyền quản lý hệ thống và người dùng

## 📸 Screenshots

_(Có thể thêm screenshots của ứng dụng tại đây)_

## 🤝 Đóng góp

Mọi đóng góp đều được chào đón! Vui lòng:

1. Fork repository
2. Tạo feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Mở Pull Request

## 📧 Liên hệ

- **Email hỗ trợ**: [yidoan20055@gmail.com](mailto:yidoan20055@gmail.com)
- **GitHub**: [@yidoan7777](https://github.com/yidoan7777)
- **Repository**: [AppQLCT](https://github.com/yidoan7777/AppQLCT)

## 📄 License

Dự án này được phân phối dưới giấy phép MIT. Xem file `LICENSE` để biết thêm chi tiết.

## 🙏 Lời cảm ơn

Cảm ơn bạn đã sử dụng AppQLCT! Nếu bạn thấy ứng dụng hữu ích, hãy ⭐ star repository này nhé!

