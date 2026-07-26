package com.dapfintech.customer.guarantor.dto.request;

import lombok.Data;

@Data
public class UpdateGuarantorRequest {

    private String guarantorName;

    private String mobileNumber;

    private String relationship;

    private String address;
}