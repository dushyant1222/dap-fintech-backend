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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
	
	private final NotificationRepository notificationRepository;
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

    private void sendOneSignalPush(String title, String message, String oneSignalPlayerId) {
        try {
            String url = "https://onesignal.com/api/v1/notifications";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + oneSignalRestApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("app_id", oneSignalAppId);
            body.put("include_player_ids", Collections.singletonList(oneSignalPlayerId));
            
            Map<String, String> headings = new HashMap<>();
            headings.put("en", title);
            body.put("headings", headings);
            
            Map<String, String> contents = new HashMap<>();
            contents.put("en", message);
            body.put("contents", contents);
            
            // Custom sound identifier corresponding to android/app/src/main/res/raw/sword.mp3
            body.put("android_sound", "sword");
            body.put("android_channel_id", "default");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
            log.info("Push notification sent to OneSignal ID: {}", oneSignalPlayerId);
        } catch (Exception e) {
            log.error("Failed to send OneSignal push notification: {}", e.getMessage());
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
