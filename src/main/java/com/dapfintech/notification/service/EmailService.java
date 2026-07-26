package com.dapfintech.notification.service;

public interface EmailService {
    void sendVerificationOtp(String toEmail, String fullName, String otpCode);
}
