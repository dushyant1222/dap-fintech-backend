package com.dapfintech.customer.guarantor.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dapfintech.customer.history.enums.CustomerHistoryAction;
import com.dapfintech.customer.history.service.CustomerHistoryService;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.entity.CustomerGuarantor;
import com.dapfintech.customer.guarantor.dto.request.CreateGuarantorRequest;
import com.dapfintech.customer.guarantor.dto.request.UpdateGuarantorRequest;
import com.dapfintech.customer.guarantor.dto.response.GuarantorResponse;
import com.dapfintech.customer.guarantor.mapper.GuarantorMapper;
import com.dapfintech.customer.guarantor.service.GuarantorService;
import com.dapfintech.customer.repository.CustomerGuarantorRepository;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.security.service.AccessControlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuarantorServiceImpl
        implements GuarantorService {

    private final CustomerRepository customerRepository;

    private final CustomerGuarantorRepository guarantorRepository;

    private final GuarantorMapper guarantorMapper;
    private final CustomerHistoryService customerHistoryService;
    private final AccessControlService accessControlService;


    // =========================================================
    // CREATE GUARANTOR
    // =========================================================

    @Override
    @Transactional
    public GuarantorResponse createGuarantor(
            CreateGuarantorRequest request
    ) {

        if (request.getCustomerId() == null) {
            throw new RuntimeException(
                    "Customer ID is required"
            );
        }

        accessControlService.validateCustomerAccess(
                request.getCustomerId()
        );

        Customer customer =
                customerRepository.findById(
                        request.getCustomerId()
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Customer not found"
                        )
                );

        CustomerGuarantor guarantor =
                CustomerGuarantor.builder()
                        .customer(customer)
                        .guarantorName(
                                cleanRequiredText(
                                        request.getGuarantorName(),
                                        "Guarantor name"
                                )
                        )
                        .mobileNumber(
                                cleanRequiredText(
                                        request.getMobileNumber(),
                                        "Mobile number"
                                )
                        )
                        .relationship(
                                cleanRequiredText(
                                        request.getRelationship(),
                                        "Relationship"
                                )
                        )
                        .address(
                                cleanRequiredText(
                                        request.getAddress(),
                                        "Address"
                                )
                        )
                        .build();

        CustomerGuarantor savedGuarantor =
                guarantorRepository.save(guarantor);

      

        customerHistoryService.recordHistory(

                customer,

                CustomerHistoryAction.GUARANTOR_CREATED,

                "Guarantor Added",

                savedGuarantor.getGuarantorName() +
                        " was added as guarantor.",

                null,

                savedGuarantor.getGuarantorName()
        );

        return guarantorMapper.toResponse(
                savedGuarantor
        );
    }


    // =========================================================
    // UPDATE GUARANTOR
    // =========================================================

    @Override
    @Transactional
    public GuarantorResponse updateGuarantor(
            UUID guarantorId,
            UpdateGuarantorRequest request
    ) {

        CustomerGuarantor guarantor =
                findAccessibleGuarantor(
                        guarantorId
                );
        String oldValue =
                "Name: " +
                guarantor.getGuarantorName() +
                ", Mobile: " +
                guarantor.getMobileNumber() +
                ", Relationship: " +
                guarantor.getRelationship();

        guarantor.setGuarantorName(
                cleanRequiredText(
                        request.getGuarantorName(),
                        "Guarantor name"
                )
        );

        guarantor.setMobileNumber(
                cleanRequiredText(
                        request.getMobileNumber(),
                        "Mobile number"
                )
        );

        guarantor.setRelationship(
                cleanRequiredText(
                        request.getRelationship(),
                        "Relationship"
                )
        );

        guarantor.setAddress(
                cleanRequiredText(
                        request.getAddress(),
                        "Address"
                )
        );

        CustomerGuarantor updatedGuarantor =
                guarantorRepository.save(
                        guarantor
                );
        String newValue =
                "Name: " +
                updatedGuarantor.getGuarantorName() +
                ", Mobile: " +
                updatedGuarantor.getMobileNumber() +
                ", Relationship: " +
                updatedGuarantor.getRelationship();
        
        customerHistoryService.recordHistory(

                updatedGuarantor.getCustomer(),

                CustomerHistoryAction.GUARANTOR_UPDATED,

                "Guarantor Updated",

                "Guarantor information was updated.",

                oldValue,

                newValue
        );
        

        return guarantorMapper.toResponse(
                updatedGuarantor
        );
    }


    // =========================================================
    // GET GUARANTOR BY ID
    // =========================================================

    @Override
    public GuarantorResponse getGuarantorById(
            UUID guarantorId
    ) {

        CustomerGuarantor guarantor =
                findAccessibleGuarantor(
                        guarantorId
                );

        return guarantorMapper.toResponse(
                guarantor
        );
    }


    // =========================================================
    // GET ALL GUARANTORS FOR CUSTOMER
    // =========================================================

    @Override
    public List<GuarantorResponse> getCustomerGuarantors(
            UUID customerId
    ) {

        accessControlService.validateCustomerAccess(
                customerId
        );

        if (!customerRepository.existsById(customerId)) {
            throw new RuntimeException(
                    "Customer not found"
            );
        }

        return guarantorRepository
                .findByCustomerId(customerId)
                .stream()
                .map(guarantorMapper::toResponse)
                .toList();
    }


    // =========================================================
    // DELETE GUARANTOR
    // =========================================================

    @Override
    @Transactional
    public void deleteGuarantor(
            UUID guarantorId
    ) {

        CustomerGuarantor guarantor =
                guarantorRepository.findById(
                        guarantorId
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Guarantor not found"
                        )
                );

        accessControlService.validateCustomerAccess(
                guarantor.getCustomer().getId()
        );

        customerHistoryService.recordHistory(

                guarantor.getCustomer(),

                CustomerHistoryAction.GUARANTOR_DELETED,

                "Guarantor Deleted",

                guarantor.getGuarantorName() +
                        " was removed as guarantor.",

                guarantor.getGuarantorName(),

                null
        );

        guarantorRepository.delete(
                guarantor
        );
    }


    // =========================================================
    // PRIVATE HELPER — FIND + ACCESS VALIDATION
    // =========================================================

    private CustomerGuarantor findAccessibleGuarantor(
            UUID guarantorId
    ) {

        if (guarantorId == null) {
            throw new RuntimeException(
                    "Guarantor ID is required"
            );
        }

        CustomerGuarantor guarantor =
                guarantorRepository.findById(
                        guarantorId
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Guarantor not found"
                        )
                );

        accessControlService.validateCustomerAccess(
                guarantor.getCustomer().getId()
        );

        return guarantor;
    }


    // =========================================================
    // PRIVATE HELPER — BASIC VALIDATION
    // =========================================================

    private String cleanRequiredText(
            String value,
            String fieldName
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            throw new RuntimeException(
                    fieldName + " is required"
            );
        }

        return value.trim();
    }
}