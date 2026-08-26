package com.dapfintech.notification.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
		return ResponseEntity.ok("Notification marked as read");
	}

	@PutMapping("/read-all")
	public ResponseEntity<String> markAllAsRead(){
		notificationService.markAllAsRead();
		return ResponseEntity.ok("All notifications marked as read");
	}

	@DeleteMapping
	public ResponseEntity<String> clearAll(){
		notificationService.clearAll();
		return ResponseEntity.ok("All notifications cleared");
	}
}
