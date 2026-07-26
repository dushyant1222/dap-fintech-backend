package com.dapfintech.auth.dto.request;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeePermissionItemRequest {

    private UUID permissionId;

    private Boolean allowed;
}