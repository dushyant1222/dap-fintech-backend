package com.dapfintech.permission.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dapfintech.permission.entity.UserPermission;

public interface UserPermissionRepository
        extends JpaRepository<UserPermission, UUID> {

    List<UserPermission> findByUser_Id(UUID userId);
}