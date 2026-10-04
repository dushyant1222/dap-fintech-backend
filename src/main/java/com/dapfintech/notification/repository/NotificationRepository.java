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

	@Query("SELECT n FROM Notification n WHERE (n.targetUser.id = :userId OR n.targetRole = :role OR (n.targetUser IS NULL AND n.targetRole IS NULL)) ORDER BY n.createdAt DESC")
	List<Notification> findForUserOrRole(UUID userId, String role);

	@Query("SELECT n FROM Notification n WHERE (n.targetRole = 'ADMIN' OR (n.targetUser IS NULL AND n.targetRole IS NULL)) ORDER BY n.createdAt DESC")
	List<Notification> findForAdmin();

	@Modifying
	@Transactional
	@Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
	void deleteOlderThan(LocalDateTime cutoff);

	@Modifying
	@Transactional
	@Query("UPDATE Notification n SET n.isRead = true")
	void markAllRead();
}
