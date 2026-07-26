package com.dapfintech.customer.guarantor.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GuarantorResponse {

    private UUID id;

    private UUID customerId;

    private String guarantorName;

    private String mobileNumber;

    private String relationship;

    private String address;
}