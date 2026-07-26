package com.dapfintech.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.permission.entity.RolePermission;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, String> {

	List<RolePermission> findByRole_Id(String roleId);

}
