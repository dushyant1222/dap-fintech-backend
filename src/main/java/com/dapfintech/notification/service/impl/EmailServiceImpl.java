package com.dapfintech.notification.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.dapfintech.notification.service.EmailService;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnClass(name = "org.springframework.mail.javamail.JavaMailSender")
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendVerificationOtp(String toEmail, String fullName, String otpCode) {
        log.info("Preparing to send verification OTP [{}] to email [{}]", otpCode, toEmail);

        if (mailSender == null || fromEmail == null || fromEmail.trim().isEmpty()) {
            log.warn("========================================================================================");
            log.warn("SMTP credentials not configured (spring.mail.username is empty). Email NOT sent via SMTP.");
            log.warn("To enable live email delivery, configure spring.mail.username and spring.mail.password in application.yml!");
            log.warn("FALLBACK OTP FOR [{}] ({}): {}", toEmail, fullName, otpCode);
            log.warn("========================================================================================");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "DAP Fintech Security");
            helper.setTo(toEmail);
            helper.setSubject("Your Verification Code - DAP Fintech");

            String htmlContent = String.format(
                "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px; background-color: #f9fbfd;'>"
                + "<div style='text-align: center; padding-bottom: 15px; border-bottom: 2px solid #063169;'>"
                + "<h2 style='color: #063169; margin: 0;'>DAP Fintech</h2>"
                + "</div>"
                + "<div style='padding: 20px 0;'>"
                + "<p style='font-size: 16px; color: #333;'>Hello <b>%s</b>,</p>"
                + "<p style='font-size: 15px; color: #555; line-height: 1.5;'>We received a request to change or verify the password for your DAP Fintech account associated with this email address.</p>"
                + "<div style='text-align: center; margin: 25px 0;'>"
                + "<span style='display: inline-block; font-size: 28px; font-weight: bold; letter-spacing: 5px; color: #063169; background-color: #e8f0fe; padding: 12px 28px; border-radius: 8px; border: 1px dashed #063169;'>%s</span>"
                + "</div>"
                + "<p style='font-size: 14px; color: #666; text-align: center;'>This verification code will expire shortly. Do not share this code with anyone.</p>"
                + "</div>"
                + "<div style='border-top: 1px solid #e0e0e0; padding-top: 15px; text-align: center; font-size: 12px; color: #888;'>"
                + "<p style='margin: 0;'>If you did not request this change, please ignore this email or contact support.</p>"
                + "<p style='margin: 5px 0 0;'>&copy; 2026 DAP Fintech. All rights reserved.</p>"
                + "</div>"
                + "</div>",
                fullName != null && !fullName.trim().isEmpty() ? fullName : "User",
                otpCode
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Verification OTP email successfully sent to [{}]", toEmail);
        } catch (Exception e) {
            log.error("Failed to send SMTP email to [{}]: {}", toEmail, e.getMessage());
            log.warn("FALLBACK OTP FOR [{}] ({}): {}", toEmail, fullName, otpCode);
        }
    }
}
