package com.dapfintech.auth.dto.response;

import java.util.UUID;

import com.dapfintech.common.enums.UserStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeResponse {

    private UUID id;

    private String fullName;

    private String mobileNumber;

    private String email;

    private String role;

    private UserStatus status;
}