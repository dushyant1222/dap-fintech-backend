package com.dapfintech.notification.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Service;

import com.dapfintech.notification.service.EmailService;

import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnMissingClass("org.springframework.mail.javamail.JavaMailSender")
@Slf4j
public class NoOpEmailServiceImpl implements EmailService {

    @Override
    public void sendVerificationOtp(String toEmail, String fullName, String otpCode) {
        log.warn("========================================================================================");
        log.warn("JavaMailSender class not found in classpath (Eclipse dependency not updated).");
        log.warn("Please right-click the project in Eclipse -> Maven -> Update Project (Alt+F5) to download spring-boot-starter-mail.");
        log.warn("FALLBACK OTP FOR [{}] ({}): {}", toEmail, fullName, otpCode);
        log.warn("========================================================================================");
    }
}
