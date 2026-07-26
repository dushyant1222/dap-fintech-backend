package com.dapfintech.customer.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.customer.dto.request.CreateCustomerRequest;
import com.dapfintech.customer.dto.response.CustomerResponse;
import com.dapfintech.customer.entity.Customer;

@Component
public class CustomerMapper {

    public Customer toEntity(
            CreateCustomerRequest request
    ) {

        return Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .mobileNumber(request.getMobileNumber())
                .alternateMobileNumber(
                        request.getAlternateMobileNumber()
                )
                .email(request.getEmail())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .aadhaarNumber(
                        request.getAadhaarNumber()
                )
                .panNumber(
                        request.getPanNumber()
                )
                .occupation(
                        request.getOccupation()
                )
                .monthlyIncome(
                        request.getMonthlyIncome()
                )
                .status(
                        request.getStatus()
                )
                .build();
    }

    public CustomerResponse toResponse(
            Customer customer
    ) {

        return CustomerResponse.builder()
                .id(customer.getId())
                .customerCode(
                        customer.getCustomerCode()
                )
                .firstName(
                        customer.getFirstName()
                )
                .lastName(
                        customer.getLastName()
                )
                .mobileNumber(
                        customer.getMobileNumber()
                )
                .alternateMobileNumber(
                        customer.getAlternateMobileNumber()
                )
                .email(customer.getEmail())
                .marketId(
                        customer.getMarket() != null
                                ? customer.getMarket().getId()
                                : null
                )

                .marketName(
                        customer.getMarket() != null
                                ? customer.getMarket().getMarketName()
                                : null
                )
                .dateOfBirth(
                        customer.getDateOfBirth()
                )
                .gender(customer.getGender())
                .aadhaarNumber(
                        customer.getAadhaarNumber()
                )
                .panNumber(
                        customer.getPanNumber()
                )
                .occupation(
                        customer.getOccupation()
                )
                .monthlyIncome(
                        customer.getMonthlyIncome()
                )
                .status(customer.getStatus())
                .createdByEmployeeId(
                        customer.getCreatedBy() != null
                                ? customer.getCreatedBy().getId()
                                : null
                )

                .createdByEmployeeName(
                        customer.getCreatedBy() != null
                                ? customer.getCreatedBy().getFullName()
                                : null
                )
                .build();
    }
}