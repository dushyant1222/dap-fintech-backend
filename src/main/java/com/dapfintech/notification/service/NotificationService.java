package com.dapfintech.notification.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.notification.dto.response.NotificationResponse;


public interface NotificationService {
	
	void createNotification(String title, String message);
	
	List<NotificationResponse> getAllNotifications();
	void markAsRead(UUID notificationId);
}
