package com.dapfintech.auth.service;

import com.dapfintech.auth.dto.request.ChangePasswordRequest;
import com.dapfintech.auth.dto.request.LoginRequest;
import com.dapfintech.auth.dto.request.RefreshTokenRequest;
import com.dapfintech.auth.dto.request.RequestEmailOtpRequest;
import com.dapfintech.auth.dto.request.VerifyEmailOtpChangePasswordRequest;
import com.dapfintech.auth.dto.request.ResetEmployeePasswordRequest;
import com.dapfintech.auth.dto.response.LoginResponse;

public interface AuthService {

	LoginResponse login(
            LoginRequest request
    );

    LoginResponse refreshToken(
            RefreshTokenRequest request
    );

    void logout(
            String refreshToken
    );
    void changePassword(
            ChangePasswordRequest request
    );
    void resetEmployeePassword(
            ResetEmployeePasswordRequest request
    );
    void requestEmailOtp(
            RequestEmailOtpRequest request
    );
    void verifyEmailOtpAndChangePassword(
            VerifyEmailOtpChangePasswordRequest request
    );

    void updatePushSubscriptionId(String userId, String pushSubscriptionId);
}
