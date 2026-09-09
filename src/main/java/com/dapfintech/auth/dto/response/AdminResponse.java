package com.dapfintech.auth.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResponse {

    private UUID id;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String role;
    private String status;
    private Boolean isMasterAdmin;
}
