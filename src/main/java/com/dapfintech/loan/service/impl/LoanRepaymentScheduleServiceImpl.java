package com.dapfintech.loan.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

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
import com.dapfintech.loan.enums.LoanStatus;
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
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );

        // First day's schedule only contains interest
        BigDecimal totalDue = interest;

        LoanRepaymentSchedule schedule =
                LoanRepaymentSchedule.builder()
                        .loan(loan)
                        .installmentNumber(1)
                        .dueDate(
                                loan.getDisbursementDate()
                                        .toLocalDate()
                        )
                        .principalAmount(
                                BigDecimal.ZERO
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

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        List<LoanRepaymentSchedule> all = scheduleRepository
                .findByLoanIdOrderByInstallmentNumberAsc(loanId);

        // For EMERGENCY loans: only show entries up to and including today
        // (no future projections — principal collected only at closure)
        if (loan.getLoanType() == LoanType.EMERGENCY) {
            LocalDate today = LocalDate.now();
            return all.stream()
                    .filter(s -> !s.getDueDate().isAfter(today))
                    .map(mapper::toResponse)
                    .toList();
        }

        return all.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Scheduled(cron = "0 1 0 * * ?")
    public void generateDailyEmergencySchedules() {
        List<Loan> emergencyLoans = loanRepository.findByLoanTypeAndLoanStatus(LoanType.EMERGENCY, LoanStatus.ACTIVE);
        LocalDate today = LocalDate.now();

        for (Loan loan : emergencyLoans) {
            BigDecimal principal = loan.getApprovedAmount();
            // Daily interest is always: principal × rate% (flat on original principal)
            BigDecimal dailyInterest = principal
                    .multiply(loan.getInterestRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            List<LoanRepaymentSchedule> existingSchedules =
                    scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());

            // Find the last date that has an entry
            LocalDate lastEntryDate = existingSchedules.stream()
                    .map(LoanRepaymentSchedule::getDueDate)
                    .max(LocalDate::compareTo)
                    .orElse(loan.getDisbursementDate() != null
                            ? loan.getDisbursementDate().toLocalDate().minusDays(1)
                            : today.minusDays(1));

            int nextInstallment = existingSchedules.size() + 1;

            // Backfill any missed days (e.g. server downtime) AND add today
            LocalDate fillDate = lastEntryDate.plusDays(1);
            while (!fillDate.isAfter(today)) {
                final LocalDate checkDate = fillDate;
                boolean alreadyExists = existingSchedules.stream()
                        .anyMatch(s -> s.getDueDate().equals(checkDate));

                if (!alreadyExists) {
                    LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                            .loan(loan)
                            .installmentNumber(nextInstallment)
                            .dueDate(checkDate)
                            .principalAmount(BigDecimal.ZERO)
                            .interestAmount(dailyInterest)
                            .installmentAmount(dailyInterest)
                            .dueAmount(dailyInterest)
                            .paidAmount(BigDecimal.ZERO)
                            .outstandingAmount(dailyInterest)
                            .repaymentStatus(RepaymentStatus.PENDING)
                            .build();

                    scheduleRepository.save(schedule);
                    nextInstallment++;
                }
                fillDate = fillDate.plusDays(1);
            }
        }
    }
}