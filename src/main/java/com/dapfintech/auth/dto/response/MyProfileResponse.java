package com.dapfintech.auth.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MyProfileResponse {

    private UUID id;

    private String fullName;

    private String mobileNumber;

    private String email;

    private String role;

    private String status;

}