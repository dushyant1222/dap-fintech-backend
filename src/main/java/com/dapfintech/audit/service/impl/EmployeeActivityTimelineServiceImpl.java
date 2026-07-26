package com.dapfintech.audit.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.audit.constants.AuditActions;
import com.dapfintech.audit.dto.response.EmployeeActivityItemResponse;
import com.dapfintech.audit.dto.response.EmployeeActivityTimelineResponse;
import com.dapfintech.audit.entity.AuditLog;
import com.dapfintech.audit.repository.AuditLogRepository;
import com.dapfintech.audit.service.EmployeeActivityTimelineService;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeActivityTimelineServiceImpl implements EmployeeActivityTimelineService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public EmployeeActivityTimelineResponse getEmployeeActivityTimeline(UUID employeeId) {

        User employee = userRepository.findById(employeeId).orElseThrow(() -> new RuntimeException("Employee not found"));

        if (employee.getRole() == null || employee.getRole().getRoleName() == null || !employee.getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            throw new RuntimeException("Selected user is not an employee");
        }

        List<AuditLog> logs = auditLogRepository.findByUserIdOrderByActionTimeDesc(employeeId);
        List<EmployeeActivityItemResponse> activities = logs.stream().map(this::toActivityResponse).toList();

        return EmployeeActivityTimelineResponse
                .builder()
                .employeeId(employee.getId())
                .employeeName(employee.getFullName())
                .totalActivities(activities.size())
                .activities(activities)
                .build();
    }

    private EmployeeActivityItemResponse toActivityResponse(AuditLog log) {

        return EmployeeActivityItemResponse
                .builder()
                .id(log.getId())
                .action(log.getAction())
                .title(getActivityTitle(log.getAction()))
                .description(getActivityDescription(log))
                .moduleName(log.getModuleName())
                .entityId(log.getEntityId())
                .actionTime(log.getActionTime())
                .build();
    }

    private String getActivityTitle(String action) {

        if (action == null) {
            return "Activity Recorded";
        }

        return switch (action) {
            case AuditActions.LOGIN -> "Logged In";
            case AuditActions.CREATE_CUSTOMER -> "Customer Created";
            case AuditActions.UPDATE_CUSTOMER -> "Customer Updated";
            case AuditActions.CREATE_LOAN -> "Loan Created";
            case AuditActions.APPROVE_LOAN -> "Loan Approved";
            case AuditActions.COLLECTION_DONE -> "EMI Collected";
            case AuditActions.CREATE_VISIT -> "Visit Recorded";
            case AuditActions.PERMISSIONS_UPDATED -> "Permissions Updated";
            case AuditActions.ACTIVATE_EMPLOYEE -> "Employee Activated";
            case AuditActions.DEACTIVATE_EMPLOYEE -> "Employee Deactivated";
            case AuditActions.PASSWORD_RESET -> "Password Reset";
            default -> formatAction(action);
        };
    }

    private String getActivityDescription(AuditLog log) {

        String action = log.getAction();

        if (action == null) {
            return "An activity was recorded.";
        }

        return switch (action) {
            case AuditActions.LOGIN -> "Employee logged into the application.";
            case AuditActions.CREATE_CUSTOMER -> "Created a new customer record.";
            case AuditActions.UPDATE_CUSTOMER -> "Updated customer information.";
            case AuditActions.CREATE_LOAN -> "Created a new loan application.";
            case AuditActions.APPROVE_LOAN -> "Approved a loan application.";
            case AuditActions.COLLECTION_DONE -> "Recorded an EMI collection.";
            case AuditActions.CREATE_VISIT -> "Recorded a customer visit.";
            case AuditActions.PERMISSIONS_UPDATED -> "Employee permissions were updated.";
            case AuditActions.ACTIVATE_EMPLOYEE -> "Employee account was activated.";
            case AuditActions.DEACTIVATE_EMPLOYEE -> "Employee account was deactivated.";
            case AuditActions.PASSWORD_RESET -> "Employee password was reset.";
            default -> "Activity recorded in "+ safeModuleName(log.getModuleName())+ ".";
        };
    }

    private String formatAction(String action) {

        if (action == null ||action.isBlank()) {
            return "Activity Recorded";
        }

        String[] words = action.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)));

            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }

    private String safeModuleName(String moduleName) {

        if (moduleName == null || moduleName.isBlank()) {
        	return "the system";
        }

        return moduleName;
    }
}