package com.dapfintech.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.notification.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	
	List<Notification> findAllByOrderByCreatedAtDesc();

	@Modifying
	@Transactional
	@Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
	void deleteOlderThan(LocalDateTime cutoff);

	@Modifying
	@Transactional
	@Query("UPDATE Notification n SET n.isRead = true")
	void markAllRead();
}
