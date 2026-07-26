package com.dapfintech.loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.loan.dto.request.CloseLoanRequest;
import com.dapfintech.loan.dto.response.LoanClosureResponse;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanClosure;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.repository.LoanClosureRepository;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.service.LoanClosureService;
import com.dapfintech.security.service.AccessControlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanClosureServiceImpl
        implements LoanClosureService {

    private final LoanRepository loanRepository;

    private final LoanClosureRepository
            loanClosureRepository;

    private final LoanRepaymentScheduleRepository
            repaymentScheduleRepository;

    private final UserRepository userRepository;

    private final AccessControlService
            accessControlService;

    private final AuditLogService
            auditLogService;


    @Override
    public LoanClosureResponse closeLoan(
            UUID loanId,
            CloseLoanRequest request
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
                    "Only admin can close a loan"
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
        // STATUS VALIDATION
        //----------------------------------------------------------

        if (loan.getLoanStatus()
                != LoanStatus.ACTIVE) {

            throw new RuntimeException(
                    "Only active loans can be closed"
            );
        }


        //----------------------------------------------------------
        // DUPLICATE CLOSURE PROTECTION
        //----------------------------------------------------------

        if (loanClosureRepository
                .existsByLoanId(loanId)) {

            throw new RuntimeException(
                    "This loan has already been closed"
            );
        }


        //----------------------------------------------------------
        // CALCULATE OUTSTANDING AMOUNT
        //----------------------------------------------------------

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
                        .filter(
                                amount -> amount != null
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );


        //----------------------------------------------------------
        // OUTSTANDING MUST BE ZERO
        //----------------------------------------------------------

        if (outstandingAmount
                .compareTo(BigDecimal.ZERO) > 0) {

            throw new RuntimeException(
                    "Loan cannot be closed because outstanding amount is ₹"
                            + outstandingAmount
                                    .setScale(
                                            2,
                                            java.math.RoundingMode.HALF_UP
                                    )
            );
        }


        //----------------------------------------------------------
        // CREATE CLOSURE RECORD
        //----------------------------------------------------------

        LocalDateTime closureDate =
                LocalDateTime.now();

        LoanClosure closure =
                LoanClosure.builder()

                        .loan(loan)

                        .closureDate(
                                closureDate
                        )

                        .remarks(
                                normalizeRemarks(
                                        request
                                )
                        )

                        .build();


        LoanClosure savedClosure =
                loanClosureRepository
                        .save(closure);


        //----------------------------------------------------------
        // UPDATE LOAN STATUS
        //----------------------------------------------------------

        loan.setLoanStatus(
                LoanStatus.CLOSED
        );

        loanRepository.save(loan);


        //----------------------------------------------------------
        // AUDIT LOG
        //----------------------------------------------------------

        auditLogService.log(
                currentUser
                        .getId()
                        .toString(),
                "CLOSE_LOAN",
                "LOAN",
                loan.getId()
                        .toString()
        );


        //----------------------------------------------------------
        // RESPONSE
        //----------------------------------------------------------

        return toResponse(
                savedClosure
        );
    }


    @Override
    @Transactional(readOnly = true)
    public LoanClosureResponse getLoanClosure(
            UUID loanId
    ) {

        //----------------------------------------------------------
        // ACCESS CONTROL
        //----------------------------------------------------------

        accessControlService
                .validateLoanAccess(
                        loanId
                );


        //----------------------------------------------------------
        // FIND CLOSURE
        //----------------------------------------------------------

        LoanClosure closure =
                loanClosureRepository
                        .findByLoanId(
                                loanId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Loan closure record not found"
                                )
                        );


        //----------------------------------------------------------
        // RESPONSE
        //----------------------------------------------------------

        return toResponse(
                closure
        );
    }


    private String normalizeRemarks(
            CloseLoanRequest request
    ) {

        if (request == null ||
                request.getRemarks() == null) {

            return null;
        }

        String remarks =
                request.getRemarks()
                        .trim();

        return remarks.isEmpty()
                ? null
                : remarks;
    }


    private LoanClosureResponse toResponse(
            LoanClosure closure
    ) {

        return LoanClosureResponse
                .builder()

                .id(
                        closure.getId()
                )

                .loanId(
                        closure
                                .getLoan()
                                .getId()
                )

                .closureDate(
                        closure.getClosureDate()
                )

                .remarks(
                        closure.getRemarks()
                )

                .build();
    }
}