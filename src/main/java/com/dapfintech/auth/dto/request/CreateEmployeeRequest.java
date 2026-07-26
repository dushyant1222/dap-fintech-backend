package com.dapfintech.auth.dto.request;

import lombok.Data;

@Data
public class CreateEmployeeRequest {

    private String fullName;

    private String mobileNumber;

    private String email;

    private String password;
}