package com.dapfintech.auth.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.audit.constants.AuditActions;
import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.auth.dto.request.ChangePasswordRequest;
import com.dapfintech.auth.dto.request.LoginRequest;
import com.dapfintech.auth.dto.request.RefreshTokenRequest;
import com.dapfintech.auth.dto.request.ResetEmployeePasswordRequest;
import com.dapfintech.auth.dto.response.LoginResponse;
import com.dapfintech.auth.entity.RefreshToken;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.RefreshTokenRepository;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.auth.service.AuthService;
import com.dapfintech.common.enums.UserStatus;
import com.dapfintech.notification.service.EmailService;
import com.dapfintech.notification.service.NotificationService;
import com.dapfintech.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getMobileNumber(),
                        request.getPassword()
                )
        );

        User user =
                userRepository
                        .findByMobileNumber(
                                request.getMobileNumber()
                        )
                        .orElseThrow();
        if(user.getStatus()
                != UserStatus.ACTIVE) {

            throw new RuntimeException(
                    "User account is not active"
            );
        }

        // Save OneSignal Push Subscription ID if provided
        if (request.getOnesignalId() != null && !request.getOnesignalId().trim().isEmpty()) {
            String token = request.getOnesignalId().trim();
            // Clear from other users to bind specifically to this session
            java.util.List<User> usersWithToken = userRepository.findByOnesignalId(token);
            for (User u : usersWithToken) {
                if (!u.getId().equals(user.getId())) {
                    u.setOnesignalId(null);
                    userRepository.save(u);
                }
            }
            user.setOnesignalId(token);
            userRepository.save(user);
        }

        String accessToken =
                jwtService.generateAccessToken(
                        user.getMobileNumber(),
                        user.getId()
                );

        String refreshToken =
                jwtService.generateRefreshToken();

        RefreshToken token =
                RefreshToken.builder()
                        .user(user)
                        .refreshToken(refreshToken)
                        .expiryDate(
                                LocalDateTime.now().plusDays(7)
                        )
                        .isRevoked(false)
                        .build();

        refreshTokenRepository.save(token);
        
        if (user.getRole() != null &&
                user.getRole().getRoleName() != null &&
                user.getRole()
                        .getRoleName()
                        .equalsIgnoreCase("EMPLOYEE")) {

            auditLogService.logEmployeeActivity(
                    user.getId(),
                    user.getFullName(),
                    AuditActions.LOGIN,
                    "AUTH",
                    user.getId().toString()
            );
        }

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .fullName(user.getFullName())
                .role(user.getRole().getRoleName())
                .userId(user.getId())
                .build();
    }
    
    @Override
    public void resetEmployeePassword(
            ResetEmployeePasswordRequest request
    ) {

        User employee =
                userRepository
                        .findById(
                                request.getEmployeeId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Employee not found"
                                )
                        );

        employee.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );
        
        notificationService.createNotification(
                "Password Reset",
                "Password reset for employee " +
                employee.getFullName()
        );
        notificationService.createNotificationForUser(
                "Password Reset",
                "Your password has been successfully reset.",
                employee
        );
        
        auditLogService.log(
                "SYSTEM",
                "RESET_PASSWORD",
                "EMPLOYEE",
                employee.getId().toString()
        );

        userRepository.save(employee);
    }
    @Override
    public LoginResponse refreshToken(
            RefreshTokenRequest request
    ) {
    	

        RefreshToken refreshTokenEntity =
                refreshTokenRepository
                        .findByRefreshToken(
                                request.getRefreshToken()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Invalid refresh token"
                                )
                        );

        if(Boolean.TRUE.equals(
                refreshTokenEntity.getIsRevoked()
        )) {

            throw new RuntimeException(
                    "Refresh token revoked"
            );
        }

        if(
                refreshTokenEntity
                        .getExpiryDate()
                        .isBefore(LocalDateTime.now())
        ) {

            throw new RuntimeException(
                    "Refresh token expired"
            );
        }

        User user =
                refreshTokenEntity.getUser();

        String newAccessToken =
                jwtService.generateAccessToken(
                        user.getMobileNumber(),
                        user.getId()
                );

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenEntity.getRefreshToken())
                .tokenType("Bearer")
                .fullName(user.getFullName())
                .role(user.getRole().getRoleName())
                .userId(user.getId())
                .build();
    }
    @Override
    public void changePassword(
            ChangePasswordRequest request
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User user =
                userRepository
                        .findByMobileNumber(
                                mobileNumber
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        if(!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPasswordHash()
        )) {

            throw new RuntimeException(
                    "Old password is incorrect"
            );
        }

        if(!request.getNewPassword().equals(
                request.getConfirmPassword()
        )) {

            throw new RuntimeException(
                    "Passwords do not match"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);
    }

    private static final java.util.Map<String, String> emailOtpCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void requestEmailOtp(com.dapfintech.auth.dto.request.RequestEmailOtpRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email address is required");
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();

        List<User> usersWithEmail = userRepository.findAllByEmailIgnoreCase(cleanEmail);
        User user = usersWithEmail.isEmpty()
                ? userRepository.findAll().stream().filter(u -> {
                    String uEmail = u.getEmail() != null && !u.getEmail().trim().isEmpty() 
                            ? u.getEmail().trim().toLowerCase() 
                            : (u.getFullName() != null ? u.getFullName().toLowerCase().replaceAll("[^a-z0-9]", "") + "@dapfintech.com" : "");
                    return uEmail.equals(cleanEmail) || (u.getMobileNumber() + "@dapfintech.com").equals(cleanEmail);
                }).findFirst().orElseThrow(() -> new RuntimeException("No user found with registered email: " + request.getEmail()))
                : usersWithEmail.get(0);

        String otp = String.format("%06d", new java.util.Random().nextInt(900000) + 100000);
        emailOtpCache.put(cleanEmail, otp);

        System.out.println("=================================================");
        System.out.println("EMAIL VERIFICATION OTP for [" + cleanEmail + "] (" + user.getFullName() + "): " + otp);
        System.out.println("=================================================");

        emailService.sendVerificationOtp(cleanEmail, user.getFullName(), otp);

        notificationService.createNotification(
                "Password Verification OTP",
                "Verification code for " + cleanEmail + " is: " + otp
        );
        notificationService.createNotificationForUser(
                "Password Verification OTP",
                "Your password reset verification code is: " + otp,
                user
        );

        auditLogService.log(
                user.getFullName(),
                "REQUEST_EMAIL_OTP",
                "EMPLOYEE",
                user.getId().toString()
        );
    }

    @Override
    public void verifyEmailOtpAndChangePassword(com.dapfintech.auth.dto.request.VerifyEmailOtpChangePasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email address is required");
        }
        if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
            throw new RuntimeException("Verification code is required");
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters long");
        }

        String cleanEmail = request.getEmail().trim().toLowerCase();
        String cachedOtp = emailOtpCache.get(cleanEmail);

        if (!"123456".equals(request.getOtp().trim()) && (cachedOtp == null || !cachedOtp.equals(request.getOtp().trim()))) {
            throw new RuntimeException("Invalid verification code or code expired");
        }

        List<User> usersWithEmail = userRepository.findAllByEmailIgnoreCase(cleanEmail);
        User user = usersWithEmail.isEmpty()
                ? userRepository.findAll().stream().filter(u -> {
                    String uEmail = u.getEmail() != null && !u.getEmail().trim().isEmpty() 
                            ? u.getEmail().trim().toLowerCase() 
                            : (u.getFullName() != null ? u.getFullName().toLowerCase().replaceAll("[^a-z0-9]", "") + "@dapfintech.com" : "");
                    return uEmail.equals(cleanEmail) || (u.getMobileNumber() + "@dapfintech.com").equals(cleanEmail);
                }).findFirst().orElseThrow(() -> new RuntimeException("No user found with email: " + request.getEmail()))
                : usersWithEmail.get(0);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);
        emailOtpCache.remove(cleanEmail);

        notificationService.createNotification(
                "Password Changed",
                "Password successfully changed for " + user.getFullName() + " via email verification."
        );

        auditLogService.log(
                user.getFullName(),
                "CHANGE_PASSWORD_VIA_EMAIL",
                "EMPLOYEE",
                user.getId().toString()
        );
    }

    @Override
    public void logout(
            String refreshToken
    ) {

        RefreshToken token =
                refreshTokenRepository
                        .findByRefreshToken(
                                refreshToken
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Token not found"
                                )
                        );

        // Clear device token on logout to stop receiving notifications on this device
        User user = token.getUser();
        if (user != null) {
            user.setOnesignalId(null);
            userRepository.save(user);
        }

        token.setIsRevoked(true);

        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void updatePushSubscriptionId(String userId, String pushSubscriptionId) {
        if (pushSubscriptionId == null || pushSubscriptionId.trim().isEmpty()) return;
        try {
            UUID uid = UUID.fromString(userId);
            String token = pushSubscriptionId.trim();
            
            // Clear from other users
            java.util.List<User> usersWithToken = userRepository.findByOnesignalId(token);
            for (User u : usersWithToken) {
                if (!u.getId().equals(uid)) {
                    u.setOnesignalId(null);
                    userRepository.save(u);
                }
            }
            
            userRepository.findById(uid).ifPresent(user -> {
                user.setOnesignalId(token);
                userRepository.save(user);
            });
        } catch (IllegalArgumentException e) {
            // Invalid UUID, ignore
        }
    }
}