package com.dapfintech.notification.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.notification.dto.response.NotificationResponse;


import com.dapfintech.notification.dto.response.NotificationResponse;
import com.dapfintech.auth.entity.User;

public interface NotificationService {
	
	void createNotification(String title, String message);
	void createNotificationForUser(String title, String message, User targetUser);
	void notifyAllAdmins(String title, String message);
	
	List<NotificationResponse> getAllNotifications();
	void markAsRead(UUID notificationId);
}
