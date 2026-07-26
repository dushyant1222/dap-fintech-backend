package com.dapfintech.auth.dto.request;

import com.dapfintech.common.enums.UserStatus;

import lombok.Data;

@Data
public class UpdateEmployeeRequest {

    private String fullName;

    private String mobileNumber;

    private String email;

    private UserStatus status;
}