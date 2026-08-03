package com.dapfintech.loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

        if (!"EMPLOYEE".equals(loggedInUser.getRole().getRoleName())) {
            throw new RuntimeException("Only employees can disburse loans");
        }

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

        loan.setDisbursedAmount(
                netDisbursedAmount
        );

        loan.setDisbursementDate(
        	    LocalDateTime.now());

        loan.setLoanStatus(
                LoanStatus.ACTIVE
        );

        loanRepository.save(loan);
        
        repaymentScheduleService
        .generateSchedule(
                loan.getId()
        );

        LocalDate today = LocalDate.now();
        dayBookRepository.findByEmployeeIdAndDate(loggedInUser.getId(), today).ifPresent(dayBook -> {
            if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(java.math.BigDecimal.ZERO);
            dayBook.setLoansDisbursed(dayBook.getLoansDisbursed().add(netDisbursedAmount));

            if (dayBook.getOpeningBalance() == null) dayBook.setOpeningBalance(java.math.BigDecimal.ZERO);
            if (dayBook.getCollections() == null) dayBook.setCollections(java.math.BigDecimal.ZERO);
            if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(java.math.BigDecimal.ZERO);
            if (dayBook.getSpends() == null) dayBook.setSpends(java.math.BigDecimal.ZERO);
            if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(java.math.BigDecimal.ZERO);
            if (dayBook.getOfficeRemittance() == null) dayBook.setOfficeRemittance(java.math.BigDecimal.ZERO);

            java.math.BigDecimal newClosing = dayBook.getOpeningBalance()
                    .add(dayBook.getCollections())
                    .add(dayBook.getIncomingTransfers())
                    .subtract(dayBook.getSpends())
                    .subtract(dayBook.getLoansDisbursed())
                    .subtract(dayBook.getOutgoingTransfers())
                    .subtract(dayBook.getOfficeRemittance());
            dayBook.setClosingBalance(newClosing);
            dayBookRepository.save(dayBook);
        });

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