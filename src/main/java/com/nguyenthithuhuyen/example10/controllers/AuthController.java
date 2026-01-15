package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.payload.request.LoginRequest;
import com.nguyenthithuhuyen.example10.payload.request.SignUpRequest;
import com.nguyenthithuhuyen.example10.payload.response.JwtResponse;
import com.nguyenthithuhuyen.example10.payload.response.MessageResponse;
import com.nguyenthithuhuyen.example10.repository.RoleRepository;
import com.nguyenthithuhuyen.example10.repository.UserRepository;
import com.nguyenthithuhuyen.example10.security.jwt.JwtUtils;
import com.nguyenthithuhuyen.example10.security.services.EmailService;
import com.nguyenthithuhuyen.example10.security.services.PasswordResetOtpService;
import com.nguyenthithuhuyen.example10.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final PasswordResetOtpService otpService;
    private final EmailService emailService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    // ✅ Constructor đúng
    public AuthController(
            PasswordResetOtpService otpService,
            EmailService emailService
    ) {
        this.otpService = otpService;
        this.emailService = emailService;
    }

    // TEST EMAIL
    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, String> body) {
        emailService.sendOtpEmail(body.get("email"), "123456");
        return ResponseEntity.ok("Send email OK");
    }

    // GỬI OTP
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        otpService.sendOtp(email);
        return ResponseEntity.ok("OTP đã được gửi qua email");
    }

    // RESET PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        otpService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    @Data
    static class ResetPasswordRequest {
        private String email;
        private String otp;
        private String newPassword;
    }

    // SIGNIN
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        ));
    }
}
