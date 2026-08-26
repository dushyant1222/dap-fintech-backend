package com.dapfintech.notification.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.notification.dto.response.NotificationResponse;
import com.dapfintech.auth.entity.User;

public interface NotificationService {
	
	void createNotification(String title, String message);
	void createNotification(String title, String message, String navigationType, UUID referenceId);
	void createNotificationForUser(String title, String message, User targetUser);
	void createNotificationForUser(String title, String message, String navigationType, UUID referenceId, User targetUser);
	void notifyAllAdmins(String title, String message);
	void notifyAllAdmins(String title, String message, String navigationType, UUID referenceId);
	
	List<NotificationResponse> getAllNotifications();
	void markAsRead(UUID notificationId);
	void markAllAsRead();
	void clearAll();
	void clearOlderThan24Hours();
}
