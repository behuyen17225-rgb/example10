package com.nguyenthithuhuyen.example10.repository;
import com.nguyenthithuhuyen.example10.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import jakarta.transaction.Transactional;

import java.util.Optional;

public interface PasswordResetOtpRepository
        extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findByEmailAndOtpAndUsedFalse(String email, String otp);
     @Transactional
    @Modifying
    void deleteByEmail(String email);
}
