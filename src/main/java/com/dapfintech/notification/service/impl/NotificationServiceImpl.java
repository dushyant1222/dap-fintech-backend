package com.dapfintech.notification.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dapfintech.notification.dto.response.NotificationResponse;
import com.dapfintech.notification.entity.Notification;
import com.dapfintech.notification.repository.NotificationRepository;
import com.dapfintech.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
	
	private final NotificationRepository notificationRepository;
	
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
