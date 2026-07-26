package com.dapfintech.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.auth.entity.Permission;

@Repository
public interface PermissionRepository
        extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByPermissionKey(
            String permissionKey
    );

    List<Permission> findAllByOrderByModuleNameAscPermissionKeyAsc();
}