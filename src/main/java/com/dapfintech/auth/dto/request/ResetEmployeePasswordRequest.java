package com.dapfintech.auth.dto.request;

import java.util.UUID;

import lombok.Data;

@Data
public class ResetEmployeePasswordRequest {

    private UUID employeeId;

    private String newPassword;
}