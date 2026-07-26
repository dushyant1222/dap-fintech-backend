package com.dapfintech.settings.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.settings.dto.request.UpdateSystemSettingRequest;
import com.dapfintech.settings.dto.response.SystemSettingResponse;
import com.dapfintech.settings.service.SystemSettingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService
            systemSettingService;

    @GetMapping
    public ResponseEntity<
            List<SystemSettingResponse>>
    getAllSettings() {

        return ResponseEntity.ok(
                systemSettingService
                        .getAllSettings()
        );
    }

    @PutMapping
    public ResponseEntity<String>
    updateSetting(
            @RequestBody
            UpdateSystemSettingRequest request
    ) {

        systemSettingService
                .updateSetting(
                        request.getSettingKey(),
                        request.getSettingValue()
                );

        return ResponseEntity.ok(
                "Setting updated successfully"
        );
    }
}