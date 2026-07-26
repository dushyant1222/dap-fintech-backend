package com.dapfintech.notification.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
	
	private UUID id;
	private String title;
	private String message;
	private Boolean isRead;
	private LocalDateTime createdAt;

}
