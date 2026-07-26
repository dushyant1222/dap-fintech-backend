package com.dapfintech.auth.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePermissionsResponse {

    private UUID employeeId;

    private String employeeName;

    private List<EmployeePermissionItemResponse> permissions;
}