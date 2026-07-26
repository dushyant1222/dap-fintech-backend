package com.dapfintech.customer.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.dapfintech.customer.enums.CustomerStatus;
import com.dapfintech.customer.enums.Gender;

import lombok.Data;

@Data
public class UpdateCustomerRequest {
	
    private String firstName;

    private String lastName;

    private String alternateMobileNumber;

    private String email;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String aadhaarNumber;

    private String panNumber;

    private String occupation;

    private BigDecimal monthlyIncome;

    private CustomerStatus status;
    private UUID marketId;
}