package com.dapfintech.customer.guarantor.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.customer.guarantor.dto.request.CreateGuarantorRequest;
import com.dapfintech.customer.guarantor.dto.request.UpdateGuarantorRequest;
import com.dapfintech.customer.guarantor.dto.response.GuarantorResponse;

public interface GuarantorService {

    GuarantorResponse createGuarantor(
            CreateGuarantorRequest request
    );

    GuarantorResponse updateGuarantor(
            UUID guarantorId,
            UpdateGuarantorRequest request
    );

    GuarantorResponse getGuarantorById(
            UUID guarantorId
    );

    List<GuarantorResponse> getCustomerGuarantors(
            UUID customerId
    );

    void deleteGuarantor(
            UUID guarantorId
    );
}