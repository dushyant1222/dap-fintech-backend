package com.dapfintech.customer.guarantor.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.customer.entity.CustomerGuarantor;
import com.dapfintech.customer.guarantor.dto.response.GuarantorResponse;

@Component
public class GuarantorMapper {

    public GuarantorResponse toResponse(
            CustomerGuarantor guarantor
    ) {

        return GuarantorResponse.builder()
                .id(guarantor.getId())
                .customerId(
                        guarantor.getCustomer().getId()
                )
                .guarantorName(
                        guarantor.getGuarantorName()
                )
                .mobileNumber(
                        guarantor.getMobileNumber()
                )
                .relationship(
                        guarantor.getRelationship()
                )
                .address(
                        guarantor.getAddress()
                )
                .build();
    }
}