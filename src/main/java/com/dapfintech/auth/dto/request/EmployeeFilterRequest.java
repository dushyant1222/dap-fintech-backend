package com.dapfintech.auth.dto.request;

import java.util.UUID;

import com.dapfintech.common.enums.UserStatus;

import lombok.Data;

@Data
public class EmployeeFilterRequest {

    private String keyword;

    private UserStatus status;

    private UUID marketId;
}