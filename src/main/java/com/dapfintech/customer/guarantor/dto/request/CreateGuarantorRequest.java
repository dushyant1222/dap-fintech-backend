package com.dapfintech.customer.guarantor.dto.request;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateGuarantorRequest {

    private UUID customerId;

    private String guarantorName;

    private String mobileNumber;

    private String relationship;

    private String address;
}