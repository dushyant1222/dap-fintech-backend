package com.dapfintech.settings.dto.request;

import lombok.Data;

@Data
public class UpdateSystemSettingRequest {

    private String settingKey;

    private String settingValue;
}