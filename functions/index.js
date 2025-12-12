const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

/**
 * Cloud Function để reset password sau khi verify OTP thành công
 * Gọi từ Android app sau khi user verify OTP và nhập mật khẩu mới
 */
exports.resetPasswordAfterOTP = functions.https.onCall(async (data, context) => {
  // Kiểm tra input
  if (!data.email || !data.newPassword) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Email và mật khẩu mới là bắt buộc'
    );
  }

  const { email, newPassword } = data;

  // Kiểm tra độ dài mật khẩu
  if (newPassword.length < 6) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Mật khẩu phải có ít nhất 6 ký tự'
    );
  }

  try {
    // Lấy user bằng email
    const userRecord = await admin.auth().getUserByEmail(email);
    
    // Reset password bằng Admin SDK
    await admin.auth().updateUser(userRecord.uid, {
      password: newPassword
    });

    return { success: true, message: 'Mật khẩu đã được đặt lại thành công' };
  } catch (error) {
    console.error('Error resetting password:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Lỗi khi đặt lại mật khẩu: ' + error.message
    );
  }
});

/**
 * Cloud Function để gửi email OTP
 * Trigger khi có document mới được tạo trong collection otp_codes
 */
exports.sendOTPEmail = functions.firestore
  .document('otp_codes/{email}')
  .onCreate(async (snap, context) => {
    const data = snap.data();
    const email = data.email;
    const otpCode = data.code;

    // TODO: Implement gửi email thực sự bằng nodemailer hoặc SendGrid
    // Hiện tại chỉ log ra để test
    console.log(`OTP Code for ${email}: ${otpCode}`);

    // Trong production, bạn cần:
    // 1. Cài đặt nodemailer: npm install nodemailer
    // 2. Cấu hình email service (Gmail, SendGrid, etc.)
    // 3. Gửi email chứa mã OTP

    return null;
  });

