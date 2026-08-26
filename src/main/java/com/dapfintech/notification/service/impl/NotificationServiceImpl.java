package com.dapfintech.notification.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
		createNotification(title, message, "GENERAL", null);
	}

	@Override
	@Transactional
	public void createNotification(String title, String message, String navigationType, UUID referenceId) {
		Notification notification = Notification.builder()
				.title(title)
				.message(message)
				.isRead(false)
				.createdAt(LocalDateTime.now())
				.navigationType(navigationType != null ? navigationType : "GENERAL")
				.referenceId(referenceId)
				.build();
		notificationRepository.save(notification);
	}
    
    @Override
	public void createNotificationForUser(String title, String message, User targetUser) {
		createNotificationForUser(title, message, "GENERAL", null, targetUser);
	}

	@Override
	@Transactional
	public void createNotificationForUser(String title, String message, String navigationType, UUID referenceId, User targetUser) {
		Notification notification = Notification.builder()
				.title(title)
				.message(message)
				.isRead(false)
				.createdAt(LocalDateTime.now())
				.navigationType(navigationType != null ? navigationType : "GENERAL")
				.referenceId(referenceId)
				.build();
		notificationRepository.save(notification);

        if (targetUser != null && targetUser.getOnesignalId() != null && !targetUser.getOnesignalId().trim().isEmpty()) {
            sendOneSignalPush(title, message, navigationType, referenceId, targetUser.getOnesignalId());
        }
	}

    @Override
    public void notifyAllAdmins(String title, String message) {
		notifyAllAdmins(title, message, "GENERAL", null);
    }

	@Override
	@Transactional
    public void notifyAllAdmins(String title, String message, String navigationType, UUID referenceId) {
        createNotification(title, message, navigationType, referenceId);
        
        try {
            List<User> admins = userRepository.findByRoleRoleName("ADMIN");
            for (User admin : admins) {
                if (admin.getOnesignalId() != null && !admin.getOnesignalId().trim().isEmpty()) {
                    sendOneSignalPush(title, message, navigationType, referenceId, admin.getOnesignalId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to notify admins: {}", e.getMessage());
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
						.navigationType(n.getNavigationType())
						.referenceId(n.getReferenceId())
						.build()).toList();
	}
	
	@Override
	@Transactional
	public void markAsRead(UUID notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new RuntimeException("Notification not found"));
		notification.setIsRead(true);
		notificationRepository.save(notification);
	}

	@Override
	@Transactional
	public void markAllAsRead() {
		notificationRepository.markAllRead();
	}

	@Override
	@Transactional
	public void clearAll() {
		notificationRepository.deleteAll();
	}

	@Override
	@Transactional
	@Scheduled(fixedRate = 3600000) // every 1 hour
	public void clearOlderThan24Hours() {
		LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
		notificationRepository.deleteOlderThan(cutoff);
		log.info("Cleared notifications older than 24 hours");
	}

    private void sendOneSignalPush(String title, String message, String navigationType, UUID referenceId, String oneSignalSubscriptionId) {
        try {
            String url = "https://onesignal.com/api/v1/notifications";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Key " + oneSignalRestApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("app_id", oneSignalAppId);
            body.put("include_subscription_ids", Collections.singletonList(oneSignalSubscriptionId));
            body.put("target_channel", "push");
            
            Map<String, String> headings = new HashMap<>();
            headings.put("en", title);
            body.put("headings", headings);
            
            Map<String, String> contents = new HashMap<>();
            contents.put("en", message);
            body.put("contents", contents);

            // Custom data for deep linking on device
            Map<String, String> data = new HashMap<>();
            if (navigationType != null) data.put("navigationType", navigationType);
            if (referenceId != null) data.put("referenceId", referenceId.toString());
            if (!data.isEmpty()) body.put("data", data);
            
            body.put("android_sound", "sword");
            body.put("existing_android_channel_id", "sword_channel");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String responseBody = restTemplate.postForObject(url, request, String.class);
            log.info("[OneSignal] Push sent to subscription: {} | Response: {}", oneSignalSubscriptionId, responseBody);
        } catch (Exception e) {
            log.error("[OneSignal] Failed to send push notification to {}: {}", oneSignalSubscriptionId, e.getMessage());
        }
    }
}
