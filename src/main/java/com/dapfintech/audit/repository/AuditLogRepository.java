package com.dapfintech.audit.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.audit.entity.AuditLog;

@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findAllByOrderByActionTimeDesc();

    List<AuditLog> findByUserIdOrderByActionTimeDesc(UUID userId);
}