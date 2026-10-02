package com.dapfintech.loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.loan.dto.request.CreateDisbursementRequest;
import com.dapfintech.loan.dto.response.DisbursementResponse;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanCharge;
import com.dapfintech.loan.entity.LoanDisbursement;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.mapper.LoanDisbursementMapper;
import com.dapfintech.loan.repository.LoanChargeRepository;
import com.dapfintech.loan.repository.LoanDisbursementRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.service.LoanDisbursementService;
import com.dapfintech.loan.service.LoanRepaymentScheduleService;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.employee.entity.DayBook;
import com.dapfintech.employee.repository.DayBookRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanDisbursementServiceImpl
        implements LoanDisbursementService {

    private final LoanRepository loanRepository;
    private final LoanChargeRepository loanChargeRepository;
    private final LoanDisbursementRepository loanDisbursementRepository;
    private final LoanDisbursementMapper mapper;
    private final LoanRepaymentScheduleService repaymentScheduleService;
    private final UserRepository userRepository;
    private final DayBookRepository dayBookRepository;
    private final com.dapfintech.employee.service.DayBookService dayBookService;
    private final com.dapfintech.market.repository.EmployeeMarketAssignmentRepository assignmentRepository;

    @Override
    public DisbursementResponse disburseLoan(
            UUID loanId,
            CreateDisbursementRequest request
    ) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new RuntimeException("Loan not found"));

        if (loan.getLoanStatus() != LoanStatus.APPROVED) {

            throw new RuntimeException(
                    "Only approved loans can be disbursed"
            );
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User loggedInUser = userRepository.findByMobileNumber(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (loanDisbursementRepository
                .findByLoanId(loanId)
                .isPresent()) {

            throw new RuntimeException(
                    "Loan already disbursed"
            );
        }

        BigDecimal approvedAmount =
                loan.getApprovedAmount();

        BigDecimal totalCharges =
                BigDecimal.ZERO;

        if (loan.getLoanType() != LoanType.EMERGENCY) {

            totalCharges =
                    loanChargeRepository
                            .findByLoanId(loanId)
                            .stream()
                            .map(LoanCharge::getChargeAmount)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );
        }

        BigDecimal netDisbursedAmount =
                approvedAmount.subtract(
                        totalCharges
                );

        LoanDisbursement disbursement =
                LoanDisbursement.builder()
                        .loan(loan)
                        .approvedAmount(
                                approvedAmount
                        )
                        .totalCharges(
                                totalCharges
                        )
                        .netDisbursedAmount(
                                netDisbursedAmount
                        )
                        .disbursementMode(
                                request.getDisbursementMode()
                        )
                        .transactionReference(
                                request.getTransactionReference()
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .disbursementDate(
                                LocalDateTime.now()
                        )
                        .build();

        loanDisbursementRepository
                .save(disbursement);

        User responsibleUser = !loggedInUser.isAdmin() ? loggedInUser : (loan.getCreatedBy() != null ? loan.getCreatedBy() : null);
        if (responsibleUser == null && loan.getCustomer() != null && loan.getCustomer().getMarket() != null) {
            List<com.dapfintech.market.entity.EmployeeMarketAssignment> assigns = assignmentRepository.findActiveOrNullByMarketId(loan.getCustomer().getMarket().getId());
            if (assigns == null || assigns.isEmpty()) {
                assigns = assignmentRepository.findByMarketId(loan.getCustomer().getMarket().getId());
            }
            if (assigns != null && !assigns.isEmpty()) {
                responsibleUser = assigns.get(0).getEmployee();
            }
        }

        java.time.LocalDate activeDate = responsibleUser != null
                ? dayBookService.getActiveDayBookDate(responsibleUser.getId())
                : (loan.getCustomer() != null && loan.getCustomer().getMarket() != null
                        ? dayBookService.getActiveMarketDayBookDate(loan.getCustomer().getMarket().getId())
                        : dayBookService.getActiveDayBookDate(loggedInUser.getId()));
        LocalDateTime disDate = activeDate.atTime(java.time.LocalTime.now());

        loan.setDisbursedAmount(
                netDisbursedAmount
        );

        loan.setDisbursementDate(disDate);

        loan.setLoanStatus(
                LoanStatus.ACTIVE
        );

        loanRepository.save(loan);
        
        repaymentScheduleService
        .generateSchedule(
                loan.getId()
        );

        if (responsibleUser != null) {
            try {
                com.dapfintech.employee.dto.DayBookTransactionRequest dbReq = new com.dapfintech.employee.dto.DayBookTransactionRequest();
                dbReq.setType("LOANS_DISBURSED");
                dbReq.setAmount(netDisbursedAmount);
                dbReq.setRemarks("New Loan: " + loan.getCustomer().getFullName() + " (" + loan.getLoanCode() + ")");
                dayBookService.addTransactionForDate(responsibleUser.getId(), activeDate, dbReq);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        return mapper.toResponse(
                disbursement
        );
    }

    @Override
    public DisbursementResponse getDisbursement(
            UUID loanId
    ) {

        LoanDisbursement disbursement =
                loanDisbursementRepository
                        .findByLoanId(loanId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Disbursement not found"
                                )
                        );

        return mapper.toResponse(
                disbursement
        );
    }
}