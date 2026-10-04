package com.dapfintech.loan.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    private final com.dapfintech.employee.service.DayBookService dayBookService;
    private final com.dapfintech.employee.repository.DayBookTransactionRepository dayBookTransactionRepository;
    
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
                            .customerCode(schedule.getCustomerCode())
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

        if (user.isAdmin()) {

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

        if (user.isAdmin()) {

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

        if (user.isAdmin()) {

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

                                .customerCode(
                                        schedule.getCustomerCode()
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

        if (user.isAdmin()) {

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

        if (user.isAdmin()) {

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

        /*
         * ADMIN can collect for any customer.
         *
         * EMPLOYEE must have an active assignment
         * for the customer's market.
         */
        boolean isAdmin = loggedInEmployee.isAdmin();

        if (!isAdmin) {
            if (customer.getMarket() == null) {
                throw new RuntimeException(
                        "Customer market not assigned"
                );
            }

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

        List<LoanRepaymentSchedule> schedulesToSave = new ArrayList<>();

        for (LoanRepaymentSchedule schedule : unpaidSchedules) {

            if (amountToAdjust.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal outstanding = schedule.getOutstandingAmount();

            if (outstanding == null || outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal amountToApply = amountToAdjust.min(outstanding);

            BigDecimal currentPaidAmount = schedule.getPaidAmount() == null ? BigDecimal.ZERO : schedule.getPaidAmount();

            schedule.setPaidAmount(currentPaidAmount.add(amountToApply));
            schedule.setOutstandingAmount(outstanding.subtract(amountToApply));

            if (schedule.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                schedule.setRepaymentStatus(RepaymentStatus.PAID);
            }

            schedulesToSave.add(schedule);
            amountToAdjust = amountToAdjust.subtract(amountToApply);
        }
        
        scheduleRepository.saveAll(schedulesToSave);


        if (loan.getLoanType() == LoanType.EMERGENCY) {
            BigDecimal principal = loan.getDisbursedAmount() != null ? loan.getDisbursedAmount()
                    : (loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getLoanAmount());
            if (principal == null) principal = BigDecimal.ZERO;

            if (principal.compareTo(BigDecimal.ZERO) > 0 && amountToAdjust.compareTo(principal) >= 0) {
                // Collect principal and close loan
                loan.setLoanStatus(LoanStatus.CLOSED);
                loanRepository.save(loan);

                LoanClosure closure = LoanClosure.builder()
                        .loan(loan)
                        .closureDate(java.time.LocalDateTime.now())
                        .remarks("Closed via EMI collection (Principal Returned)")
                        .build();
                loanClosureRepository.save(closure);

                amountToAdjust = amountToAdjust.subtract(principal);
            }
        }

            // Determine employee for attribution
        User targetEmployee = null;
        if (loggedInEmployee != null && loggedInEmployee.getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            targetEmployee = loggedInEmployee;
        } else if (customer != null && customer.getMarket() != null) {
            List<com.dapfintech.market.entity.EmployeeMarketAssignment> assigns = 
                    assignmentRepository.findActiveOrNullByMarketId(customer.getMarket().getId());
            if (assigns == null || assigns.isEmpty()) {
                assigns = assignmentRepository.findByMarketId(customer.getMarket().getId());
            }
            if (assigns != null && !assigns.isEmpty()) {
                targetEmployee = assigns.get(0).getEmployee();
            }
        }
        if (targetEmployee == null && loan != null && loan.getCreatedBy() != null &&
                loan.getCreatedBy().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            targetEmployee = loan.getCreatedBy();
        }

        java.time.LocalDateTime colDateTime;
        if (request.getCollectionDate() != null) {
            colDateTime = request.getCollectionDate();
            if (colDateTime.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
                colDateTime = colDateTime.toLocalDate().atTime(java.time.LocalTime.now());
            }
        } else {
            java.time.LocalDate activeColDate = dayBookService.getActiveDayBookDate(
                    targetEmployee != null ? targetEmployee.getId() : (loggedInEmployee != null ? loggedInEmployee.getId() : null)
            );
            colDateTime = activeColDate.atTime(java.time.LocalTime.now());
        }

        java.time.LocalDate colDate = colDateTime.toLocalDate();
        UUID targetMarketId = null;
        if (customer != null && customer.getMarket() != null) {
            targetMarketId = customer.getMarket().getId();
        } else if (targetEmployee != null) {
            List<com.dapfintech.market.entity.EmployeeMarketAssignment> empAsgs = assignmentRepository.findByEmployeeId(targetEmployee.getId());
            if (empAsgs != null && !empAsgs.isEmpty() && empAsgs.get(0).getMarket() != null) {
                targetMarketId = empAsgs.get(0).getMarket().getId();
            }
        }

        // Sequential DayBook rule: If previous DayBook is not closed, no values can go into this DayBook
        if (targetMarketId != null) {
            com.dapfintech.employee.dto.MarketDayBookResponse mdbResp = dayBookService.getOrCreateMarketDayBook(targetMarketId, colDate);
            if (!mdbResp.isPreviousDayClosed() && mdbResp.getUnclosedDate() != null) {
                throw new RuntimeException("Cannot collect EMI for " + colDate + " because previous Market DayBook for " +
                        mdbResp.getUnclosedDate() + " is not closed yet. Please close the DayBook for " + mdbResp.getUnclosedDate() + " first.");
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
                                colDateTime
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
                                targetEmployee != null ? targetEmployee : loggedInEmployee
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

        // Avoid double fetch: Just check if outstanding is zero using our aggregate query or the list we already fetched
        BigDecimal totalOutstanding = scheduleRepository.getSumOutstandingByLoan(loan.getId());
        boolean allPaid = (totalOutstanding == null || totalOutstanding.compareTo(BigDecimal.ZERO) <= 0);

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

        // Update DayBook for target employee and market on collection date
        if (targetEmployee != null) {
            try {
                com.dapfintech.employee.dto.DayBookTransactionRequest dbReq = new com.dapfintech.employee.dto.DayBookTransactionRequest();
                dbReq.setType("COLLECTIONS");
                dbReq.setAmount(collection.getCollectedAmount());
                String custName = customer != null ? customer.getFullName() : "";
                dbReq.setRemarks("EMI Collected: " + custName + " (" + loan.getLoanCode() + ")");
                dayBookService.addTransactionForDate(targetEmployee.getId(), colDate, dbReq);
            } catch(Exception e) {
                log.error("Failed to add collection transaction to DayBook: {}", e.getMessage(), e);
            }
        }
        if (targetMarketId != null) {
            try {
                dayBookService.syncMarketDayBook(targetMarketId, colDate);
            } catch(Exception e) {
                log.error("Failed to sync Market DayBook for collection date: {}", e.getMessage(), e);
            }
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

    @Override
    @Transactional
    public void deleteCollection(UUID collectionId) {
        User loggedInUser = getLoggedInUser();
        if (!loggedInUser.isAdmin()) {
            throw new RuntimeException("Only administrators can delete collections");
        }

        LoanCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new RuntimeException("Collection record not found with ID: " + collectionId));

        Loan loan = collection.getLoan();
        BigDecimal amountToRollback = collection.getCollectedAmount() != null
                ? collection.getCollectedAmount() : BigDecimal.ZERO;

        log.info("Deleting collection {} of amount {} for loan {}", collectionId, amountToRollback, loan != null ? loan.getLoanCode() : "-");

        // 1. Rollback schedule payments in reverse order (installmentNumber DESC)
        if (loan != null && amountToRollback.compareTo(BigDecimal.ZERO) > 0) {
            List<LoanRepaymentSchedule> schedules = scheduleRepository
                    .findByLoanIdOrderByInstallmentNumberAsc(loan.getId());

            // Reverse order so latest payments are undone first
            List<LoanRepaymentSchedule> reversedSchedules = new ArrayList<>(schedules);
            java.util.Collections.reverse(reversedSchedules);

            BigDecimal remainingRollback = amountToRollback;
            List<LoanRepaymentSchedule> toUpdate = new ArrayList<>();

            for (LoanRepaymentSchedule schedule : reversedSchedules) {
                if (remainingRollback.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal paid = schedule.getPaidAmount() != null ? schedule.getPaidAmount() : BigDecimal.ZERO;
                if (paid.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal deduction = remainingRollback.min(paid);
                schedule.setPaidAmount(paid.subtract(deduction));
                schedule.setOutstandingAmount(
                        (schedule.getOutstandingAmount() != null ? schedule.getOutstandingAmount() : BigDecimal.ZERO).add(deduction)
                );

                if (schedule.getDueDate() != null && schedule.getDueDate().isBefore(java.time.LocalDate.now())) {
                    schedule.setRepaymentStatus(RepaymentStatus.OVERDUE);
                } else {
                    schedule.setRepaymentStatus(RepaymentStatus.PENDING);
                }

                toUpdate.add(schedule);
                remainingRollback = remainingRollback.subtract(deduction);
            }

            if (!toUpdate.isEmpty()) {
                scheduleRepository.saveAll(toUpdate);
            }

            // If loan was CLOSED, reopen it to ACTIVE
            if (loan.getLoanStatus() == LoanStatus.CLOSED) {
                loan.setLoanStatus(LoanStatus.ACTIVE);
                loanRepository.save(loan);
                loanClosureRepository.findByLoanId(loan.getId()).ifPresent(loanClosureRepository::delete);
            }
        }

        // 2. DayBook resync & transaction removal
        UUID targetMarketId = null;
        if (loan != null && loan.getCustomer() != null && loan.getCustomer().getMarket() != null) {
            targetMarketId = loan.getCustomer().getMarket().getId();
        } else if (collection.getCollectedBy() != null) {
            List<EmployeeMarketAssignment> empAsgs = assignmentRepository.findByEmployeeId(collection.getCollectedBy().getId());
            if (empAsgs != null && !empAsgs.isEmpty() && empAsgs.get(0).getMarket() != null) {
                targetMarketId = empAsgs.get(0).getMarket().getId();
            }
        }

        java.time.LocalDate colDate = collection.getCollectionDate() != null
                ? collection.getCollectionDate().toLocalDate()
                : java.time.LocalDate.now();

        // Remove matching DayBook transaction if present
        if (targetMarketId != null && loan != null) {
            try {
                java.time.LocalDateTime start = colDate.atStartOfDay();
                java.time.LocalDateTime end = colDate.plusDays(1).atStartOfDay();
                List<com.dapfintech.employee.entity.DayBookTransaction> txs = dayBookTransactionRepository
                        .findByMarketIdAndCreatedAtBetween(targetMarketId, start, end);
                for (com.dapfintech.employee.entity.DayBookTransaction tx : txs) {
                    if ("COLLECTIONS".equalsIgnoreCase(tx.getType()) &&
                            tx.getRemarks() != null && tx.getRemarks().contains(loan.getLoanCode()) &&
                            tx.getAmount() != null && tx.getAmount().compareTo(amountToRollback) == 0) {
                        dayBookTransactionRepository.delete(tx);
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Could not delete matching DayBookTransaction: {}", e.getMessage());
            }
        }

        // 3. Delete the collection record
        collectionRepository.delete(collection);

        // 4. Sync Market DayBook for that date
        if (targetMarketId != null) {
            try {
                dayBookService.syncMarketDayBook(targetMarketId, colDate);
            } catch (Exception e) {
                log.error("Failed to sync Market DayBook after collection deletion: {}", e.getMessage(), e);
            }
        }

        // 5. Audit Log
        String adminLog = loggedInUser.getFullName() != null ? loggedInUser.getFullName() : "Admin";
        String loanCode = loan != null ? loan.getLoanCode() : "-";
        auditLogService.log(
                adminLog,
                "Admin deleted collection #" + (collection.getReceiptNumber() != null ? collection.getReceiptNumber() : collectionId) + " of ₹" + amountToRollback + " for loan " + loanCode,
                "COLLECTION_DELETED",
                collectionId.toString()
        );
    }

    @Override
    @Transactional
    public CollectionResponse updateCollection(UUID collectionId, com.dapfintech.loan.dto.request.UpdateCollectionRequest request) {
        User loggedInUser = getLoggedInUser();
        if (!loggedInUser.isAdmin()) {
            throw new RuntimeException("Only administrators can edit collections");
        }

        LoanCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new RuntimeException("Collection record not found with ID: " + collectionId));

        if (request.getCollectedAmount() == null || request.getCollectedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Updated collection amount must be greater than zero");
        }

        Loan loan = collection.getLoan();
        BigDecimal oldAmount = collection.getCollectedAmount() != null ? collection.getCollectedAmount() : BigDecimal.ZERO;
        BigDecimal newAmount = request.getCollectedAmount();
        BigDecimal diff = newAmount.subtract(oldAmount);

        java.time.LocalDate oldColDate = collection.getCollectionDate() != null
                ? collection.getCollectionDate().toLocalDate()
                : java.time.LocalDate.now();

        java.time.LocalDate newColDate = request.getCollectionDate() != null
                ? request.getCollectionDate().toLocalDate()
                : oldColDate;

        log.info("Updating collection {}: oldAmount={}, newAmount={}, diff={}", collectionId, oldAmount, newAmount, diff);

        // Adjust schedules based on difference
        if (loan != null && diff.compareTo(BigDecimal.ZERO) != 0) {
            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                // Amount was REDUCED: Rollback abs(diff) from schedules in reverse order
                BigDecimal amountToRollback = diff.abs();
                List<LoanRepaymentSchedule> schedules = scheduleRepository
                        .findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
                List<LoanRepaymentSchedule> reversed = new ArrayList<>(schedules);
                java.util.Collections.reverse(reversed);

                List<LoanRepaymentSchedule> toUpdate = new ArrayList<>();
                for (LoanRepaymentSchedule schedule : reversed) {
                    if (amountToRollback.compareTo(BigDecimal.ZERO) <= 0) break;
                    BigDecimal paid = schedule.getPaidAmount() != null ? schedule.getPaidAmount() : BigDecimal.ZERO;
                    if (paid.compareTo(BigDecimal.ZERO) <= 0) continue;

                    BigDecimal deduction = amountToRollback.min(paid);
                    schedule.setPaidAmount(paid.subtract(deduction));
                    schedule.setOutstandingAmount(
                            (schedule.getOutstandingAmount() != null ? schedule.getOutstandingAmount() : BigDecimal.ZERO).add(deduction)
                    );
                    if (schedule.getDueDate() != null && schedule.getDueDate().isBefore(java.time.LocalDate.now())) {
                        schedule.setRepaymentStatus(RepaymentStatus.OVERDUE);
                    } else {
                        schedule.setRepaymentStatus(RepaymentStatus.PENDING);
                    }
                    toUpdate.add(schedule);
                    amountToRollback = amountToRollback.subtract(deduction);
                }
                if (!toUpdate.isEmpty()) {
                    scheduleRepository.saveAll(toUpdate);
                }
                // If loan was closed, reopen it
                if (loan.getLoanStatus() == LoanStatus.CLOSED) {
                    loan.setLoanStatus(LoanStatus.ACTIVE);
                    loanRepository.save(loan);
                    loanClosureRepository.findByLoanId(loan.getId()).ifPresent(loanClosureRepository::delete);
                }
            } else {
                // Amount was INCREASED: Apply diff to unpaid schedules in forward order
                BigDecimal amountToApply = diff;
                List<LoanRepaymentSchedule> unpaid = scheduleRepository
                        .findByLoanIdOrderByInstallmentNumberAsc(loan.getId())
                        .stream()
                        .filter(s -> s.getRepaymentStatus() != RepaymentStatus.PAID)
                        .toList();

                List<LoanRepaymentSchedule> toUpdate = new ArrayList<>();
                for (LoanRepaymentSchedule schedule : unpaid) {
                    if (amountToApply.compareTo(BigDecimal.ZERO) <= 0) break;
                    BigDecimal outstanding = schedule.getOutstandingAmount() != null ? schedule.getOutstandingAmount() : BigDecimal.ZERO;
                    if (outstanding.compareTo(BigDecimal.ZERO) <= 0) continue;

                    BigDecimal apply = amountToApply.min(outstanding);
                    schedule.setPaidAmount((schedule.getPaidAmount() != null ? schedule.getPaidAmount() : BigDecimal.ZERO).add(apply));
                    schedule.setOutstandingAmount(outstanding.subtract(apply));
                    if (schedule.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        schedule.setRepaymentStatus(RepaymentStatus.PAID);
                    }
                    toUpdate.add(schedule);
                    amountToApply = amountToApply.subtract(apply);
                }
                if (!toUpdate.isEmpty()) {
                    scheduleRepository.saveAll(toUpdate);
                }

                // Check if all schedules are now paid
                BigDecimal totalOutstanding = scheduleRepository.getSumOutstandingByLoan(loan.getId());
                if (totalOutstanding == null || totalOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
                    loan.setLoanStatus(LoanStatus.CLOSED);
                    loanRepository.save(loan);
                    if (!loanClosureRepository.existsByLoanId(loan.getId())) {
                        LoanClosure closure = LoanClosure.builder()
                                .loan(loan)
                                .closureDate(java.time.LocalDateTime.now())
                                .remarks("Auto closed after collection update")
                                .build();
                        loanClosureRepository.save(closure);
                    }
                }
            }
        }

        // Update collection fields
        collection.setCollectedAmount(newAmount);
        if (request.getCollectionMode() != null) {
            collection.setCollectionMode(request.getCollectionMode());
        }
        if (request.getCollectionDate() != null) {
            collection.setCollectionDate(request.getCollectionDate());
        }
        if (request.getRemarks() != null) {
            collection.setRemarks(request.getRemarks());
        }
        collection = collectionRepository.save(collection);

        // DayBook resync
        UUID targetMarketId = null;
        if (loan != null && loan.getCustomer() != null && loan.getCustomer().getMarket() != null) {
            targetMarketId = loan.getCustomer().getMarket().getId();
        } else if (collection.getCollectedBy() != null) {
            List<EmployeeMarketAssignment> empAsgs = assignmentRepository.findByEmployeeId(collection.getCollectedBy().getId());
            if (empAsgs != null && !empAsgs.isEmpty() && empAsgs.get(0).getMarket() != null) {
                targetMarketId = empAsgs.get(0).getMarket().getId();
            }
        }

        if (targetMarketId != null) {
            try {
                // If date changed, sync old date DayBook first
                if (!oldColDate.isEqual(newColDate)) {
                    dayBookService.syncMarketDayBook(targetMarketId, oldColDate);
                }
                dayBookService.syncMarketDayBook(targetMarketId, newColDate);
            } catch (Exception e) {
                log.error("Failed to sync Market DayBook after collection update: {}", e.getMessage(), e);
            }
        }

        // Audit Log
        String adminLog = loggedInUser.getFullName() != null ? loggedInUser.getFullName() : "Admin";
        String loanCode = loan != null ? loan.getLoanCode() : "-";
        auditLogService.log(
                adminLog,
                "Admin updated collection #" + (collection.getReceiptNumber() != null ? collection.getReceiptNumber() : collectionId) + " from ₹" + oldAmount + " to ₹" + newAmount + " for loan " + loanCode,
                "COLLECTION_UPDATED",
                collectionId.toString()
        );

        return mapper.toResponse(collection);
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