package com.dapfintech.notification.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.notification.dto.response.NotificationResponse;
import com.dapfintech.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
	
	private final NotificationService notificationService;
	
	@GetMapping
	public ResponseEntity<List<NotificationResponse>> getNotifications(){
		return ResponseEntity.ok(notificationService.getAllNotifications());
	}
	
	@PutMapping("/{notificationId}/read")
	public ResponseEntity<String> markAsRead(@PathVariable UUID notificationId){
		notificationService.markAsRead(notificationId);
		return ResponseEntity.ok("Notification mark as read");
	}

}
