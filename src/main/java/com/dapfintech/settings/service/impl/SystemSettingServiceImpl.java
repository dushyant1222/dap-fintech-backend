package com.dapfintech.settings.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.dapfintech.settings.dto.response.SystemSettingResponse;
import com.dapfintech.settings.entity.SystemSetting;
import com.dapfintech.settings.repository.SystemSettingRepository;
import com.dapfintech.settings.service.SystemSettingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl
        implements SystemSettingService {

    private final SystemSettingRepository
            repository;

    @Override
    public List<SystemSettingResponse>
    getAllSettings() {

        return repository.findAll()
                .stream()
                .map(
                        setting ->
                                SystemSettingResponse
                                        .builder()
                                        .settingKey(
                                                setting.getSettingKey()
                                        )
                                        .settingValue(
                                                setting.getSettingValue()
                                        )
                                        .build()
                )
                .toList();
    }

    @Override
    public void updateSetting(
            String key,
            String value
    ) {

        SystemSetting setting =
                repository
                        .findBySettingKey(
                                key
                        )
                        .orElse(
                                SystemSetting
                                        .builder()
                                        .settingKey(
                                                key
                                        )
                                        .build()
                        );

        setting.setSettingValue(
                value
        );

        repository.save(
                setting
        );
    }
}