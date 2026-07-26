package com.dapfintech.settings.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dapfintech.settings.entity.SystemSetting;

public interface SystemSettingRepository
        extends JpaRepository<
                SystemSetting,
                UUID> {

    Optional<SystemSetting>
    findBySettingKey(
            String settingKey
    );
}