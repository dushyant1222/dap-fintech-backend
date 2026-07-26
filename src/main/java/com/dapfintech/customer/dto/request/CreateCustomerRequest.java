package com.dapfintech.customer.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.dapfintech.customer.enums.CustomerStatus;
import com.dapfintech.customer.enums.Gender;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCustomerRequest {
	
    @NotBlank
    private String firstName;

    private String lastName;

    @NotBlank
    private String mobileNumber;

    private String alternateMobileNumber;

    private String email;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String aadhaarNumber;

    private String panNumber;

    private String occupation;

    private BigDecimal monthlyIncome;

    private CustomerStatus status;
    private String currentAddress;
    private String permanentAddress;
    private String city;
    private String state;
    private String pincode;
    private UUID marketId;
}