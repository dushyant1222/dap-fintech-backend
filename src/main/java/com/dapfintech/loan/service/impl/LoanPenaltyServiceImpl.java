package com.dapfintech.loan.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.loan.dto.request.CloseSpecialLoanRequest;
import com.dapfintech.loan.dto.request.UpdatePenaltySettingsRequest;
import com.dapfintech.loan.dto.response.LoanClosureResponse;
import com.dapfintech.loan.dto.response.LoanPenaltySummaryResponse;
import com.dapfintech.loan.dto.response.OverdueInstallmentPenaltyResponse;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanClosure;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.RepaymentStatus;
import com.dapfintech.loan.repository.LoanClosureRepository;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.loan.repository.LoanRepository;

import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.enums.CollectionStatus;
import com.dapfintech.loan.enums.CollectionMode;
import com.dapfintech.employee.repository.DayBookRepository;
import com.dapfintech.employee.service.DayBookService;


import com.dapfintech.loan.service.LoanPenaltyService;
import com.dapfintech.security.service.AccessControlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanPenaltyServiceImpl implements LoanPenaltyService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final LoanClosureRepository loanClosureRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;

    private final LoanCollectionRepository loanCollectionRepository;
    private final DayBookRepository dayBookRepository;
    private final DayBookService dayBookService;


    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public LoanPenaltySummaryResponse calculatePenalty(UUID loanId) {
        accessControlService.validateLoanAccess(loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        BigDecimal penaltyRate = loan.getPenaltyRate() != null ? loan.getPenaltyRate() : BigDecimal.valueOf(0.50);
        if (penaltyRate.compareTo(BigDecimal.valueOf(0.10)) < 0) {
            penaltyRate = BigDecimal.valueOf(0.10);
        }

        BigDecimal waivedPercent = loan.getPenaltyWaivedPercent() != null ? loan.getPenaltyWaivedPercent() : BigDecimal.ZERO;

        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        LocalDate today = LocalDate.now();

        List<OverdueInstallmentPenaltyResponse> overdueList = new ArrayList<>();
        BigDecimal totalOutstandingPrincipal = BigDecimal.ZERO;
        BigDecimal grossPenalty = BigDecimal.ZERO;

        for (LoanRepaymentSchedule sch : schedules) {
            BigDecimal outstanding = sch.getOutstandingAmount() != null ? sch.getOutstandingAmount() : BigDecimal.ZERO;
            if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                totalOutstandingPrincipal = totalOutstandingPrincipal.add(outstanding);
            }

            if (sch.getDueDate() != null && sch.getDueDate().isBefore(today) && outstanding.compareTo(BigDecimal.ZERO) > 0) {
                long daysOverdue = ChronoUnit.DAYS.between(sch.getDueDate(), today);
                boolean withinGrace = daysOverdue <= 2;
                BigDecimal compoundPenalty = BigDecimal.ZERO;

                if (!withinGrace && daysOverdue >= 3) {
                    long effectiveDays = daysOverdue - 2;
                    double rateDecimal = penaltyRate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP).doubleValue();
                    double multiplier = Math.pow(1.0 + rateDecimal, effectiveDays) - 1.0;
                    if (multiplier < 0) multiplier = 0;
                    compoundPenalty = outstanding.multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP);
                    grossPenalty = grossPenalty.add(compoundPenalty);
                }

                overdueList.add(OverdueInstallmentPenaltyResponse.builder()
                        .installmentNumber(sch.getInstallmentNumber())
                        .dueDate(sch.getDueDate())
                        .daysOverdue(daysOverdue)
                        .installmentAmount(sch.getInstallmentAmount())
                        .paidAmount(sch.getPaidAmount())
                        .shortfallAmount(outstanding)
                        .withinGracePeriod(withinGrace)
                        .compoundPenalty(compoundPenalty)
                        .build());
            }
        }

        BigDecimal waivedPenaltyAmount = grossPenalty.multiply(waivedPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal netPayablePenalty = grossPenalty.subtract(waivedPenaltyAmount).max(BigDecimal.ZERO);
        BigDecimal totalPayable = totalOutstandingPrincipal.add(netPayablePenalty);

        return LoanPenaltySummaryResponse.builder()
                .loanId(loan.getId())
                .loanAmount(loan.getLoanAmount())
                .totalOutstandingPrincipal(totalOutstandingPrincipal)
                .penaltyRate(penaltyRate)
                .penaltyWaivedPercent(waivedPercent)
                .gracePeriodDays(2)
                .totalOverdueInstallments(overdueList.size())
                .overdueInstallments(overdueList)
                .grossCompoundPenalty(grossPenalty)
                .waivedPenaltyAmount(waivedPenaltyAmount)
                .netPayablePenalty(netPayablePenalty)
                .totalPayableWithPenalty(totalPayable)
                .build();
    }

    @Override
    public LoanPenaltySummaryResponse updatePenaltySettings(UUID loanId, UpdatePenaltySettingsRequest request) {
        verifyAdminAccess();
        accessControlService.validateLoanAccess(loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (request.getPenaltyRate() != null) {
            if (request.getPenaltyRate().compareTo(BigDecimal.valueOf(0.10)) < 0) {
                throw new RuntimeException("Minimum penalty rate must be at least 0.1%");
            }
            loan.setPenaltyRate(request.getPenaltyRate());
        }

        if (request.getPenaltyWaivedPercent() != null) {
            if (request.getPenaltyWaivedPercent().compareTo(BigDecimal.ZERO) < 0 || request.getPenaltyWaivedPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("Waived off percentage must be between 0% and 100%");
            }
            loan.setPenaltyWaivedPercent(request.getPenaltyWaivedPercent());
        }

        loanRepository.save(loan);

        return calculatePenalty(loanId);
    }

    @Override
    public LoanClosureResponse closeOnSpecialCondition(UUID loanId, CloseSpecialLoanRequest request) {
        User currentUser = verifyAdminAccess();
        accessControlService.validateLoanAccess(loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getLoanStatus() == LoanStatus.CLOSED) {
            throw new RuntimeException("This loan is already closed.");
        }

        if (request.getSpecialRemarks() == null || request.getSpecialRemarks().trim().isEmpty()) {
            throw new RuntimeException("Special remarks explaining genuine problem / compromise condition are required.");
        }

        if (loanClosureRepository.existsByLoanId(loanId)) {
            throw new RuntimeException("This loan already has a closure record.");
        }

        if (request.getWaivedPenaltyPercent() != null) {
            loan.setPenaltyWaivedPercent(request.getWaivedPenaltyPercent());
        } else {
            loan.setPenaltyWaivedPercent(BigDecimal.valueOf(100));
        }

        
        // CREATE LOAN COLLECTION FOR THE SETTLEMENT AMOUNT
        if (request.getSettlementAmountPaid() != null && request.getSettlementAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            LoanCollection collection = LoanCollection.builder()
                    .loan(loan)
                    .collectedBy(loan.getCreatedBy())
                    .collectedAmount(request.getSettlementAmountPaid())
                    .collectionDate(LocalDateTime.now())
                    .collectionMode(CollectionMode.CASH)
                    .collectionStatus(CollectionStatus.SUCCESS)
                    .receiptNumber("SPL-" + System.currentTimeMillis())
                    .remarks("SPECIAL CLOSURE SETTLEMENT")
                    .build();
            loanCollectionRepository.save(collection);

            // UPDATE DAYBOOK OF THE EMPLOYEE WHO CREATED THE LOAN
            if (loan.getCreatedBy() != null && loan.getCreatedBy().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
                LocalDate today = LocalDate.now();
                dayBookService.getOrCreateTodayDayBook(loan.getCreatedBy().getId());
                dayBookRepository.findByEmployeeIdAndDate(loan.getCreatedBy().getId(), today).ifPresent(dayBook -> {
                    if (dayBook.getCollections() == null) dayBook.setCollections(BigDecimal.ZERO);
                    dayBook.setCollections(dayBook.getCollections().add(request.getSettlementAmountPaid()));
                    
                    if (dayBook.getOpeningBalance() == null) dayBook.setOpeningBalance(BigDecimal.ZERO);
                    if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(BigDecimal.ZERO);
                    if (dayBook.getSpends() == null) dayBook.setSpends(BigDecimal.ZERO);
                    if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(BigDecimal.ZERO);
                    if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(BigDecimal.ZERO);
                    if (dayBook.getOfficeRemittance() == null) dayBook.setOfficeRemittance(BigDecimal.ZERO);
                    
                    if (dayBook.getCashIncomingTransfers() == null) dayBook.setCashIncomingTransfers(BigDecimal.ZERO);
                    if (dayBook.getCashOutgoingTransfers() == null) dayBook.setCashOutgoingTransfers(BigDecimal.ZERO);
                    
                    BigDecimal newClosing = dayBook.getOpeningBalance()
                            .add(dayBook.getCollections())
                            .add(dayBook.getIncomingTransfers())
                            .add(dayBook.getCashIncomingTransfers())
                            .subtract(dayBook.getSpends())
                            .subtract(dayBook.getLoansDisbursed())
                            .subtract(dayBook.getOutgoingTransfers())
                            .subtract(dayBook.getCashOutgoingTransfers())
                            .subtract(dayBook.getOfficeRemittance());
                    dayBook.setClosingBalance(newClosing);
                    dayBookRepository.save(dayBook);
                });
            }
        }

        loan.setClosedSpecialCondition(true);
        loan.setSpecialClosureRemarks(request.getSpecialRemarks());

        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        for (LoanRepaymentSchedule sch : schedules) {
            if (sch.getOutstandingAmount() != null && sch.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
                sch.setPaidAmount(sch.getInstallmentAmount());
                sch.setDueAmount(BigDecimal.ZERO);
                sch.setOutstandingAmount(BigDecimal.ZERO);
                sch.setRepaymentStatus(RepaymentStatus.PAID);
                scheduleRepository.save(sch);
            }
        }

        LoanClosure closure = LoanClosure.builder()
                .loan(loan)
                .closureDate(LocalDateTime.now())
                .remarks("SPECIAL CONDITION CLOSURE: " + request.getSpecialRemarks() + " [Penalty Waived: " + loan.getPenaltyWaivedPercent() + "%]")
                .build();

        LoanClosure savedClosure = loanClosureRepository.save(closure);

        loan.setLoanStatus(LoanStatus.CLOSED);
        loanRepository.save(loan);

        auditLogService.log(currentUser.getId().toString(), "CLOSE_LOAN_SPECIAL", "LOAN", loan.getId().toString());

        return LoanClosureResponse.builder()
                .id(savedClosure.getId())
                .loanId(loan.getId())
                .closureDate(savedClosure.getClosureDate())
                .remarks(savedClosure.getRemarks())
                .build();
    }

    private User verifyAdminAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByMobileNumber(auth.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        if (!user.getRole().getRoleName().equalsIgnoreCase("ADMIN")) {
            throw new RuntimeException("Only ADMIN can perform this action");
        }
        return user;
    }
}
