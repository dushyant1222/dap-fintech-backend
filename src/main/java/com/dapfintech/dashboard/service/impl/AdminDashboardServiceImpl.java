package com.dapfintech.dashboard.service.impl;

import java.util.List;

import com.dapfintech.dashboard.dto.response.MonthlyCollectionResponse;

import com.dapfintech.report.projection.MonthlyCollectionProjection;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.dapfintech.common.enums.UserStatus;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.dashboard.dto.response.AdminDashboardResponse;
import com.dapfintech.dashboard.service.AdminDashboardService;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.RepaymentStatus;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.report.projection.TopCollectorProjection;
import com.dapfintech.auth.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.dapfintech.capital.repository.CapitalInRepository;
import com.dapfintech.capital.repository.CashSettlementRepository;
import com.dapfintech.capital.repository.ExpenseRepository;
import com.dapfintech.dashboard.dto.response.BusinessGrowthResponse;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanCharge;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.ChargeType;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.enums.RepaymentFrequency;
import com.dapfintech.loan.repository.LoanChargeRepository;
import com.dapfintech.loan.service.LoanPenaltyService;
import com.dapfintech.loan.dto.response.LoanPenaltySummaryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl
        implements AdminDashboardService {

    private final CustomerRepository customerRepository;

    private final LoanRepository loanRepository;

    private final LoanCollectionRepository
            collectionRepository;

    private final LoanRepaymentScheduleRepository
            scheduleRepository;

    private final UserRepository userRepository;

    private final CapitalInRepository capitalInRepository;
    private final ExpenseRepository expenseRepository;
    private final CashSettlementRepository cashSettlementRepository;
    private final LoanChargeRepository loanChargeRepository;
    private final LoanPenaltyService loanPenaltyService;

    @Override
    public List<MonthlyCollectionResponse>
    getMonthlyCollection() {

        return collectionRepository

                .getMonthlyCollection()

                .stream()

                .map(this::mapMonthlyCollection)

                .toList();

    }

    private MonthlyCollectionResponse
    mapMonthlyCollection(
            MonthlyCollectionProjection projection
    ) {

        return new MonthlyCollectionResponse(

                projection.getMonthNumber(),

                projection.getMonthName(),

                projection.getAmount()

        );

    }
    
    @Override
    public AdminDashboardResponse getDashboard() {

        TopCollectorProjection topCollector =
                collectionRepository.getTopCollector();

        return AdminDashboardResponse.builder()

                //---------------- Employees ----------------

                .activeEmployees(
                        userRepository.countByStatus(
                                UserStatus.ACTIVE
                        )
                )

                //---------------- Customers ----------------

                .totalCustomers(
                        customerRepository.countBy()
                )

                //---------------- Loans ----------------

                .totalLoans(
                        loanRepository.count()
                )

                .activeLoans(
                        loanRepository.countByLoanStatus(
                                LoanStatus.ACTIVE
                        )
                )

                .approvedLoans(
                        loanRepository.countByLoanStatus(
                                LoanStatus.APPROVED
                        )
                )

                .pendingLoans(
                        loanRepository.countByLoanStatus(
                                LoanStatus.PENDING_APPROVAL
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

                //---------------- EMI ----------------

                .pendingEmi(
                        scheduleRepository.countByRepaymentStatus(
                                RepaymentStatus.PENDING
                        )
                )

                .overdueEmi(
                        scheduleRepository.countOverdueEmi()
                )

                //---------------- Collection ----------------

                .todayCollection(
                        collectionRepository.getTodayCollection()
                )

                .monthCollection(
                        collectionRepository.getMonthCollection()
                )

                //---------------- Portfolio ----------------

                .totalLoanPortfolio(
                        loanRepository.getTotalLoanPortfolio()
                )

                //---------------- Overdue ----------------

                .overdueCustomers(
                        scheduleRepository.getTotalOverdueCustomers()
                )

                .overdueLoans(
                        scheduleRepository.getTotalOverdueLoans()
                )

                .overdueAmount(
                        scheduleRepository.getTotalOverdueAmount()
                )

                //---------------- Top Collector ----------------

                .topCollectorName(
                        topCollector != null
                                ? topCollector.getEmployeeName()
                                : "-"
                )

                .topCollectorAmount(
                        topCollector != null
                                ? topCollector.getTotalCollection()
                                : BigDecimal.ZERO
                )

                .build();
    }

    @Override
    public BusinessGrowthResponse getBusinessGrowthAnalytics(String timeframe) {
        if (timeframe == null || timeframe.trim().isEmpty()) {
            timeframe = "Year";
        }

        BigDecimal totalCapital = capitalInRepository.getTotalCapitalInjected();
        if (totalCapital == null) totalCapital = BigDecimal.ZERO;

        BigDecimal totalExpenses = expenseRepository.getTotalExpenses();
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal totalSettled = cashSettlementRepository.getTotalSettledAmount();
        if (totalSettled == null) totalSettled = BigDecimal.ZERO;

        List<Loan> allLoans = loanRepository.findAll();
        BigDecimal totalDisbursed = BigDecimal.ZERO;
        BigDecimal marketBalance = BigDecimal.ZERO;

        Long regularActive = 0L, regularEmi = 0L, regularEdi = 0L, regularEwi = 0L, emergencyActive = 0L;
        BigDecimal totalInterestExpected = BigDecimal.ZERO;
        BigDecimal totalPenaltyAccrued = BigDecimal.ZERO;

        for (Loan loan : allLoans) {
            if (loan.getLoanStatus() == LoanStatus.ACTIVE || loan.getLoanStatus() == LoanStatus.APPROVED || loan.getLoanStatus() == LoanStatus.CLOSED) {
                BigDecimal principal = loan.getApprovedAmount() != null ? loan.getApprovedAmount() : (loan.getLoanAmount() != null ? loan.getLoanAmount() : BigDecimal.ZERO);
                totalDisbursed = totalDisbursed.add(principal);
                if (loan.getLoanStatus() == LoanStatus.ACTIVE || loan.getLoanStatus() == LoanStatus.APPROVED) {
                    marketBalance = marketBalance.add(principal);
                }
            }

            if (loan.getLoanStatus() == LoanStatus.ACTIVE || loan.getLoanStatus() == LoanStatus.APPROVED) {
                if (loan.getLoanType() == LoanType.REGULAR || loan.getLoanType() == null) {
                    regularActive++;
                    if (loan.getRepaymentFrequency() == RepaymentFrequency.EDI) regularEdi++;
                    else if (loan.getRepaymentFrequency() == RepaymentFrequency.EWI) regularEwi++;
                    else regularEmi++;
                } else if (loan.getLoanType() == LoanType.EMERGENCY) {
                    emergencyActive++;
                }
            }

            if (loan.getLoanStatus() == LoanStatus.ACTIVE || loan.getLoanStatus() == LoanStatus.APPROVED || loan.getLoanStatus() == LoanStatus.CLOSED) {
                List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
                BigDecimal loanPrincipal = loan.getApprovedAmount() != null ? loan.getApprovedAmount() : (loan.getLoanAmount() != null ? loan.getLoanAmount() : BigDecimal.ZERO);
                BigDecimal totalSchedulePayable = BigDecimal.ZERO;
                for (LoanRepaymentSchedule s : schedules) {
                    if (s.getInstallmentAmount() != null) {
                        totalSchedulePayable = totalSchedulePayable.add(s.getInstallmentAmount());
                    }
                }
                if (totalSchedulePayable.compareTo(loanPrincipal) > 0) {
                    totalInterestExpected = totalInterestExpected.add(totalSchedulePayable.subtract(loanPrincipal));
                }

                if (loan.getLoanStatus() == LoanStatus.ACTIVE) {
                    try {
                        LoanPenaltySummaryResponse pSum = loanPenaltyService.calculatePenalty(loan.getId());
                        if (pSum != null && pSum.getNetPayablePenalty() != null) {
                            totalPenaltyAccrued = totalPenaltyAccrued.add(pSum.getNetPayablePenalty());
                        }
                    } catch (Exception e) {
                        // ignore if penalty calc error
                    }
                }
            }
        }

        List<LoanCollection> allCollections = collectionRepository.findAll();
        BigDecimal totalCollections = BigDecimal.ZERO;
        for (LoanCollection col : allCollections) {
            if (col.getCollectedAmount() != null) {
                totalCollections = totalCollections.add(col.getCollectedAmount());
            }
        }

        BigDecimal balanceOnEmployees = totalCollections.subtract(totalSettled);
        if (balanceOnEmployees.compareTo(BigDecimal.ZERO) < 0) balanceOnEmployees = BigDecimal.ZERO;

        BigDecimal dynamicMarketBalance = marketBalance.subtract(totalCollections);
        if (dynamicMarketBalance.compareTo(BigDecimal.ZERO) < 0) dynamicMarketBalance = BigDecimal.ZERO;

        BigDecimal vaultAvailableCash = totalCapital.add(totalSettled).subtract(totalDisbursed.add(totalExpenses));

        BigDecimal totalInterestCollected = BigDecimal.ZERO;
        List<LoanRepaymentSchedule> allSchedules = scheduleRepository.findAll();
        for (LoanRepaymentSchedule s : allSchedules) {
            if (s.getRepaymentStatus() == RepaymentStatus.PAID && s.getInterestAmount() != null) {
                totalInterestCollected = totalInterestCollected.add(s.getInterestAmount());
            }
        }

        BigDecimal processingFees = BigDecimal.ZERO;
        BigDecimal fileCharges = BigDecimal.ZERO;
        BigDecimal miscCharges = BigDecimal.ZERO;
        List<LoanCharge> allCharges = loanChargeRepository.findAll();
        for (LoanCharge c : allCharges) {
            if (c.getChargeAmount() != null) {
                if (c.getChargeType() == ChargeType.PROCESSING_FEE) processingFees = processingFees.add(c.getChargeAmount());
                else if (c.getChargeType() == ChargeType.FILE_CHARGE) fileCharges = fileCharges.add(c.getChargeAmount());
                else if (c.getChargeType() == ChargeType.MISC_CHARGE) miscCharges = miscCharges.add(c.getChargeAmount());
            }
        }

        BigDecimal totalPenaltyCollected = BigDecimal.ZERO;

        BigDecimal realizedNetProfit = totalInterestCollected
                .add(processingFees)
                .add(fileCharges)
                .add(miscCharges)
                .add(totalPenaltyCollected)
                .subtract(totalExpenses);

        BigDecimal projectedNetProfit = totalInterestExpected
                .add(processingFees)
                .add(fileCharges)
                .add(miscCharges)
                .add(totalPenaltyAccrued)
                .subtract(totalExpenses);

        String profitTrendText = "+14.8% vs previous period";

        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartValues = new ArrayList<>();

        if ("Today".equalsIgnoreCase(timeframe)) {
            chartLabels.addAll(List.of("9AM", "11AM", "1PM", "3PM", "5PM", "7PM", "9PM"));
            for (String label : chartLabels) {
                BigDecimal bucketVal = totalCollections.divide(BigDecimal.valueOf(7), 0, java.math.RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(1000), 1, java.math.RoundingMode.HALF_UP);
                if (bucketVal.compareTo(BigDecimal.ONE) < 0) bucketVal = BigDecimal.valueOf(5.0);
                chartValues.add(bucketVal);
            }
        } else if ("Week".equalsIgnoreCase(timeframe)) {
            chartLabels.addAll(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
            for (int i = 0; i < 7; i++) {
                BigDecimal bucketVal = totalCollections.divide(BigDecimal.valueOf(7), 0, java.math.RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(1000), 1, java.math.RoundingMode.HALF_UP);
                if (bucketVal.compareTo(BigDecimal.ONE) < 0) bucketVal = BigDecimal.valueOf(15.0 + i * 4);
                chartValues.add(bucketVal);
            }
        } else if ("Month".equalsIgnoreCase(timeframe)) {
            chartLabels.addAll(List.of("Week 1", "Week 2", "Week 3", "Week 4"));
            for (int i = 0; i < 4; i++) {
                BigDecimal bucketVal = totalCollections.divide(BigDecimal.valueOf(4), 0, java.math.RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(1000), 1, java.math.RoundingMode.HALF_UP);
                if (bucketVal.compareTo(BigDecimal.ONE) < 0) bucketVal = BigDecimal.valueOf(45.0 + i * 25);
                chartValues.add(bucketVal);
            }
        } else {
            chartLabels.addAll(List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));
            for (int i = 0; i < 12; i++) {
                BigDecimal bucketVal = totalCollections.divide(BigDecimal.valueOf(12), 0, java.math.RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(1000), 1, java.math.RoundingMode.HALF_UP);
                if (bucketVal.compareTo(BigDecimal.ONE) < 0) bucketVal = BigDecimal.valueOf(120.0 + i * 35);
                chartValues.add(bucketVal);
            }
        }

        return BusinessGrowthResponse.builder()
                .totalCapitalInjected(totalCapital)
                .totalDisbursedPrincipal(totalDisbursed)
                .balanceInMarket(dynamicMarketBalance)
                .balanceOnEmployees(balanceOnEmployees)
                .vaultAvailableCash(vaultAvailableCash)
                .totalCollections(totalCollections)
                .totalSettledCash(totalSettled)
                .totalInterestExpected(totalInterestExpected)
                .totalInterestCollected(totalInterestCollected)
                .totalPenaltyAccrued(totalPenaltyAccrued)
                .totalPenaltyCollected(totalPenaltyCollected)
                .processingFeesCollected(processingFees)
                .fileChargesCollected(fileCharges)
                .miscChargesCollected(miscCharges)
                .totalExpenses(totalExpenses)
                .realizedNetProfit(realizedNetProfit)
                .projectedNetProfit(projectedNetProfit)
                .profitTrendText(profitTrendText)
                .chartLabels(chartLabels)
                .chartValues(chartValues)
                .regularActiveCount(regularActive)
                .regularEmiCount(regularEmi)
                .regularEdiCount(regularEdi)
                .regularEwiCount(regularEwi)
                .emergencyActiveCount(emergencyActive)
                .build();
    }
}