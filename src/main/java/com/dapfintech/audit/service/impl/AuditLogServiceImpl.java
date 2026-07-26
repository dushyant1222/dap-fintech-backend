package com.dapfintech.audit.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.dapfintech.audit.dto.response.AuditLogResponse;
import com.dapfintech.audit.entity.AuditLog;
import com.dapfintech.audit.repository.AuditLogRepository;
import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final LoanCollectionRepository collectionRepository;


    private String formatCustomerName(com.dapfintech.customer.entity.Customer cust) {
        if (cust == null) return "Customer";
        return (cust.getFirstName() != null ? cust.getFirstName() : "") +
               (cust.getLastName() != null && !cust.getLastName().trim().isEmpty() ? " " + cust.getLastName() : "");
    }

    @Override
    public List<AuditLogResponse> getAllLogs() {
        List<AuditLogResponse> all = new ArrayList<>();

        //db audit logs
        auditLogRepository.findAllByOrderByActionTimeDesc().stream()
                .map(this::toResponse)
                .forEach(all::add);

        //recent collections
        try {
            collectionRepository.findAll(PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "collectionDate")))
                    .getContent().forEach(col -> {
                        String empName = (col.getCollectedBy() != null && col.getCollectedBy().getFullName() != null)
                                ? col.getCollectedBy().getFullName() + " (" + (col.getCollectedBy().getRole() != null ? col.getCollectedBy().getRole().getRoleName() : "Employee") + ")"
                                : "System/Employee";
                        com.dapfintech.customer.entity.Customer custObj = (col.getRepaymentSchedule() != null && col.getRepaymentSchedule().getLoan() != null)
                                ? col.getRepaymentSchedule().getLoan().getCustomer()
                                : (col.getLoan() != null ? col.getLoan().getCustomer() : null);
                        String custName = formatCustomerName(custObj);
                        all.add(AuditLogResponse.builder()
                                .id(col.getId())
                                .userName(empName)
                                .action("Collected ₹" + (col.getCollectedAmount() != null ? col.getCollectedAmount().toBigInteger().toString() : "0") + " from " + custName)
                                .moduleName("COLLECTION")
                                .entityId(col.getId().toString())
                                .actionTime(col.getCollectionDate() != null ? col.getCollectionDate() : LocalDateTime.now())
                                .build());
                    });
        } catch (Exception ignored) {}

        //recent loans
        try {
            loanRepository.findAll(PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .getContent().forEach(loan -> {
                        String creatorName = (loan.getCreatedBy() != null && loan.getCreatedBy().getFullName() != null)
                                ? loan.getCreatedBy().getFullName() + " (" + (loan.getCreatedBy().getRole() != null ? loan.getCreatedBy().getRole().getRoleName() : "Employee") + ")"
                                : "System/Admin";
                        String custName = formatCustomerName(loan.getCustomer());
                        String actionStr = switch (loan.getLoanStatus() != null ? loan.getLoanStatus() : com.dapfintech.loan.enums.LoanStatus.PENDING_APPROVAL) {
                            case APPROVED -> "Approved Loan ₹" + (loan.getLoanAmount() != null ? loan.getLoanAmount().toBigInteger().toString() : "0") + " for " + custName;
                            case DISBURSED, ACTIVE -> "Disbursed Loan ₹" + (loan.getLoanAmount() != null ? loan.getLoanAmount().toBigInteger().toString() : "0") + " to " + custName;
                            case REJECTED -> "Rejected Loan application of ₹" + (loan.getLoanAmount() != null ? loan.getLoanAmount().toBigInteger().toString() : "0") + " for " + custName;
                            default -> "Submitted Loan application ₹" + (loan.getLoanAmount() != null ? loan.getLoanAmount().toBigInteger().toString() : "0") + " for " + custName;
                        };
                        all.add(AuditLogResponse.builder()
                                .id(loan.getId())
                                .userName(creatorName)
                                .action(actionStr)
                                .moduleName("LOAN")
                                .entityId(loan.getId().toString())
                                .actionTime(loan.getCreatedAt() != null ? loan.getCreatedAt() : LocalDateTime.now())
                                .build());
                    });
        } catch (Exception ignored) {}

        //recent cus
        try {
            customerRepository.findAll(PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .getContent().forEach(cust -> {
                        String creatorName = (cust.getCreatedBy() != null && cust.getCreatedBy().getFullName() != null)
                                ? cust.getCreatedBy().getFullName() + " (" + (cust.getCreatedBy().getRole() != null ? cust.getCreatedBy().getRole().getRoleName() : "Employee") + ")"
                                : "System/Admin";
                        all.add(AuditLogResponse.builder()
                                .id(cust.getId())
                                .userName(creatorName)
                                .action("Added customer " + formatCustomerName(cust) + (cust.getCustomerCode() != null ? " (" + cust.getCustomerCode() + ")" : ""))
                                .moduleName("CUSTOMER")
                                .entityId(cust.getId().toString())
                                .actionTime(cust.getCreatedAt() != null ? cust.getCreatedAt() : LocalDateTime.now())
                                .build());
                    });
        } catch (Exception ignored) {}

        return all.stream()
                .filter(item -> {
                    if ("SYSTEM".equalsIgnoreCase(item.getUserName()) && ("CREATE_CUSTOMER".equalsIgnoreCase(item.getAction()) || "CREATE_LOAN".equalsIgnoreCase(item.getAction()) || "COLLECTION_DONE".equalsIgnoreCase(item.getAction()))) {
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(AuditLogResponse::getActionTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(60)
                .toList();
    }

    @Override
    public void log(String userName,String action,String moduleName,String entityId) {

        AuditLog log = AuditLog.builder()
                .userId(null)
                .userName(userName)
                .action(action)
                .moduleName(moduleName)
                .entityId(entityId)
                .actionTime(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    @Override
    public void logEmployeeActivity(UUID userId,String userName,String action,String moduleName,String entityId) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID is required for employee activity log");
        }

        AuditLog log = AuditLog.builder()
                .userId(userId)
                .userName(userName)
                .action(action)
                .moduleName(moduleName)
                .entityId(entityId)
                .actionTime(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    private AuditLogResponse toResponse(AuditLog log) {

        return AuditLogResponse
                .builder()
                .id(log.getId())
                .userName(log.getUserName())
                .action(log.getAction())
                .moduleName(log.getModuleName())
                .entityId(log.getEntityId())
                .actionTime(log.getActionTime())
                .build();
    }
}