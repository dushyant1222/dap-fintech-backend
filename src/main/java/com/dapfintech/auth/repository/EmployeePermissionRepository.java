package com.dapfintech.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.auth.entity.EmployeePermission;

@Repository
public interface EmployeePermissionRepository
        extends JpaRepository<EmployeePermission, UUID> {

    List<EmployeePermission> findByEmployeeId(
            UUID employeeId
    );

    Optional<EmployeePermission>
    findByEmployeeIdAndPermissionId(
            UUID employeeId,
            UUID permissionId
    );

    boolean existsByEmployeeIdAndPermissionPermissionKeyAndAllowedTrue(
            UUID employeeId,
            String permissionKey
    );
}