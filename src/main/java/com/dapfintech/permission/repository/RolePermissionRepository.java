package com.dapfintech.permission.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.dapfintech.permission.entity.RolePermission;

@Repository
public interface RolePermissionRepository {

	List<RolePermission> findByRole_Id(String roleId);
}
