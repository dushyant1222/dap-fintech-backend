package com.dapfintech.loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.auth.entity.User;
import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.loan.enums.InterestType;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.enums.RepaymentFrequency;
import com.dapfintech.loan.enums.TenureUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name="loans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan extends BaseEntity {
	
	 	@Id
	 	@GeneratedValue(strategy = GenerationType.UUID)
	    private UUID id;

	    @ManyToOne
	    @JoinColumn(name = "customer_id")
	    private Customer customer;

	    @Enumerated(EnumType.STRING)
	    @Column(name = "loan_type")
	    private LoanType loanType;

	    @Column(name = "loan_amount")
	    private BigDecimal loanAmount;

	    @Column(name = "approved_amount")
	    private BigDecimal approvedAmount;

	    @Column(name = "disbursed_amount")
	    private BigDecimal disbursedAmount;
	    
	    @Column(name = "duration_in_days")
	    private Integer durationInDays;
	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "tenure_unit")
	    private TenureUnit tenureUnit;

	    @Enumerated(EnumType.STRING)
	    @Column(name = "interest_type")
	    private InterestType interestType;

	    @Column(name = "interest_rate")
	    private BigDecimal interestRate;

	    private Integer tenure;

	    @Enumerated(EnumType.STRING)
	    @Column(name = "repayment_frequency")
	    private RepaymentFrequency repaymentFrequency;

	    @Enumerated(EnumType.STRING)
	    @Column(name = "loan_status")
	    private LoanStatus loanStatus;

	    @Column(name = "application_date")
	    private LocalDateTime applicationDate;

	    @Column(name = "approval_date")
	    private LocalDateTime approvalDate;

	    @Column(name = "disbursement_date")
	    private LocalDateTime disbursementDate;
	    

	    
	    @ManyToOne
	    @JoinColumn(name = "created_by")
	    private User createdBy;

	    @Column(name = "penalty_rate")
	    private BigDecimal penaltyRate;

	    @Column(name = "penalty_waived_percent")
	    private BigDecimal penaltyWaivedPercent;

	    @Column(name = "closed_special_condition")
	    private Boolean closedSpecialCondition;

	    @Column(name = "special_closure_remarks")
	    private String specialClosureRemarks;
}
