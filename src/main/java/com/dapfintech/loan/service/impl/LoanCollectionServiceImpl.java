package com.dapfintech.loan.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.loan.dto.request.CreateCollectionRequest;
import com.dapfintech.loan.dto.response.CollectionDashboardResponse;
import com.dapfintech.loan.dto.response.CollectionHistoryResponse;
import com.dapfintech.loan.dto.response.CollectionResponse;
import com.dapfintech.loan.dto.response.PendingCollectionResponse;
import com.dapfintech.loan.dto.response.TodayScheduleResponse;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanClosure;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.CollectionStatus;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.LoanType;
import com.dapfintech.loan.enums.RepaymentStatus;
import com.dapfintech.loan.mapper.LoanCollectionMapper;
import com.dapfintech.loan.repository.LoanClosureRepository;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.loan.service.LoanCollectionService;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
import com.dapfintech.notification.service.NotificationService;
import com.dapfintech.security.service.AccessControlService;
import com.dapfintech.sync.service.SyncLogService;
import com.dapfintech.employee.repository.DayBookRepository;
import com.dapfintech.employee.entity.DayBook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.dapfintech.loan.dto.response.EmployeeCollectionOverviewResponse;
import com.dapfintech.auth.entity.User;
import com.dapfintech.market.entity.EmployeeMarketAssignment;
import com.dapfintech.loan.projection.CollectionHistoryProjection;
import com.dapfintech.loan.projection.PendingCollectionProjection;
import com.dapfintech.loan.projection.TodayScheduleProjection;
import com.dapfintech.loan.dto.response.OverdueCollectionResponse;
import com.dapfintech.report.projection.OverdueCustomerProjection;
import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanCollectionServiceImpl
        implements LoanCollectionService {

    private final LoanRepository loanRepository;
    private final LoanCollectionRepository collectionRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final LoanCollectionMapper mapper;
    private final LoanClosureRepository loanClosureRepository;
    private final UserRepository userRepository;
    private final EmployeeMarketAssignmentRepository assignmentRepository;
    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final SyncLogService syncLogService;
    private final DayBookRepository dayBookRepository;
    
    @Override
    public EmployeeCollectionOverviewResponse
    getEmployeeCollectionOverview(
            UUID employeeId
    ) {

        User employee =
                userRepository
                        .findById(employeeId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Employee not found"
                                )
                        );


        if (!employee
                .getRole()
                .getRoleName()
                .equalsIgnoreCase("EMPLOYEE")) {

            throw new RuntimeException(
                    "Selected user is not an employee"
            );

        }


        EmployeeMarketAssignment assignment =
                assignmentRepository
                        .findFirstByEmployeeIdAndIsActiveTrue(
                                employeeId
                        )
                        .orElse(null);


        UUID marketId = null;

        String marketName = null;


        if (assignment != null &&
                assignment.getMarket() != null) {

            marketId =
                    assignment
                            .getMarket()
                            .getId();

            marketName =
                    assignment
                            .getMarket()
                            .getMarketName();

        }


        List<CollectionHistoryResponse>
                history =

                collectionRepository
                        .getCollectionHistoryByEmployee(
                                employeeId
                        )
                        .stream()
                        .map(item ->

                                CollectionHistoryResponse
                                        .builder()

                                        .collectionId(
                                                item.getCollectionId()
                                        )

                                        .loanId(
                                                item.getLoanId()
                                        )

                                        .loanCode(
                                                item.getLoanCode()
                                        )

                                        .receiptNumber(
                                                item.getReceiptNumber()
                                        )

                                        .customerName(
                                                item.getCustomerName()
                                        )

                                        .mobileNumber(
                                                item.getMobileNumber()
                                        )

                                        .collectedAmount(
                                                item.getCollectedAmount()
                                        )

                                        .collectionMode(
                                                item.getCollectionMode()
                                        )

                                        .collectionStatus(
                                                item.getCollectionStatus()
                                        )

                                        .collectionDate(
                                                item.getCollectionDate()
                                        )

                                        .collectedBy(
                                                item.getCollectedBy()
                                        )

                                        .build()

                        )
                        .toList();


        Long todaySchedule = 0L;
        Long pendingCollections = 0L;
        Long overdueCustomers = 0L;
        List<TodayScheduleResponse> scheduleList = new java.util.ArrayList<>();
        if (marketId != null) {

            todaySchedule =
                    collectionRepository
                            .getTodayScheduleCountByMarket(
                                    marketId
                            );

            pendingCollections =
                    collectionRepository
                            .getPendingCollectionCountByMarket(
                                    marketId
                            );

            overdueCustomers =
                    scheduleRepository
                            .getTotalOverdueCustomersByMarket(
                                    marketId
                            );

            scheduleList = collectionRepository
                    .getTodayScheduleByMarket(marketId)
                    .stream()
                    .map(schedule -> TodayScheduleResponse.builder()
                            .loanId(schedule.getLoanId())
                            .loanCode(schedule.getLoanCode())
                            .scheduleId(schedule.getScheduleId())
                            .customerId(schedule.getCustomerId())
                            .customerName(schedule.getCustomerName())
                            .mobileNumber(schedule.getMobileNumber())
                            .installmentNumber(schedule.getInstallmentNumber())
                            .dueDate(schedule.getDueDate())
                            .installmentAmount(schedule.getInstallmentAmount())
                            .outstandingAmount(schedule.getOutstandingAmount())
                            .build())
                    .toList();
        }


        return EmployeeCollectionOverviewResponse
                .builder()

                .employeeId(
                        employee.getId()
                )

                .employeeName(
                        employee.getFullName()
                )

                .marketName(
                        marketName
                )

                .todayCollection(
                        collectionRepository
                                .getSuccessfulTodayCollectionByEmployee(
                                        employeeId
                                )
                )

                .todaySuccessfulPayments(
                        collectionRepository
                                .getSuccessfulTodayCollectionCountByEmployee(
                                        employeeId
                                )
                )

                .todaySchedule(
                        todaySchedule
                )

                .pendingCollections(
                        pendingCollections
                )

                .overdueCustomers(
                        overdueCustomers
                )

                .collectionHistory(
                        history
                )

                .todayScheduleList(
                        scheduleList
                )

                .build();

    }
    
    
    @Override
    public List<OverdueCollectionResponse>
    getOverdueCollections() {

    	User user = getLoggedInUser();

        List<OverdueCustomerProjection> projections;

        if (user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            projections =
                    scheduleRepository
                            .getOverdueCustomers();

        }
        else {

        	EmployeeMarketAssignment assignment =
        	        getEmployeeAssignment(
        	                user.getId()
        	        );

            projections =
                    scheduleRepository
                            .getOverdueCustomersByMarket(
                                    assignment
                                            .getMarket()
                                            .getId()
                            );

        }

        return projections
                .stream()
                .map(item ->

                        OverdueCollectionResponse
                                .builder()

                                .loanId(
                                        item.getLoanId()
                                )

                                .customerId(
                                        item.getCustomerId()
                                )

                                .customerName(
                                        item.getCustomerName()
                                )

                                .mobileNumber(
                                        item.getMobileNumber()
                                )

                                .marketName(
                                        item.getMarketName()
                                )

                                .overdueAmount(
                                        item.getOverdueAmount()
                                )

                                .overdueDays(
                                        item.getOverdueDays()
                                )

                                .build()

                )
                .toList();

    }
    
    @Override
    public CollectionDashboardResponse
    getDashboard() {

    	User user = getLoggedInUser();

        if (user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            return CollectionDashboardResponse
                    .builder()

                    .todayCollection(
                            collectionRepository
                                    .getTodayCollection()
                    )

                    .todaySchedule(
                            collectionRepository
                                    .getTodayScheduleCount()
                    )

                    .pendingCollections(
                            collectionRepository
                                    .getPendingCollectionCount()
                    )

                    .overdueCustomers(
                            scheduleRepository
                                    .getTotalOverdueCustomers()
                    )
                    
                 
                   

                    .build();

        }

        EmployeeMarketAssignment assignment =
                getEmployeeAssignment(
                        user.getId()
                );

        UUID marketId =
                assignment
                        .getMarket()
                        .getId();

        return CollectionDashboardResponse
                .builder()

                .todayCollection(
                        collectionRepository
                                .getTodayCollectionByEmployee(
                                        user.getId()
                                )
                )

                .todaySchedule(
                        collectionRepository
                                .getTodayScheduleCountByMarket(
                                        marketId
                                )
                )

                .pendingCollections(
                        collectionRepository
                                .getPendingCollectionCountByMarket(
                                        marketId
                                )
                )

              
                .overdueCustomers(

                        scheduleRepository
                                .getTotalOverdueCustomersByMarket(
                                        marketId
                                )

                )

                .build();

    }
    
    
    @Override
    public List<TodayScheduleResponse>
    getTodaySchedule() {

    	User user = getLoggedInUser();

        List<TodayScheduleProjection> projections;

        if (user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            projections =
                    collectionRepository
                            .getTodaySchedule();

        }
        else {

        	EmployeeMarketAssignment assignment =
        	        getEmployeeAssignment(
        	                user.getId()
        	        );

            projections =
                    collectionRepository
                            .getTodayScheduleByMarket(
                                    assignment
                                            .getMarket()
                                            .getId()
                            );

        }

        return projections
                .stream()
                .map(schedule ->

                        TodayScheduleResponse
                                .builder()

                                .loanId(
                                        schedule.getLoanId()
                                )
                                
                                .loanCode(
                                        schedule.getLoanCode()
                                )

                                .scheduleId(
                                        schedule.getScheduleId()
                                )

                                .customerId(
                                        schedule.getCustomerId()
                                )

                                .customerName(
                                        schedule.getCustomerName()
                                )

                                .mobileNumber(
                                        schedule.getMobileNumber()
                                )

                                .installmentNumber(
                                        schedule.getInstallmentNumber()
                                )

                                .dueDate(
                                        schedule.getDueDate()
                                )

                                .installmentAmount(
                                        schedule.getInstallmentAmount()
                                )

                                .outstandingAmount(
                                        schedule.getOutstandingAmount()
                                )

                                .build()

                )
                .toList();

    }
    
    @Override
    public List<PendingCollectionResponse>
    getPendingCollections() {

    	User user = getLoggedInUser();

        List<PendingCollectionProjection> projections;

        if (user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            projections =
                    collectionRepository
                            .getPendingCollections();

        }
        else {

        	EmployeeMarketAssignment assignment =
        	        getEmployeeAssignment(
        	                user.getId()
        	        );

            projections =
                    collectionRepository
                            .getPendingCollectionsByMarket(
                                    assignment
                                            .getMarket()
                                            .getId()
                            );

        }

        return projections
                .stream()
                .map(item ->

                        PendingCollectionResponse
                                .builder()

                                .loanId(
                                        item.getLoanId()
                                )

                                .scheduleId(
                                        item.getScheduleId()
                                )

                                .customerId(
                                        item.getCustomerId()
                                )

                                .customerName(
                                        item.getCustomerName()
                                )

                                .mobileNumber(
                                        item.getMobileNumber()
                                )

                                .installmentNumber(
                                        item.getInstallmentNumber()
                                )

                                .dueDate(
                                        item.getDueDate()
                                )

                                .installmentAmount(
                                        item.getInstallmentAmount()
                                )

                                .outstandingAmount(
                                        item.getOutstandingAmount()
                                )

                                .overdueDays(
                                        item.getOverdueDays()
                                )

                                .build()

                )
                .toList();

    }
    
    @Override
    public List<CollectionHistoryResponse>
    getCollectionHistory() {

        User user = getLoggedInUser();

        List<CollectionHistoryProjection> projections;

        if (user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            projections =
                    collectionRepository
                            .getCollectionHistory();

        } else {

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

            projections =
                    collectionRepository
                            .getCollectionHistoryByMarket(
                                    assignment
                                            .getMarket()
                                            .getId()
                            );

        }

        return projections
                .stream()
                .map(item ->

                        CollectionHistoryResponse
                                .builder()

                                .collectionId(
                                        item.getCollectionId()
                                )

                                .loanId(
                                        item.getLoanId()
                                )

                                .receiptNumber(
                                        item.getReceiptNumber()
                                )

                                .customerName(
                                        item.getCustomerName()
                                )

                                .mobileNumber(
                                        item.getMobileNumber()
                                )

                                .collectedAmount(
                                        item.getCollectedAmount()
                                )

                                .collectionMode(
                                        item.getCollectionMode()
                                )

                                .collectionStatus(
                                        item.getCollectionStatus()
                                )

                                .collectionDate(
                                        item.getCollectionDate()
                                )

                                .collectedBy(
                                        item.getCollectedBy()
                                )

                                .build()

                )
                .toList();

    }
    @Override
    public List<CollectionHistoryResponse>
    getCollectionHistoryByEmployee(
            UUID employeeId
    ) {

        User employee =
                userRepository.findById(
                                employeeId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Employee not found"
                                )
                        );

        if (!employee.getRole()
                .getRoleName()
                .equalsIgnoreCase("EMPLOYEE")) {

            throw new RuntimeException(
                    "Selected user is not an employee"
            );
        }

        List<CollectionHistoryProjection> projections =
                collectionRepository
                        .getCollectionHistoryByEmployee(
                                employeeId
                        );

        return projections
                .stream()
                .map(item ->

                        CollectionHistoryResponse
                                .builder()

                                .collectionId(
                                        item.getCollectionId()
                                )

                                .loanId(
                                        item.getLoanId()
                                )

                                .receiptNumber(
                                        item.getReceiptNumber()
                                )

                                .customerName(
                                        item.getCustomerName()
                                )

                                .mobileNumber(
                                        item.getMobileNumber()
                                )

                                .collectedAmount(
                                        item.getCollectedAmount()
                                )

                                .collectionMode(
                                        item.getCollectionMode()
                                )

                                .collectionStatus(
                                        item.getCollectionStatus()
                                )

                                .collectionDate(
                                        item.getCollectionDate()
                                )

                                .collectedBy(
                                        item.getCollectedBy()
                                )

                                .build()

                )
                .toList();
    }
   
    @Override
    @Transactional
    public CollectionResponse collectPayment(
            CreateCollectionRequest request
    ) {

        if (request.getLoanId() == null) {
            throw new RuntimeException(
                    "Loan ID is required"
            );
        }

        if (request.getCollectedAmount() == null ||
                request.getCollectedAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Collection amount must be greater than zero"
            );
        }

        if (request.getCollectionMode() == null) {
            throw new RuntimeException(
                    "Collection mode is required"
            );
        }

        Loan loan =
                loanRepository.findById(
                                request.getLoanId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Loan not found"
                                )
                        );

        User loggedInEmployee =
                getLoggedInUser();

        Customer customer =
                loan.getCustomer();

        if (customer == null) {
            throw new RuntimeException(
                    "Customer not found for this loan"
            );
        }

        if (customer.getMarket() == null) {
            throw new RuntimeException(
                    "Customer market not assigned"
            );
        }

        /*
         * ADMIN can collect for any customer.
         *
         * EMPLOYEE must have an active assignment
         * for the customer's market.
         */
        boolean isAdmin =
                loggedInEmployee
                        .getRole()
                        .getRoleName()
                        .equalsIgnoreCase("ADMIN");

        if (!isAdmin) {

            boolean authorized =
                    assignmentRepository
                            .existsByMarketIdAndEmployeeIdAndIsActiveTrue(
                                    customer.getMarket().getId(),
                                    loggedInEmployee.getId()
                            );

            if (!authorized) {
                throw new RuntimeException(
                        "You are not authorized to collect for this customer"
                );
            }
        }

        List<LoanRepaymentSchedule> schedules =
                scheduleRepository
                        .findByLoanIdOrderByInstallmentNumberAsc(
                                loan.getId()
                        );

        java.util.List<LoanRepaymentSchedule> unpaidSchedules = new java.util.ArrayList<>();
        if (request.getScheduleId() != null) {
            LoanRepaymentSchedule targetSchedule = schedules.stream()
                    .filter(s -> s.getId().equals(request.getScheduleId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Target schedule not found in this loan"));
            
            if (targetSchedule.getRepaymentStatus() == RepaymentStatus.PAID) {
                throw new RuntimeException("This schedule is already fully paid");
            }
            
            if (request.getCollectedAmount().compareTo(targetSchedule.getOutstandingAmount()) > 0) {
                throw new RuntimeException("Collection amount cannot exceed the specific EMI's outstanding amount");
            }
            
            unpaidSchedules.add(targetSchedule);
        } else {
            unpaidSchedules = schedules.stream()
                    .filter(schedule -> schedule.getRepaymentStatus() != RepaymentStatus.PAID)
                    .toList();
        }

        if (unpaidSchedules.isEmpty()) {
            throw new RuntimeException(
                    "Loan already closed"
            );
        }

        BigDecimal requestedAmount =
                request.getCollectedAmount();

        LoanRepaymentSchedule collectionReferenceSchedule =
                unpaidSchedules.get(0);

        BigDecimal amountToAdjust =
                requestedAmount;

        for (LoanRepaymentSchedule schedule
                : unpaidSchedules) {

            if (amountToAdjust.compareTo(
                    BigDecimal.ZERO
            ) <= 0) {
                break;
            }

            BigDecimal outstanding =
                    schedule.getOutstandingAmount();

            if (outstanding == null ||
                    outstanding.compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                continue;
            }

            BigDecimal amountToApply = amountToAdjust.min(outstanding);

            BigDecimal currentPaidAmount =
                    schedule.getPaidAmount() == null
                            ? BigDecimal.ZERO
                            : schedule.getPaidAmount();

            schedule.setPaidAmount(
                    currentPaidAmount.add(
                            amountToApply
                    )
            );

            schedule.setOutstandingAmount(
                    outstanding.subtract(
                            amountToApply
                    )
            );

            if (schedule.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                schedule.setRepaymentStatus(
                        RepaymentStatus.PAID
                );
            }

            scheduleRepository.save(
                    schedule
            );

            amountToAdjust =
                    amountToAdjust.subtract(
                            amountToApply
                    );
        }

        if (loan.getLoanType() == LoanType.EMERGENCY) {
            if (amountToAdjust.compareTo(loan.getApprovedAmount()) >= 0) {
                // Collect principal and close loan
                loan.setLoanStatus(LoanStatus.CLOSED);
                loanRepository.save(loan);

                LoanClosure closure = LoanClosure.builder()
                        .loan(loan)
                        .closureDate(java.time.LocalDateTime.now())
                        .remarks("Closed via EMI collection (Principal Returned)")
                        .build();
                loanClosureRepository.save(closure);
                
                amountToAdjust = amountToAdjust.subtract(loan.getApprovedAmount());
            }
        }

        LoanCollection collection =
                LoanCollection.builder()
                        .loan(loan)
                        .repaymentSchedule(
                                collectionReferenceSchedule
                        )
                        .receiptNumber(
                                generateReceiptNumber()
                        )
                        .collectedAmount(
                                requestedAmount
                        )
                        .collectionDate(
                                request.getCollectionDate() != null
                                        ? request.getCollectionDate()
                                        : java.time.LocalDateTime.now()
                        )
                        .collectionMode(
                                request.getCollectionMode()
                        )
                        .collectionStatus(
                                CollectionStatus.SUCCESS
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .collectedBy(
                                loggedInEmployee
                        )
                        .latitude(
                                request.getLatitude()
                        )
                        .longitude(
                                request.getLongitude()
                        )
                        .build();

        collection =
                collectionRepository.save(
                        collection
                );

        syncLogService.logSync(
                "COLLECTION",
                collection.getId().toString()
        );

        notificationService.createNotification(
                "Collection Received",
                "Collection of ₹" +
                        collection.getCollectedAmount() +
                        " received."
        );
        if (loan != null && loan.getCreatedBy() != null) {
            notificationService.createNotificationForUser(
                "Collection Received",
                "Collection of ₹" + collection.getCollectedAmount() + " received for your loan.",
                loan.getCreatedBy()
            );
        }

        String collectorLog = (loggedInEmployee != null && loggedInEmployee.getFullName() != null)
                ? loggedInEmployee.getFullName() + " (" + (loggedInEmployee.getRole() != null ? loggedInEmployee.getRole().getRoleName() : "Employee") + ")"
                : "System/User";
        String custNameLog = (loan != null && loan.getCustomer() != null)
                ? (loan.getCustomer().getFirstName() != null ? loan.getCustomer().getFirstName() : "") + (loan.getCustomer().getLastName() != null ? " " + loan.getCustomer().getLastName() : "")
                : "Customer";
        auditLogService.log(
                collectorLog,
                "Collected ₹" + (collection.getCollectedAmount() != null ? collection.getCollectedAmount().toBigInteger().toString() : "0") + " from " + custNameLog.trim(),
                "COLLECTION",
                collection.getId().toString()
        );

        boolean allPaid =
                scheduleRepository
                        .findByLoanIdOrderByInstallmentNumberAsc(
                                loan.getId()
                        )
                        .stream()
                        .allMatch(
                                schedule ->
                                        schedule.getRepaymentStatus()
                                                == RepaymentStatus.PAID
                        );

        if (allPaid) {

            loan.setLoanStatus(
                    LoanStatus.CLOSED
            );

            loanRepository.save(
                    loan
            );

            LoanClosure closure =
                    LoanClosure.builder()
                            .loan(loan)
                            .closureDate(
                                    java.time.LocalDateTime.now()
                            )
                            .remarks(
                                    "Auto closed after final payment"
                            )
                            .build();

            loanClosureRepository.save(
                    closure
            );
        }

        // Update DayBook if collected by an employee
        if (loggedInEmployee != null && loggedInEmployee.getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            java.time.LocalDate today = java.time.LocalDate.now();
            final java.math.BigDecimal finalCollectedAmount = collection.getCollectedAmount();
            dayBookRepository.findByEmployeeIdAndDate(loggedInEmployee.getId(), today).ifPresent(dayBook -> {
                if (dayBook.getCollections() == null) {
                    dayBook.setCollections(java.math.BigDecimal.ZERO);
                }
                dayBook.setCollections(dayBook.getCollections().add(finalCollectedAmount));
                
                // Recalculate closing balance
                if (dayBook.getOpeningBalance() == null) dayBook.setOpeningBalance(java.math.BigDecimal.ZERO);
                if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(java.math.BigDecimal.ZERO);
                if (dayBook.getSpends() == null) dayBook.setSpends(java.math.BigDecimal.ZERO);
                if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(java.math.BigDecimal.ZERO);
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
        }

        return mapper.toResponse(
                collection
        );
    }
    
    @Override
    public CollectionResponse getCollectionById(
            UUID collectionId
    ) {
    	accessControlService
        .validateCollectionAccess(
                collectionId
        );

        LoanCollection collection =
                collectionRepository.findById(
                        collectionId
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Collection not found"
                        )
                );

        return mapper.toResponse(
                collection
        );
    }

    @Override
    public List<CollectionResponse>
    getLoanCollections(
            UUID loanId
    ) {
    	accessControlService
        .validateLoanAccess(
                loanId
        );

        return collectionRepository
                .findByLoanId(loanId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////
    private User getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        return userRepository
                .findByMobileNumber(
                        mobileNumber
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "User not found"
                        )
                );

    }
    private EmployeeMarketAssignment getEmployeeAssignment(
            UUID employeeId
    ) {

        return assignmentRepository
                .findFirstByEmployeeIdAndIsActiveTrue(
                        employeeId
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "No market assigned"
                        )
                );

    }
    
    private String generateReceiptNumber() {

        return "RCPT-" +
                System.currentTimeMillis();
    }
}