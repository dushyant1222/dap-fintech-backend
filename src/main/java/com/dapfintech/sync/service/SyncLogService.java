package com.dapfintech.sync.service;

import java.util.List;

import com.dapfintech.sync.dto.response.SyncLogResponse;

public interface SyncLogService {

    void logSync(
            String entityType,
            String entityId
    );

    List<SyncLogResponse>
    getAllSyncLogs();
}