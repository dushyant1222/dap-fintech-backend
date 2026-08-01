package com.dapfintech.enquiry.entity;

import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.customer.enums.Gender;
import com.dapfintech.auth.entity.User;
import com.dapfintech.enquiry.enums.EnquiryStatus;
import com.dapfintech.market.entity.Market;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "enquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "father_name", nullable = false, length = 150)
    private String fatherName;

    @Column(name = "mother_name", length = 150)
    private String motherName;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "alternate_mobile", length = 15)
    private String alternateMobile;

    @Column(length = 100)
    private String email;

    @Column
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(length = 100)
    private String occupation;

    @Column(length = 100)
    private String qualification;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "reference_source", length = 100)
    private String referenceSource;

    @Column(name = "gps_latitude", nullable = false)
    private Double gpsLatitude;

    @Column(name = "gps_longitude", nullable = false)
    private Double gpsLongitude;

    @Column(name = "loan_demand_amount", precision = 15, scale = 2)
    private BigDecimal loanDemandAmount;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EnquiryStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "current_address_id", nullable = false)
    private EnquiryAddress currentAddress;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "permanent_address_id", nullable = false)
    private EnquiryAddress permanentAddress;

    @OneToMany(mappedBy = "enquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EnquiryMedia> mediaList = new ArrayList<>();

    @OneToMany(mappedBy = "enquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EnquiryHistory> historyList = new ArrayList<>();

    public void addMedia(EnquiryMedia media) {
        mediaList.add(media);
        media.setEnquiry(this);
    }

    public void addHistory(EnquiryHistory history) {
        historyList.add(history);
        history.setEnquiry(this);
    }
  
}