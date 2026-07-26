package com.dapfintech.sync.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.sync.dto.response.SyncLogResponse;
import com.dapfintech.sync.service.SyncLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncLogService
            syncLogService;

    @GetMapping("/logs")
    public ResponseEntity<
            List<SyncLogResponse>>
    getSyncLogs() {

        return ResponseEntity.ok(
                syncLogService
                        .getAllSyncLogs()
        );
    }
}