package com.dapfintech.enquiry.dto;

import com.dapfintech.customer.enums.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class EnquiryRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Father name is required")
    private String fatherName;

    private String motherName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Invalid mobile number format")
    private String mobileNumber;

    @Pattern(regexp = "^(\\d{10})?$", message = "Invalid alternate mobile format")
    private String alternateMobile;

    @Email(message = "Invalid email format")
    private String email;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private String occupation;
    private String qualification;
    private String businessType;
    private String businessName;

    @Positive(message = "Annual income must be positive")
    private BigDecimal annualIncome;

    private String referenceSource;

    @NotBlank(message = "Current address is required")
    private String currentAddress;

    @NotBlank(message = "Permanent address is required")
    private String permanentAddress;

    @NotNull(message = "GPS Latitude is required")
    private Double gpsLatitude;

    @NotNull(message = "GPS Longitude is required")
    private Double gpsLongitude;

    @Positive(message = "Loan demand amount must be positive")
    private BigDecimal loanDemandAmount;

    private String remarks;

    @NotNull(message = "Market ID is required")
    private UUID marketId;

    @Valid
    private List<EnquiryMediaDto> media;
}