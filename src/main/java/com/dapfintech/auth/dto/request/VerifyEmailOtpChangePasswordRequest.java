package com.dapfintech.auth.dto.request;

import lombok.Data;

@Data
public class VerifyEmailOtpChangePasswordRequest {
    private String email;
    private String otp;
    private String newPassword;
}
