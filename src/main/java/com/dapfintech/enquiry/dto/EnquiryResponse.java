package com.dapfintech.enquiry.dto;

import com.dapfintech.customer.enums.Gender;
import com.dapfintech.enquiry.enums.EnquiryStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class EnquiryResponse {
    private UUID id;
    private String fullName;
    private String fatherName;
    private String motherName;
    private String mobileNumber;
    private String alternateMobile;
    private String email;
    private LocalDate dob;
    private Gender gender;
    private String occupation;
    private String qualification;
    private String businessType;
    private String businessName;
    private BigDecimal annualIncome;
    private String referenceSource;
    private String currentAddress;
    private String permanentAddress;
    private Double gpsLatitude;
    private Double gpsLongitude;
    private String remarks;
    private EnquiryStatus status;
    private UUID employeeId;
    private String employeeName;
    private UUID marketId;
    private String marketName;
    private List<EnquiryMediaDto> media;
    private LocalDateTime createdAt;
   
}