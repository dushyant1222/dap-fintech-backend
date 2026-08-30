package com.dapfintech.customer.entity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.auth.entity.User;
import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.customer.enums.CustomerStatus;
import com.dapfintech.customer.enums.Gender;
import com.dapfintech.market.entity.Market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name="customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(name = "customer_code", unique = true)
	private String customerCode;
	
	@Column(name="first_name")
	private String firstName;
	
	@Column(name="last_name")
	private String lastName;
	
	@Column(name="mobile_number")
	private String mobileNumber;
	
	@Column(name = "alternate_mobile_number")
	private String alternateMobileNumber;
	
	@Column(name = "email")
	private String email;

	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;
	
	@Enumerated(EnumType.STRING)
	@Column(name="gender")
	private Gender gender;
	
	@Column(name="aadhaar_number")
	private String aadhaarNumber;
	
	@Column(name = "pan_number")
    private String panNumber;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "monthly_income")
    private BigDecimal monthlyIncome;
    
    @Enumerated(EnumType.STRING)
    @Column(name="status")
    private CustomerStatus status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id")
    private Market market;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="Created_by")
    private User createdBy;
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;


    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @Column(name = "deleted_by")
    private UUID deletedBy;
	public String getFullName() {
		return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
	}
}

