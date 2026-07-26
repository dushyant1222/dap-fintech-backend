package com.dapfintech.settings.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemSettingResponse {

    private String settingKey;

    private String settingValue;
}