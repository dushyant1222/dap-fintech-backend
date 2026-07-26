package com.dapfintech.sync.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dapfintech.sync.dto.response.SyncLogResponse;
import com.dapfintech.sync.entity.SyncLog;
import com.dapfintech.sync.enums.SyncStatus;
import com.dapfintech.sync.repository.SyncLogRepository;
import com.dapfintech.sync.service.SyncLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SyncLogServiceImpl
        implements SyncLogService {

    private final SyncLogRepository
            syncLogRepository;


    @Override
    public void logSync(
            String entityType,
            String entityId
    ) {

        SyncLog log =
                SyncLog.builder()
                        .entityType(entityType)
                        .entityId(entityId)
                        .syncStatus(
                                SyncStatus.SYNCED
                        )
                        .syncTime(
                                LocalDateTime.now()
                        )
                        .build();

        syncLogRepository.save(log);
    }
    
    @Override
    public List<SyncLogResponse>
    getAllSyncLogs() {

        return syncLogRepository
                .findAllByOrderBySyncTimeDesc()
                .stream()
                .map(
                        log ->
                                SyncLogResponse
                                        .builder()
                                        .id(
                                                log.getId()
                                        )
                                        .entityType(
                                                log.getEntityType()
                                        )
                                        .entityId(
                                                log.getEntityId()
                                        )
                                        .syncStatus(
                                                log.getSyncStatus()
                                        )
                                        .syncTime(
                                                log.getSyncTime()
                                        )
                                        .build()
                )
                .toList();
    }
}