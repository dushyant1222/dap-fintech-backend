package com.dapfintech.customer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.dapfintech.customer.enums.CustomerStatus;
import com.dapfintech.customer.enums.Gender;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerResponse {
	
	private UUID marketId;

	private String marketName;
	private UUID createdByEmployeeId;
	private String createdByEmployeeName;
    private UUID id;

    private String customerCode;

    private String firstName;

    private String lastName;

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
}