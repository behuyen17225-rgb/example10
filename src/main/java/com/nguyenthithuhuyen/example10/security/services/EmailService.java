package com.nguyenthithuhuyen.example10.security.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("nguyenhuyenla209@gmail.com"); // 👈 EMAIL ĐÃ VERIFY TRÊN SENDGRID
        message.setTo(to);
        message.setSubject("Mã OTP đặt lại mật khẩu");
        message.setText(
                "Mã OTP của bạn là: " + otp +
                "\nOTP có hiệu lực trong 5 phút."
        );

        mailSender.send(message);
    }
}
