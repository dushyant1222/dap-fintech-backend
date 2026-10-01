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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;

import com.dapfintech.loan.dto.request.LoanFilterRequest;
import com.dapfintech.loan.specification.LoanSpecification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.dapfintech.loan.enums.RepaymentStatus;
import com.dapfintech.loan.service.LoanRepaymentScheduleService;
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
    private final LoanRepaymentScheduleService repaymentScheduleService;
    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final LoanChargeRepository loanChargeRepository;
    private final EmployeeMarketAssignmentRepository assignmentRepository;
    private final com.dapfintech.notification.service.NotificationService notificationService;
    
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

        if (user.isAdmin()) {

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

        if (user.isAdmin()) {

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

        if (user.isAdmin()) {

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

        PageRequest pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "applicationDate"
                        )
                );

        if (user.isAdmin()) {
            LoanFilterRequest filter = LoanFilterRequest.builder()
                    .keyword(keyword)
                    .build();

            return loanRepository
                    .findAll(
                            LoanSpecification.withFilters(filter),
                            pageable
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

        LoanFilterRequest filter = LoanFilterRequest.builder()
                .keyword(keyword)
                .marketId(assignment.getMarket().getId())
                .build();

        return loanRepository
                .findAll(
                        LoanSpecification.withFilters(filter),
                        pageable
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
            BigDecimal principal = request.getLoanAmount() != null ? request.getLoanAmount() : BigDecimal.ZERO;
            BigDecimal dailyInterest = request.getInterestRate() != null ? request.getInterestRate() : BigDecimal.ZERO;
            totalInterest = dailyInterest;
            totalPayable = principal.add(dailyInterest);
            emiAmount = dailyInterest;

            return CalculateEmiResponse.builder()
                    .emiAmount(emiAmount)
                    .totalInterest(totalInterest)
                    .totalPayable(totalPayable)
                    .build();
        }

        // -------------------------
        // REGULAR LOAN - FLAT DIRECT (Flat Amount)
        // -------------------------
        else if (request.getInterestType() == InterestType.FLAT_DIRECT) {

            totalInterest = request.getInterestRate();

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
        
        if (loanRepository.existsByCustomerIdAndLoanStatus(request.getCustomerId(), LoanStatus.ACTIVE)) {
            throw new RuntimeException("Cannot issue a new loan. Customer already has an ACTIVE loan.");
        }

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

        String typePrefix = request.getLoanType() == com.dapfintech.loan.enums.LoanType.REGULAR ? "RLN" : "ELN";
        String custPrefix = "NA";
        if (customer.getFirstName() != null && !customer.getFirstName().trim().isEmpty()) {
            String cName = customer.getFirstName().trim().toUpperCase();
            custPrefix = cName.length() >= 2 ? cName.substring(0, 2) : cName;
        }
        String marketPrefix = "NA";
        if (customer.getMarket() != null && customer.getMarket().getMarketName() != null && !customer.getMarket().getMarketName().trim().isEmpty()) {
            String mName = customer.getMarket().getMarketName().trim().toUpperCase();
            marketPrefix = mName.length() >= 2 ? mName.substring(0, 2) : mName;
        }

        String baseCode = String.format("%s-%s-%s", typePrefix, custPrefix, marketPrefix);
        int num = 1;
        String loanCodeStr = baseCode + "-" + num;
        while (loanRepository.existsByLoanCode(loanCodeStr)) {
            num++;
            loanCodeStr = baseCode + "-" + num;
        }

        Loan loan =
                Loan.builder()
                        .customer(customer)
                        .loanCode(loanCodeStr)
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

        if (savedLoan != null && savedLoan.getCreatedBy() != null) {
            notificationService.createNotificationForUser(
                "Loan Request Created",
                "Your loan request for ₹" + savedLoan.getLoanAmount() + " has been created.",
                savedLoan.getCreatedBy()
            );
        }

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
    @Transactional
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

        if (!currentUser.isAdmin()) {

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

        boolean isMasterAdmin = (currentUser.getEmail() != null && "singh.amitjadoun@gmail.com".equalsIgnoreCase(currentUser.getEmail()))
                || (currentUser.getRole() != null && currentUser.getRole().getRoleName() != null && currentUser.getRole().getRoleName().toUpperCase().contains("MASTER"))
                || currentUser.isAdmin();

        boolean isActive = loan.getLoanStatus() == LoanStatus.ACTIVE;

        if (loan.getLoanStatus() != LoanStatus.PENDING_APPROVAL) {
            if (isActive && isMasterAdmin) {
                // Master admin allowed to edit ACTIVE loans
            } else {
                throw new RuntimeException(
                        "Only pending approval loans can be edited (Master Admin can edit active loans)"
                );
            }
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

        if (request.getLoanType() == LoanType.EMERGENCY) {
            if (request.getTenure() == null || request.getTenure() < 0) {
                request.setTenure(0);
            }
            if (request.getRepaymentFrequency() == null) {
                request.setRepaymentFrequency(com.dapfintech.loan.enums.RepaymentFrequency.EDI);
            }
        } else {
            if (request.getTenure() == null || request.getTenure() <= 0) {
                throw new RuntimeException(
                        "Tenure must be greater than zero"
                );
            }
            if (request.getRepaymentFrequency() == null) {
                request.setRepaymentFrequency(com.dapfintech.loan.enums.RepaymentFrequency.EDI);
            }
        }


        //----------------------------------------------------------
        // UPDATE LOAN
        //----------------------------------------------------------

        LoanType newType = request.getLoanType();
        loan.setLoanType(newType);
        loan.setLoanAmount(request.getLoanAmount());
        if (isActive) {
            loan.setApprovedAmount(request.getLoanAmount());
        }
        loan.setInterestRate(request.getInterestRate());
        loan.setInterestType(request.getInterestType());
        loan.setTenure(request.getTenure());
        loan.setRepaymentFrequency(request.getRepaymentFrequency());

        // Update Loan Code prefix if loan type changed
        if (loan.getLoanCode() != null) {
            if (newType == LoanType.EMERGENCY && loan.getLoanCode().startsWith("RLN")) {
                loan.setLoanCode("ELN" + loan.getLoanCode().substring(3));
            } else if (newType == LoanType.REGULAR && loan.getLoanCode().startsWith("ELN")) {
                loan.setLoanCode("RLN" + loan.getLoanCode().substring(3));
            }
        }

        Loan updatedLoan = loanRepository.save(loan);

        //----------------------------------------------------------
        // RECALCULATE ACTIVE LOAN SCHEDULES & RELINK COLLECTIONS
        //----------------------------------------------------------
        if (isActive) {
            // A. Detach all collections
            List<LoanCollection> collections = collectionRepository.findByLoanIdOrderByCollectionDateAsc(updatedLoan.getId());
            for (LoanCollection col : collections) {
                col.setRepaymentSchedule(null);
            }
            collectionRepository.saveAll(collections);
            collectionRepository.flush();

            // B. Delete existing schedules
            repaymentScheduleRepository.deleteByLoanId(updatedLoan.getId());
            repaymentScheduleRepository.flush();

            // C. Generate new schedules
            if (updatedLoan.getLoanType() == LoanType.EMERGENCY) {
                BigDecimal principal = updatedLoan.getApprovedAmount() != null ? updatedLoan.getApprovedAmount() : updatedLoan.getLoanAmount();
                BigDecimal dailyInterest = updatedLoan.getInterestRate() != null ? updatedLoan.getInterestRate() : BigDecimal.ZERO;
                LocalDate disDate = updatedLoan.getDisbursementDate() != null ? updatedLoan.getDisbursementDate().toLocalDate() : LocalDate.now();
                LocalDate today = LocalDate.now();
                List<LoanRepaymentSchedule> newSchedules = new ArrayList<>();

                if (dailyInterest.compareTo(BigDecimal.ZERO) > 0) {
                    LocalDate curr = disDate;
                    int instNum = 1;
                    while (!curr.isAfter(today)) {
                        newSchedules.add(LoanRepaymentSchedule.builder()
                                .loan(updatedLoan)
                                .installmentNumber(instNum++)
                                .dueDate(curr)
                                .principalAmount(BigDecimal.ZERO)
                                .interestAmount(dailyInterest)
                                .installmentAmount(dailyInterest)
                                .dueAmount(dailyInterest)
                                .paidAmount(BigDecimal.ZERO)
                                .outstandingAmount(dailyInterest)
                                .repaymentStatus(RepaymentStatus.PENDING)
                                .build());
                        curr = curr.plusDays(1);
                    }
                    if (newSchedules.isEmpty()) {
                        newSchedules.add(LoanRepaymentSchedule.builder()
                                .loan(updatedLoan)
                                .installmentNumber(1)
                                .dueDate(disDate)
                                .principalAmount(BigDecimal.ZERO)
                                .interestAmount(dailyInterest)
                                .installmentAmount(dailyInterest)
                                .dueAmount(dailyInterest)
                                .paidAmount(BigDecimal.ZERO)
                                .outstandingAmount(dailyInterest)
                                .repaymentStatus(RepaymentStatus.PENDING)
                                .build());
                    }
                }
                repaymentScheduleRepository.saveAll(newSchedules);
                repaymentScheduleRepository.flush();
            } else {
                repaymentScheduleService.generateSchedule(updatedLoan.getId());
            }

            // D. Reapply collected amounts to new schedules
            BigDecimal totalCollected = collections.stream()
                    .map(LoanCollection::getCollectedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalCollected.compareTo(BigDecimal.ZERO) > 0) {
                List<LoanRepaymentSchedule> newScheds = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(updatedLoan.getId());
                BigDecimal rem = totalCollected;
                int colIdx = 0;

                for (LoanRepaymentSchedule s : newScheds) {
                    if (rem.compareTo(BigDecimal.ZERO) <= 0) {
                        s.setPaidAmount(BigDecimal.ZERO);
                        s.setOutstandingAmount(s.getInstallmentAmount());
                        s.setRepaymentStatus(RepaymentStatus.PENDING);
                        continue;
                    }

                    BigDecimal due = s.getInstallmentAmount() != null ? s.getInstallmentAmount() : BigDecimal.ZERO;
                    if (rem.compareTo(due) >= 0) {
                        s.setPaidAmount(due);
                        s.setOutstandingAmount(BigDecimal.ZERO);
                        s.setRepaymentStatus(RepaymentStatus.PAID);
                        rem = rem.subtract(due);

                        if (colIdx < collections.size()) {
                            collections.get(colIdx).setRepaymentSchedule(s);
                            colIdx++;
                        }
                    } else {
                        s.setPaidAmount(rem);
                        s.setOutstandingAmount(due.subtract(rem));
                        s.setRepaymentStatus(RepaymentStatus.PENDING);
                        if (colIdx < collections.size()) {
                            collections.get(colIdx).setRepaymentSchedule(s);
                        }
                        rem = BigDecimal.ZERO;
                    }
                }
                repaymentScheduleRepository.saveAll(newScheds);
                collectionRepository.saveAll(collections);
            }
        }

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
    @Transactional
    public void deleteLoan(
            UUID loanId
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

        if (!user.isAdmin()) {
            throw new RuntimeException("Only Admin can delete loans");
        }

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

        // Delete all child entities in foreign-key order
        loanRepository.deleteCollectionsByLoanId(loanId);
        loanRepository.deleteSchedulesByLoanId(loanId);
        loanRepository.deleteChargesByLoanId(loanId);
        loanRepository.deleteDocumentsByLoanId(loanId);
        loanRepository.deleteApprovalsByLoanId(loanId);
        loanRepository.deleteDisbursementsByLoanId(loanId);
        loanRepository.deleteClosuresByLoanId(loanId);

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

        BigDecimal totalCollected = collectionRepository.getSumCollectedByLoan(loanId);
        if (totalCollected == null) totalCollected = BigDecimal.ZERO;

        if (loan.getLoanType() == LoanType.EMERGENCY) {
            BigDecimal principal = loan.getDisbursedAmount() != null ? loan.getDisbursedAmount()
                    : (loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getLoanAmount());
            if (principal == null) principal = BigDecimal.ZERO;

            LocalDate today = LocalDate.now();
            List<LoanRepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
            BigDecimal unpaidDailyFeesTillToday = schedules.stream()
                    .filter(s -> s.getDueDate() == null || !s.getDueDate().isAfter(today))
                    .map(s -> s.getOutstandingAmount() != null ? s.getOutstandingAmount() : (s.getDueAmount() != null ? s.getDueAmount().subtract(s.getPaidAmount() != null ? s.getPaidAmount() : BigDecimal.ZERO) : BigDecimal.ZERO))
                    .filter(amt -> amt.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal emergencyOutstanding = principal.add(unpaidDailyFeesTillToday);

            return LoanSummaryResponse.builder()
                    .loanId(loan.getId())
                    .approvedAmount(loan.getApprovedAmount())
                    .disbursedAmount(loan.getDisbursedAmount())
                    .totalLoanAmount(principal)
                    .totalCollected(totalCollected)
                    .outstandingAmount(emergencyOutstanding)
                    .loanStatus(loan.getLoanStatus().name())
                    .build();
        }

        BigDecimal outstandingAmount = repaymentScheduleRepository.getSumOutstandingByLoan(loanId);
        if (outstandingAmount == null) outstandingAmount = BigDecimal.ZERO;

        BigDecimal totalLoanAmount = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)
                .stream()
                .map(s -> s.getInstallmentAmount() != null ? s.getInstallmentAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalLoanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal principal = loan.getApprovedAmount() != null ? loan.getApprovedAmount()
                    : (loan.getDisbursedAmount() != null ? loan.getDisbursedAmount() : loan.getLoanAmount());
            if (principal == null) principal = BigDecimal.ZERO;
            BigDecimal interest = (principal.compareTo(BigDecimal.ZERO) > 0 && loan.getInterestRate() != null)
                    ? principal.multiply(loan.getInterestRate()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            totalLoanAmount = principal.add(interest);
        }

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
                .totalLoanAmount(
                        totalLoanAmount
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

    @Override
    public Page<com.dapfintech.loan.dto.response.LoanBureauResponse> getLoanBureau(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        Page<Loan> loans = loanRepository.findByLoanStatus(LoanStatus.ACTIVE, pageable);
        
        return loans.map(loan -> {
            BigDecimal installmentAmount = repaymentScheduleRepository.getFirstInstallmentAmountByLoan(loan.getId());
            if (installmentAmount == null) installmentAmount = BigDecimal.ZERO;
            
            BigDecimal totalCollected = collectionRepository.getSumCollectedByLoan(loan.getId());
            if (totalCollected == null) totalCollected = BigDecimal.ZERO;
            
            BigDecimal totalAmount = installmentAmount.multiply(BigDecimal.valueOf(loan.getTenure()));
            BigDecimal totalBalance = totalAmount.subtract(totalCollected);
            
            java.time.LocalDateTime startDate = loan.getDisbursementDate();
            if (startDate == null) {
                startDate = loan.getApprovalDate();
            }
            if (startDate == null) {
                startDate = java.time.LocalDateTime.now();
            }

            long daysTillToday = java.time.temporal.ChronoUnit.DAYS.between(startDate.toLocalDate(), java.time.LocalDate.now()) + 1;
            
            BigDecimal requiredTillDate = installmentAmount.multiply(BigDecimal.valueOf(daysTillToday));
            BigDecimal pendingBalance = requiredTillDate.subtract(totalCollected);
            
            return com.dapfintech.loan.dto.response.LoanBureauResponse.builder()
                    .loanId(loan.getId())
                    .loanCode(loan.getLoanCode())
                    .customerName(loan.getCustomer().getFirstName() + " " + loan.getCustomer().getLastName())
                    .repaymentFrequency(loan.getRepaymentFrequency().name())
                    .tenure(loan.getTenure())
                    .installmentAmount(installmentAmount)
                    .totalAmount(totalAmount)
                    .receivedTillDate(totalCollected)
                    .totalBalance(totalBalance)
                    .pendingBalance(pendingBalance)
                    .build();
        });
    }
}