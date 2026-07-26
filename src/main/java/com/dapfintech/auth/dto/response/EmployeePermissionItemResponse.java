package com.dapfintech.auth.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok. NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePermissionItemResponse {

private UUID permissionId;

private String permissionKey;

private String moduleName;

private String description;

private Boolean allowed;
}