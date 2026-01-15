package com.nguyenthithuhuyen.example10.security.services;
import com.nguyenthithuhuyen.example10.entity.PasswordResetOtp;
import com.nguyenthithuhuyen.example10.entity.User;
import com.nguyenthithuhuyen.example10.repository.PasswordResetOtpRepository;
import com.nguyenthithuhuyen.example10.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import jakarta.transaction.Transactional;


@Service
@RequiredArgsConstructor
public class PasswordResetOtpService {

    private final PasswordResetOtpRepository otpRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Gửi OTP
    @Transactional
    public void sendOtp(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản bị khóa");
        }

        otpRepo.deleteByEmail(email);

        String otp = String.valueOf(
                100000 + new SecureRandom().nextInt(900000)
        );

        otpRepo.save(
                PasswordResetOtp.builder()
                        .email(email)
                        .otp(otp)
                        .expiredAt(LocalDateTime.now().plusMinutes(5))
                        .used(false)
                        .build()
        );

        // GỬI EMAIL
        emailService.sendOtpEmail(email, otp);
    }

    // Đổi mật khẩu
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {

        PasswordResetOtp resetOtp = otpRepo
                .findByEmailAndOtpAndUsedFalse(email, otp)
                .orElseThrow(() -> new RuntimeException("OTP không đúng"));

        if (resetOtp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP đã hết hạn");
        }

        User user = userRepo.findByEmail(email).orElseThrow();

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        resetOtp.setUsed(true);
        otpRepo.save(resetOtp);
    }
}
