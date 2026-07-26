package com.dapfintech.loan.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dapfintech.loan.dto.response.RepaymentScheduleResponse;
import com.dapfintech.loan.mapper.LoanRepaymentScheduleMapper;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.service.LoanRepaymentScheduleService;
import com.dapfintech.security.service.AccessControlService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;

import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.InterestType;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.enums.RepaymentFrequency;
import com.dapfintech.loan.enums.RepaymentStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanRepaymentScheduleServiceImpl
        implements LoanRepaymentScheduleService {

    private final LoanRepository loanRepository;

    private final LoanRepaymentScheduleRepository
            scheduleRepository;

    private final LoanRepaymentScheduleMapper mapper;
    private final AccessControlService accessControlService;

    @Override
    public void generateSchedule(
            UUID loanId
    ) {

        Loan loan =
                loanRepository.findById(loanId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Loan not found"
                                )
                        );

        if (loan.getLoanType() ==
                LoanType.EMERGENCY) {

            generateEmergencySchedule(
                    loan
            );

            return;
        }

        if (loan.getInterestType() ==
                InterestType.FLAT || loan.getInterestType() == InterestType.FLAT_DIRECT || loan.getInterestType() == InterestType.FLAT_PER_MONTH) {

            generateRegularFlatSchedule(
                    loan
            );
        }
        else {

            generateRegularReducingSchedule(
                    loan
            );
        }
    }
    
    private void generateEmergencySchedule(
            Loan loan
    ) {

        BigDecimal principal =
                loan.getApprovedAmount();

        BigDecimal interest =
                principal
                        .multiply(
                                loan.getInterestRate()
                        )
                        .multiply(
                                BigDecimal.valueOf(
                                		loan.getTenure()
                                )
                        )
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal totalDue =
                principal.add(interest);

        LoanRepaymentSchedule schedule =
                LoanRepaymentSchedule.builder()
                        .loan(loan)
                        .installmentNumber(1)
                        .dueDate(
                                loan.getDisbursementDate()
                                        .toLocalDate()
                                        .plusDays(
                                        		loan.getTenure()
                                        )
                        )
                        .principalAmount(
                                principal
                        )
                        .interestAmount(
                                interest
                        )
                        .installmentAmount(
                                totalDue
                        )
                        .dueAmount(
                                totalDue
                        )
                        .paidAmount(
                                BigDecimal.ZERO
                        )
                        .outstandingAmount(
                                totalDue
                        )
                        .repaymentStatus(
                                RepaymentStatus.PENDING
                        )
                        .build();

        scheduleRepository.save(
                schedule
        );
    }
    private void generateRegularFlatSchedule(
            Loan loan
    ) {

    	BigDecimal principal =
    	        loan.getApprovedAmount();

    	int tenure =
    	        loan.getTenure();

        BigDecimal totalInterest;

        if (loan.getInterestType() == InterestType.FLAT_DIRECT) {
            totalInterest = principal
                    .multiply(loan.getInterestRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (loan.getInterestType() == InterestType.FLAT_PER_MONTH) {
            BigDecimal months;
            switch (loan.getRepaymentFrequency()) {
                case EMI:
                    months = BigDecimal.valueOf(tenure);
                    break;
                case EWI:
                    months = BigDecimal.valueOf(tenure)
                            .divide(BigDecimal.valueOf(4.3333333333), 10, RoundingMode.HALF_UP);
                    break;
                case EDI:
                    months = BigDecimal.valueOf(tenure)
                            .divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP);
                    break;
                default:
                    months = BigDecimal.ONE;
            }
            totalInterest = principal
                    .multiply(loan.getInterestRate())
                    .multiply(months)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal years;
            switch (loan.getRepaymentFrequency()) {
                case EMI:
                    years = BigDecimal.valueOf(tenure)
                            .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
                    break;
                case EWI:
                    years = BigDecimal.valueOf(tenure)
                            .divide(BigDecimal.valueOf(52), 10, RoundingMode.HALF_UP);
                    break;
                case EDI:
                    years = BigDecimal.valueOf(tenure)
                            .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
                    break;
                default:
                    years = BigDecimal.ONE;
            }
            totalInterest = principal
                    .multiply(loan.getInterestRate())
                    .multiply(years)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalPayable =
                principal.add(
                        totalInterest
                );

        BigDecimal installmentAmount =
                totalPayable.divide(
                        BigDecimal.valueOf(
                                tenure
                        ),
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal principalPerInstallment =
                principal.divide(
                        BigDecimal.valueOf(
                                tenure
                        ),
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal interestPerInstallment =
                totalInterest.divide(
                        BigDecimal.valueOf(
                                tenure
                        ),
                        2,
                        RoundingMode.HALF_UP
                );

        for (int i = 1; i <= tenure; i++) {

            LoanRepaymentSchedule schedule =
                    LoanRepaymentSchedule.builder()
                            .loan(loan)
                            .installmentNumber(i)
                            .dueDate(
                                    getDueDate(
                                            loan.getDisbursementDate()
                                                    .toLocalDate(),
                                            i,
                                            loan.getRepaymentFrequency()
                                    )
                            )
                            .principalAmount(
                                    principalPerInstallment
                            )
                            .interestAmount(
                                    interestPerInstallment
                            )
                            .installmentAmount(
                                    installmentAmount
                            )
                            .dueAmount(
                                    installmentAmount
                            )
                            .paidAmount(
                                    BigDecimal.ZERO
                            )
                            .outstandingAmount(
                                    installmentAmount
                            )
                            .repaymentStatus(
                                    RepaymentStatus.PENDING
                            )
                            .build();

            scheduleRepository.save(
                    schedule
            );
        }
    }
    
    private void generateRegularReducingSchedule(
            Loan loan
    ) {

        BigDecimal principal =
                loan.getApprovedAmount();

        int tenure =
                loan.getTenure();

        BigDecimal periodicRate;

        switch (loan.getRepaymentFrequency()) {

            case EMI:

                periodicRate =
                        loan.getInterestRate()
                                .divide(
                                        BigDecimal.valueOf(1200),
                                        10,
                                        RoundingMode.HALF_UP
                                );

                break;

            case EWI:

                periodicRate =
                        loan.getInterestRate()
                                .divide(
                                        BigDecimal.valueOf(5200),
                                        10,
                                        RoundingMode.HALF_UP
                                );

                break;

            case EDI:

                periodicRate =
                        loan.getInterestRate()
                                .divide(
                                        BigDecimal.valueOf(36500),
                                        10,
                                        RoundingMode.HALF_UP
                                );

                break;

            default:

                periodicRate =
                        loan.getInterestRate()
                                .divide(
                                        BigDecimal.valueOf(1200),
                                        10,
                                        RoundingMode.HALF_UP
                                );
        }

        double r =
                periodicRate.doubleValue();

        double p =
                principal.doubleValue();

        double emi =
                p * r *
                Math.pow(
                        1 + r,
                        tenure
                )
                /
                (
                        Math.pow(
                                1 + r,
                                tenure
                        ) - 1
                );

        BigDecimal emiAmount =
                BigDecimal.valueOf(
                        emi
                ).setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal outstanding =
                principal;

        for (int i = 1; i <= tenure; i++) {

            BigDecimal interest =
                    outstanding.multiply(
                    		periodicRate
                    ).setScale(
                            2,
                            RoundingMode.HALF_UP
                    );

            BigDecimal principalComponent =
                    emiAmount.subtract(
                            interest
                    );

            outstanding =
                    outstanding.subtract(
                            principalComponent
                    );

            if (outstanding.compareTo(
                    BigDecimal.ZERO
            ) < 0) {

                outstanding =
                        BigDecimal.ZERO;
            }

            LoanRepaymentSchedule schedule =
                    LoanRepaymentSchedule.builder()
                            .loan(loan)
                            .installmentNumber(i)
                            .dueDate(
                                    getDueDate(
                                            loan.getDisbursementDate()
                                                    .toLocalDate(),
                                            i,
                                            loan.getRepaymentFrequency()
                                    )
                            )
                            .principalAmount(
                                    principalComponent
                            )
                            .interestAmount(
                                    interest
                            )
                            .installmentAmount(
                                    emiAmount
                            )
                            .dueAmount(
                                    emiAmount
                            )
                            .paidAmount(
                                    BigDecimal.ZERO
                            )
                            .outstandingAmount(
                                    emiAmount
                            )
                            .repaymentStatus(
                                    RepaymentStatus.PENDING
                            )
                            .build();

            scheduleRepository.save(
                    schedule
            );
        }
    }
    
    private LocalDate getDueDate(
            LocalDate disbursementDate,
            int installment,
            RepaymentFrequency frequency
    ) {

        return switch (frequency) {

            case EMI ->
                    disbursementDate.plusMonths(
                            installment
                    );

            case EWI ->
                    disbursementDate.plusWeeks(
                            installment
                    );

            case EDI ->
                    disbursementDate.plusDays(
                            installment
                    );
        };
    }

    @Override
    public List<RepaymentScheduleResponse>
    getLoanSchedule(
            UUID loanId
    ) {
    	accessControlService
        .validateLoanAccess(
                loanId
        );

        return scheduleRepository
                .findByLoanIdOrderByInstallmentNumberAsc(
                        loanId
                )
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}