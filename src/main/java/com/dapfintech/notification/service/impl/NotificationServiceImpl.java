package com.dapfintech.notification.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.dapfintech.notification.dto.response.NotificationResponse;
import com.dapfintech.notification.entity.Notification;
import com.dapfintech.notification.repository.NotificationRepository;
import com.dapfintech.notification.service.NotificationService;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
	
	private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${onesignal.app-id}")
    private String oneSignalAppId;

    @Value("${onesignal.rest-api-key}")
    private String oneSignalRestApiKey;
	
	@Override
	public void createNotification(String title, String message) {
		Notification notification = Notification.builder()
				.title(title)
				.message(message)
				.isRead(false)
				.createdAt(LocalDateTime.now())
				.build();
		notificationRepository.save(notification);
	}
    
    @Override
	public void createNotificationForUser(String title, String message, User targetUser) {
        // Save to DB
		Notification notification = Notification.builder()
				.title(title)
				.message(message)
				.isRead(false)
				.createdAt(LocalDateTime.now())
				.build();
		notificationRepository.save(notification);

        // Send Push Notification via OneSignal
        if (targetUser != null && targetUser.getOnesignalId() != null && !targetUser.getOnesignalId().trim().isEmpty()) {
            sendOneSignalPush(title, message, targetUser.getOnesignalId());
        }
	}

    @Override
    public void notifyAllAdmins(String title, String message) {
        // Log global notification in DB
        createNotification(title, message);
        
        // Push individually to all Admins
        try {
            List<User> admins = userRepository.findByRoleRoleName("ADMIN");
            for (User admin : admins) {
                if (admin.getOnesignalId() != null && !admin.getOnesignalId().trim().isEmpty()) {
                    sendOneSignalPush(title, message, admin.getOnesignalId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to notify admins: {}", e.getMessage());
        }
    }

    private void sendOneSignalPush(String title, String message, String oneSignalSubscriptionId) {
        try {
            String url = "https://onesignal.com/api/v1/notifications";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // os_v2_app_ keys use 'Key ' prefix (NOT 'Basic ')
            headers.set("Authorization", "Key " + oneSignalRestApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("app_id", oneSignalAppId);
            // OneSignal SDK v5 uses Subscription IDs — use include_subscription_ids
            body.put("include_subscription_ids", Collections.singletonList(oneSignalSubscriptionId));
            body.put("target_channel", "push");
            
            Map<String, String> headings = new HashMap<>();
            headings.put("en", title);
            body.put("headings", headings);
            
            Map<String, String> contents = new HashMap<>();
            contents.put("en", message);
            body.put("contents", contents);
            
            // Custom sound: sword.mp3 in res/raw/
            body.put("android_sound", "sword");
            body.put("android_channel_id", "sword_channel");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String responseBody = restTemplate.postForObject(url, request, String.class);
            log.info("[OneSignal] Push sent to subscription: {} | Response: {}", oneSignalSubscriptionId, responseBody);
        } catch (Exception e) {
            log.error("[OneSignal] Failed to send push notification to {}: {}", oneSignalSubscriptionId, e.getMessage());
        }
    }
	
	@Override
	public List<NotificationResponse> getAllNotifications(){
		return notificationRepository.findAllByOrderByCreatedAtDesc()
				.stream()
				.map(n -> NotificationResponse
						.builder()
						.id(n.getId())
						.title(n.getTitle())
						.message(n.getMessage())
						.isRead(n.getIsRead())
						.createdAt(n.getCreatedAt())
						.build()).toList();
	}
	
	@Override
	public void markAsRead(UUID notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new RuntimeException("Notification not found"));
		notification.setIsRead(true);
		notificationRepository.save(notification);
	}

}
