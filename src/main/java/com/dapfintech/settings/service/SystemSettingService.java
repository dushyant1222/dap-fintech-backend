package com.dapfintech.settings.service;

import java.util.List;

import com.dapfintech.settings.dto.response.SystemSettingResponse;

public interface SystemSettingService {

    List<SystemSettingResponse>
    getAllSettings();

    void updateSetting(
            String key,
            String value
    );
}