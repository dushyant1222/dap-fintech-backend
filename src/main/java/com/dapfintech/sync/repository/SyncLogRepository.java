package com.dapfintech.sync.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dapfintech.sync.entity.SyncLog;

public interface SyncLogRepository
        extends JpaRepository<SyncLog, UUID> {

    List<SyncLog>
    findAllByOrderBySyncTimeDesc();
}