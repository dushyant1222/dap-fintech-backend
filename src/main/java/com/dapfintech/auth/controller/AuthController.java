package com.dapfintech.auth.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.auth.dto.request.ChangePasswordRequest;
import com.dapfintech.auth.dto.request.LoginRequest;
import com.dapfintech.auth.dto.request.LogoutRequest;
import com.dapfintech.auth.dto.request.RefreshTokenRequest;
import com.dapfintech.auth.dto.request.ResetEmployeePasswordRequest;
import com.dapfintech.auth.dto.response.LoginResponse;
import com.dapfintech.auth.service.AuthService;
import com.dapfintech.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(response)
                        .build()
        );
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {

        LoginResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .message("Token refreshed")
                        .data(response)
                        .build()
        );
    
    }
    
    @PostMapping("/reset-employee-password")
    public ResponseEntity<ApiResponse<String>> resetEmployeePassword(@RequestBody ResetEmployeePasswordRequest request) {

        authService.resetEmployeePassword(request);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                        .success(true)
                        .message("Employee password reset successfully")
                        .data("SUCCESS")
                        .build()
        );
    }
    
    
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePasswordRequest request) {

        authService.changePassword(request);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                        .success(true)
                        .message("Password changed successfully")
                        .data("SUCCESS")
                        .build()
        );
    }
    
    @PostMapping("/request-email-otp")
    public ResponseEntity<ApiResponse<String>> requestEmailOtp(@RequestBody com.dapfintech.auth.dto.request.RequestEmailOtpRequest request) {
        
    	authService.requestEmailOtp(request);
        
        return ResponseEntity.ok(ApiResponse.<String>builder()
                        .success(true)
                        .message("Verification code sent to email successfully")
                        .data("SENT")
                        .build()
        );
    }

    @PostMapping("/verify-email-otp-change-password")
    public ResponseEntity<ApiResponse<String>> verifyEmailOtpChangePassword(@RequestBody com.dapfintech.auth.dto.request.VerifyEmailOtpChangePasswordRequest request) {
        authService.verifyEmailOtpAndChangePassword(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                        .success(true)
                        .message("Password changed successfully via email verification")
                        .data("SUCCESS")
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.<String>builder()
                        .success(true)
                        .message("Logged out successfully")
                        .data("SUCCESS")
                        .build()
        );
    }
}