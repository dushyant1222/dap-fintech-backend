package com.dapfintech.loan.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.market.entity.EmployeeMarketAssignment;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;

import com.dapfintech.loan.dto.request.LoanFilterRequest;
import com.dapfintech.loan.specification.LoanSpecification;

import org.springframework.stereotype.Service;

import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.loan.dto.request.CalculateEmiRequest;
import com.dapfintech.loan.dto.request.CreateLoanRequest;
import com.dapfintech.loan.dto.request.UpdateLoanRequest;
import com.dapfintech.loan.dto.response.CalculateEmiResponse;
import com.dapfintech.loan.dto.response.LoanResponse;
import com.dapfintech.loan.dto.response.LoanStatisticsResponse;
import com.dapfintech.loan.dto.response.LoanSummaryResponse;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanCharge;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.ChargeType;
import com.dapfintech.loan.enums.InterestType;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.mapper.LoanMapper;
import com.dapfintech.loan.repository.LoanChargeRepository;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.service.LoanService;
import com.dapfintech.security.service.AccessControlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl
        implements LoanService {

    private final LoanRepository loanRepository;

    private final CustomerRepository customerRepository;

    private final LoanMapper loanMapper;
    private final LoanCollectionRepository collectionRepository;

    private final LoanRepaymentScheduleRepository repaymentScheduleRepository;
    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final LoanChargeRepository loanChargeRepository;
    private final EmployeeMarketAssignmentRepository assignmentRepository;
    
    
    
    @Override
    public Page<LoanResponse> filterLoans(
            LoanFilterRequest filter,
            int page,
            int size
    ) {

        //----------------------------------------------------------
        // AUTHENTICATED USER
        //----------------------------------------------------------

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User user =
                userRepository
                        .findByMobileNumber(
                                mobileNumber
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );


        //----------------------------------------------------------
        // PAGE REQUEST
        //----------------------------------------------------------

        PageRequest pageable =
                PageRequest.of(

                        page,

                        size,

                        Sort.by(
                                Sort.Direction.DESC,
                                "applicationDate"
                        )

                );


        //----------------------------------------------------------
        // ADMIN
        //----------------------------------------------------------

        if (user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            return loanRepository

                    .findAll(

                            LoanSpecification
                                    .withFilters(filter),

                            pageable

                    )

                    .map(
                            loanMapper::toResponse
                    );

        }


        //----------------------------------------------------------
        // EMPLOYEE MARKET ACCESS
        //----------------------------------------------------------

        EmployeeMarketAssignment assignment =
                assignmentRepository
                        .findFirstByEmployeeIdAndIsActiveTrue(
                                user.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No market assigned"
                                )
                        );


        /*
         * For employee-side filtered search, market restriction must
         * also be applied. We will keep this admin Loan Management
         * endpoint admin-focused for this commit.
         */

        throw new RuntimeException(
                "Advanced loan filtering is currently available for admin only"
        );
    }
    
    private void createCharge(
            Loan loan,
            ChargeType type,
            BigDecimal amount,
            boolean mandatory
    ) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        LoanCharge charge = LoanCharge.builder()
                .loan(loan)
                .chargeType(type)
                .chargeAmount(amount)
                .isMandatory(mandatory)
                .build();

        loanChargeRepository.save(charge);
    }
    
    
    @Override
    public LoanStatisticsResponse getLoanStatistics() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User user =
                userRepository
                        .findByMobileNumber(
                                mobileNumber
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        if (user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            return LoanStatisticsResponse
                    .builder()

                    .totalLoans(
                            loanRepository.count()
                    )

                    .draftLoans(
                            loanRepository.countByLoanStatus(
                                    LoanStatus.DRAFT
                            )
                    )

                    .approvedLoans(
                            loanRepository.countByLoanStatus(
                                    LoanStatus.APPROVED
                            )
                    )

                    .activeLoans(
                            loanRepository.countByLoanStatus(
                                    LoanStatus.ACTIVE
                            )
                    )

                    .rejectedLoans(
                            loanRepository.countByLoanStatus(
                                    LoanStatus.REJECTED
                            )
                    )

                    .closedLoans(
                            loanRepository.countByLoanStatus(
                                    LoanStatus.CLOSED
                            )
                    )

                    .build();

        }

        EmployeeMarketAssignment assignment =
                assignmentRepository
                        .findFirstByEmployeeIdAndIsActiveTrue(
                                user.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No market assigned"
                                )
                        );

        UUID marketId =
                assignment.getMarket().getId();

        return LoanStatisticsResponse
                .builder()

                .totalLoans(
                        loanRepository.countByCustomerMarketId(
                                marketId
                        )
                )

                .draftLoans(
                        loanRepository.countByCustomerMarketIdAndLoanStatus(
                                marketId,
                                LoanStatus.DRAFT
                        )
                )

                .approvedLoans(
                        loanRepository.countByCustomerMarketIdAndLoanStatus(
                                marketId,
                                LoanStatus.APPROVED
                        )
                )

                .activeLoans(
                        loanRepository.countByCustomerMarketIdAndLoanStatus(
                                marketId,
                                LoanStatus.ACTIVE
                        )
                )

                .rejectedLoans(
                        loanRepository.countByCustomerMarketIdAndLoanStatus(
                                marketId,
                                LoanStatus.REJECTED
                        )
                )

                .closedLoans(
                        loanRepository.countByCustomerMarketIdAndLoanStatus(
                                marketId,
                                LoanStatus.CLOSED
                        )
                )

                .build();

    }
    
    @Override
    public Page<LoanResponse> getAllLoans(

            int page,

            int size,

            LoanStatus status

    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User user =
                userRepository
                        .findByMobileNumber(
                                mobileNumber
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        PageRequest pageable =
                PageRequest.of(page, size);

        // ==========================
        // ADMIN
        // ==========================

        if (user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            Page<Loan> loans;

            if (status == null) {

                loans =
                        loanRepository.findAll(
                                pageable
                        );

            } else {

                loans =
                        loanRepository.findByLoanStatus(
                                status,
                                pageable
                        );

            }

            return loans.map(
                    loanMapper::toResponse
            );
        }

        // ==========================
        // EMPLOYEE
        // ==========================

        EmployeeMarketAssignment assignment =
                assignmentRepository
                        .findFirstByEmployeeIdAndIsActiveTrue(
                                user.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No market assigned"
                                )
                        );

        UUID marketId =
                assignment.getMarket().getId();

        Page<Loan> loans;

        if (status == null) {

            loans =
                    loanRepository.findByCustomerMarketId(
                            marketId,
                            pageable
                    );

        } else {

            loans =
                    loanRepository
                            .findByCustomerMarketIdAndLoanStatus(

                                    marketId,

                                    status,

                                    pageable

                            );

        }

        return loans.map(
                loanMapper::toResponse
        );

    }
    @Override
    public Page<LoanResponse> searchLoans(

            String keyword,

            int page,

            int size

    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User user =
                userRepository
                        .findByMobileNumber(
                                mobileNumber
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        if (user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            return loanRepository

                    .findByCustomerFirstNameContainingIgnoreCaseOrCustomerLastNameContainingIgnoreCaseOrCustomerMobileNumberContaining(

                            keyword,

                            keyword,

                            keyword,

                            PageRequest.of(
                                    page,
                                    size
                            )

                    )

                    .map(
                            loanMapper::toResponse
                    );

        }

        EmployeeMarketAssignment assignment =
                assignmentRepository
                        .findFirstByEmployeeIdAndIsActiveTrue(
                                user.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No market assigned"
                                )
                        );

        return loanRepository

                .findByCustomerMarketIdAndCustomerFirstNameContainingIgnoreCaseOrCustomerMarketIdAndCustomerLastNameContainingIgnoreCaseOrCustomerMarketIdAndCustomerMobileNumberContaining(

                        assignment.getMarket().getId(),

                        keyword,

                        assignment.getMarket().getId(),

                        keyword,

                        assignment.getMarket().getId(),

                        keyword,

                        PageRequest.of(
                                page,
                                size
                        )

                )

                .map(
                        loanMapper::toResponse
                );

    }
    
    
    @Override
    public CalculateEmiResponse calculateEmi(
            CalculateEmiRequest request
    ) {

        BigDecimal emiAmount = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalPayable = BigDecimal.ZERO;

        // -------------------------
        // EMERGENCY LOAN
        // -------------------------
        if (request.getLoanType() == LoanType.EMERGENCY) {

            if (request.getTenure() == null || request.getTenure() <= 0) {
                throw new RuntimeException("Emergency loan days are required");
            }

            if (request.getLoanAmount() == null) {
                throw new RuntimeException("Loan amount required");
            }

            if (request.getInterestRate() == null) {
                throw new RuntimeException("Interest rate required");
            }

            totalInterest =
                    request.getLoanAmount()
                            .multiply(request.getInterestRate())
                            .multiply(BigDecimal.valueOf(request.getTenure()))
                            .divide(
                                    BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            totalPayable =
                    request.getLoanAmount()
                            .add(totalInterest);

            emiAmount = totalPayable;

            return CalculateEmiResponse.builder()
                    .emiAmount(emiAmount)
                    .totalInterest(totalInterest)
                    .totalPayable(totalPayable)
                    .build();
        }

        // -------------------------
        // REGULAR LOAN - FLAT DIRECT (% on Total Principal)
        // -------------------------
        else if (request.getInterestType() == InterestType.FLAT_DIRECT) {

            totalInterest =
                    request.getLoanAmount()
                            .multiply(request.getInterestRate())
                            .divide(
                                    BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            totalPayable =
                    request.getLoanAmount()
                            .add(totalInterest);

            emiAmount =
                    totalPayable.divide(
                            BigDecimal.valueOf(request.getTenure()),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        // -------------------------
        // REGULAR LOAN - FLAT PER MONTH (% Per Month)
        // -------------------------
        else if (request.getInterestType() == InterestType.FLAT_PER_MONTH) {

            BigDecimal monthlyRate =
                    request.getInterestRate()
                            .divide(
                                    BigDecimal.valueOf(100),
                                    10,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal tenureInMonths;

            switch (request.getRepaymentFrequency()) {

                case EMI:
                    tenureInMonths =
                            BigDecimal.valueOf(request.getTenure());
                    break;

                case EWI:
                    tenureInMonths =
                            BigDecimal.valueOf(request.getTenure())
                                    .divide(
                                            BigDecimal.valueOf(4.3333333333),
                                            10,
                                            RoundingMode.HALF_UP
                                    );
                    break;

                case EDI:
                    tenureInMonths =
                            BigDecimal.valueOf(request.getTenure())
                                    .divide(
                                            BigDecimal.valueOf(30),
                                            10,
                                            RoundingMode.HALF_UP
                                    );
                    break;

                default:
                    tenureInMonths = BigDecimal.ONE;
            }

            totalInterest =
                    request.getLoanAmount()
                            .multiply(monthlyRate)
                            .multiply(tenureInMonths)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            totalPayable =
                    request.getLoanAmount()
                            .add(totalInterest);

            emiAmount =
                    totalPayable.divide(
                            BigDecimal.valueOf(request.getTenure()),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        // -------------------------
        // REGULAR LOAN - FLAT
        // -------------------------
        else if (request.getInterestType() == InterestType.FLAT) {

            BigDecimal yearlyRate =
                    request.getInterestRate()
                            .divide(
                                    BigDecimal.valueOf(100),
                                    10,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal tenureInYears;

            switch (request.getRepaymentFrequency()) {

                case EMI:
                    tenureInYears =
                            BigDecimal.valueOf(request.getTenure())
                                    .divide(
                                            BigDecimal.valueOf(12),
                                            10,
                                            RoundingMode.HALF_UP
                                    );
                    break;

                case EWI:
                    tenureInYears =
                            BigDecimal.valueOf(request.getTenure())
                                    .divide(
                                            BigDecimal.valueOf(52),
                                            10,
                                            RoundingMode.HALF_UP
                                    );
                    break;

                case EDI:
                    tenureInYears =
                            BigDecimal.valueOf(request.getTenure())
                                    .divide(
                                            BigDecimal.valueOf(365),
                                            10,
                                            RoundingMode.HALF_UP
                                    );
                    break;

                default:
                    tenureInYears = BigDecimal.ONE;
            }

            totalInterest =
                    request.getLoanAmount()
                            .multiply(yearlyRate)
                            .multiply(tenureInYears)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            totalPayable =
                    request.getLoanAmount()
                            .add(totalInterest);

            emiAmount =
                    totalPayable.divide(
                            BigDecimal.valueOf(request.getTenure()),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        // -------------------------
        // REGULAR LOAN - REDUCING
        // -------------------------
        else {

            double principal =
                    request.getLoanAmount().doubleValue();

            double annualRate =
                    request.getInterestRate().doubleValue();

            double periodicRate;

            switch (request.getRepaymentFrequency()) {

                case EMI:
                    periodicRate = annualRate / 1200.0;
                    break;

                case EWI:
                    periodicRate = annualRate / 5200.0;
                    break;

                case EDI:
                    periodicRate = annualRate / 36500.0;
                    break;

                default:
                    periodicRate = annualRate / 1200.0;
            }

            int tenure = request.getTenure();

            double emi =
                    principal
                            * periodicRate
                            * Math.pow(1 + periodicRate, tenure)
                            /
                            (
                                    Math.pow(1 + periodicRate, tenure) - 1
                            );

            emiAmount =
                    BigDecimal.valueOf(emi)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            totalPayable =
                    emiAmount.multiply(
                            BigDecimal.valueOf(tenure)
                    );

            totalInterest =
                    totalPayable
                            .subtract(request.getLoanAmount())
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );
        }

        return CalculateEmiResponse
                .builder()
                .emiAmount(emiAmount)
                .totalInterest(totalInterest)
                .totalPayable(totalPayable)
                .build();
    }

    @Override
    public LoanResponse createLoan(
            CreateLoanRequest request
    ) {

        Customer customer =
                customerRepository.findById(
                        request.getCustomerId()
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Customer not found"
                        )
                );
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User currentUser =
                userRepository
                        .findByMobileNumber(mobileNumber)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Authenticated user not found"
                                )
                        );

        Loan loan =
                Loan.builder()
                        .customer(customer)
                        .loanType(request.getLoanType())
                        .loanAmount(request.getLoanAmount())
                        .interestRate(request.getInterestRate())
                        .interestType(request.getInterestType())
                        .tenure(request.getTenure())
                        .repaymentFrequency(
                                request.getRepaymentFrequency()
                        )
                        .loanStatus(
                                LoanStatus.DRAFT
                        )
                        .applicationDate(
                                LocalDateTime.now()
                        )
                        .createdBy(currentUser)
                        .build();

        Loan savedLoan =
                loanRepository.save(loan);
        createCharge(
                savedLoan,
                ChargeType.PROCESSING_FEE,
                request.getProcessingCharge(),
                true
        );

        createCharge(
                savedLoan,
                ChargeType.FILE_CHARGE,
                request.getFileCharge(),
                true
        );

        createCharge(
                savedLoan,
                ChargeType.MISC_CHARGE,
                request.getMiscellaneousCharge(),
                false
        );
        
        String creatorLog = (currentUser != null && currentUser.getFullName() != null)
                ? currentUser.getFullName() + " (" + (currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "Employee") + ")"
                : "System/User";
        String custNameStr = (savedLoan.getCustomer() != null)
                ? (savedLoan.getCustomer().getFirstName() != null ? savedLoan.getCustomer().getFirstName() : "") + (savedLoan.getCustomer().getLastName() != null ? " " + savedLoan.getCustomer().getLastName() : "")
                : "Customer";
        auditLogService.log(
                creatorLog,
                "Submitted loan application ₹" + (savedLoan.getLoanAmount() != null ? savedLoan.getLoanAmount().toBigInteger().toString() : "0") + " for " + custNameStr.trim(),
                "LOAN",
                savedLoan.getId().toString()
        );

        return loanMapper.toResponse(
                savedLoan
        );
    }

    @Override
    public LoanResponse updateLoan(
            UUID loanId,
            UpdateLoanRequest request
    ) {

        //----------------------------------------------------------
        // AUTHENTICATED USER
        //----------------------------------------------------------

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User currentUser =
                userRepository
                        .findByMobileNumber(
                                mobileNumber
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Authenticated user not found"
                                )
                        );


        //----------------------------------------------------------
        // ADMIN ONLY
        //----------------------------------------------------------

        if (!currentUser
                .getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            throw new RuntimeException(
                    "Only admin can edit a submitted loan"
            );
        }


        //----------------------------------------------------------
        // ACCESS CONTROL
        //----------------------------------------------------------

        accessControlService
                .validateLoanAccess(
                        loanId
                );


        //----------------------------------------------------------
        // FIND LOAN
        //----------------------------------------------------------

        Loan loan =
                loanRepository
                        .findById(loanId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Loan not found"
                                )
                        );


        //----------------------------------------------------------
        // STATUS PROTECTION
        //----------------------------------------------------------

        if (loan.getLoanStatus()
                != LoanStatus.PENDING_APPROVAL) {

            throw new RuntimeException(
                    "Only pending approval loans can be edited"
            );
        }


        //----------------------------------------------------------
        // VALIDATE REQUEST
        //----------------------------------------------------------

        if (request.getLoanType() == null) {

            throw new RuntimeException(
                    "Loan type is required"
            );
        }

        if (request.getLoanAmount() == null ||
                request.getLoanAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Loan amount must be greater than zero"
            );
        }

        if (request.getInterestRate() == null ||
                request.getInterestRate()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new RuntimeException(
                    "Interest rate cannot be negative"
            );
        }

        if (request.getInterestType() == null) {

            throw new RuntimeException(
                    "Interest type is required"
            );
        }

        if (request.getTenure() == null ||
                request.getTenure() <= 0) {

            throw new RuntimeException(
                    "Tenure must be greater than zero"
            );
        }

        if (request.getRepaymentFrequency() == null) {

            throw new RuntimeException(
                    "Repayment frequency is required"
            );
        }


        //----------------------------------------------------------
        // UPDATE LOAN
        //----------------------------------------------------------

        loan.setLoanType(
                request.getLoanType()
        );

        loan.setLoanAmount(
                request.getLoanAmount()
        );

        loan.setInterestRate(
                request.getInterestRate()
        );

        loan.setInterestType(
                request.getInterestType()
        );

        loan.setTenure(
                request.getTenure()
        );

        loan.setRepaymentFrequency(
                request.getRepaymentFrequency()
        );


        //----------------------------------------------------------
        // SAVE
        //----------------------------------------------------------

        Loan updatedLoan =
                loanRepository.save(loan);


        //----------------------------------------------------------
        // AUDIT LOG
        //----------------------------------------------------------

        auditLogService.log(
                currentUser.getId().toString(),
                "UPDATE_LOAN",
                "LOAN",
                updatedLoan.getId().toString()
        );


        //----------------------------------------------------------
        // RESPONSE
        //----------------------------------------------------------

        return loanMapper.toResponse(
                updatedLoan
        );
    }

    
    @Override
    public LoanResponse getLoanById(
            UUID loanId
    ) {
    	accessControlService
        .validateLoanAccess(
                loanId
        );

        Loan loan =
                loanRepository.findById(loanId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Loan not found"
                                )
                        );

        return loanMapper.toResponse(
                loan
        );
    }

    @Override
    public List<LoanResponse> getCustomerLoans(
            UUID customerId
    ) {
    	accessControlService
        .validateCustomerAccess(
                customerId
        );

        return loanRepository
                .findByCustomerId(customerId)
                .stream()
                .map(loanMapper::toResponse)
                .toList();
    }

    @Override
    public void deleteLoan(
            UUID loanId
    ) {
    	accessControlService
        .validateLoanAccess(
                loanId
        );

        Loan loan =
                loanRepository.findById(loanId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Loan not found"
                                )
                        );

        loanRepository.delete(loan);
    }
    @Override
    public LoanSummaryResponse getLoanSummary(
            UUID loanId
    ) {
    	
    	accessControlService
        .validateLoanAccess(
                loanId
        );

        Loan loan =
                loanRepository.findById(
                        loanId
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Loan not found"
                        )
                );

        BigDecimal totalCollected =
                collectionRepository
                        .findByLoanId(
                                loanId
                        )
                        .stream()
                        .map(
                                LoanCollection
                                        ::getCollectedAmount
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal outstandingAmount =
                repaymentScheduleRepository
                        .findByLoanIdOrderByInstallmentNumberAsc(
                                loanId
                        )
                        .stream()
                        .map(
                                LoanRepaymentSchedule
                                        ::getOutstandingAmount
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return LoanSummaryResponse
                .builder()
                .loanId(
                        loan.getId()
                )
                .approvedAmount(
                        loan.getApprovedAmount()
                )
                .disbursedAmount(
                        loan.getDisbursedAmount()
                )
                .totalCollected(
                        totalCollected
                )
                .outstandingAmount(
                        outstandingAmount
                )
                .loanStatus(
                        loan.getLoanStatus()
                                .name()
                )
                .build();
    }
}