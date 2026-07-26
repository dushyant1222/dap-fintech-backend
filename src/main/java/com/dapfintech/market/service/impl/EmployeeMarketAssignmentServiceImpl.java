package com.dapfintech.market.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.market.dto.request.AssignMarketRequest;
import com.dapfintech.market.dto.response.AssignmentResponse;
import com.dapfintech.market.entity.EmployeeMarketAssignment;
import com.dapfintech.market.entity.Market;
import com.dapfintech.market.enums.MarketStatus;
import com.dapfintech.market.mapper.EmployeeMarketAssignmentMapper;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
import com.dapfintech.market.repository.MarketRepository;
import com.dapfintech.market.service.EmployeeMarketAssignmentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeMarketAssignmentServiceImpl
        implements EmployeeMarketAssignmentService {

    private final EmployeeMarketAssignmentRepository
            assignmentRepository;

    private final MarketRepository marketRepository;

    private final UserRepository userRepository;

    private final EmployeeMarketAssignmentMapper mapper;

    private final AuditLogService auditLogService;

    // =====================================================
    // GET MY MARKETS
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getMyMarkets() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null ||
                !authentication.isAuthenticated()
        ) {

            throw new RuntimeException(
                    "Authentication required"
            );
        }

        String mobileNumber =
                authentication.getName();

        User user =
                userRepository
                        .findByMobileNumber(
                                mobileNumber
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        return getEmployeeMarkets(
                user.getId()
        );
    }

    // =====================================================
    // ASSIGN EMPLOYEE TO MARKET
    // =====================================================

    @Override
    @Transactional
    public AssignmentResponse assignMarket(
            AssignMarketRequest request
    ) {

        validateRequest(request);

        if (
                assignmentRepository
                        .existsByMarketIdAndEmployeeIdAndIsActiveTrue(
                                request.getMarketId(),
                                request.getEmployeeId()
                        )
        ) {

            throw new RuntimeException(
                    "Employee is already assigned to this market"
            );
        }

        Market market =
                marketRepository
                        .findById(
                                request.getMarketId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Market not found"
                                )
                        );

        if (
                market.getStatus() !=
                MarketStatus.ACTIVE
        ) {

            throw new RuntimeException(
                    "Cannot assign employee to an inactive market"
            );
        }

        User employee =
                userRepository
                        .findById(
                                request.getEmployeeId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Employee not found"
                                )
                        );

        if (
                employee.getRole() == null ||
                employee.getRole().getRoleName() == null ||
                !employee.getRole()
                        .getRoleName()
                        .equalsIgnoreCase(
                                "EMPLOYEE"
                        )
        ) {

            throw new RuntimeException(
                    "Selected user is not an employee"
            );
        }

        EmployeeMarketAssignment assignment =
                EmployeeMarketAssignment
                        .builder()
                        .market(
                                market
                        )
                        .employee(
                                employee
                        )
                        .assignedDate(
                                LocalDateTime.now()
                        )
                        .isActive(
                                true
                        )
                        .build();

        assignment =
                assignmentRepository.save(
                        assignment
                );

        auditLogService.log(
                employee.getFullName(),
                "ASSIGN_EMPLOYEE_TO_MARKET",
                "MARKET",
                market.getId().toString()
        );

        return mapper.toResponse(
                assignment
        );
    }

    // =====================================================
    // TRANSFER EMPLOYEE TO MARKET
    // =====================================================

    @Override
    @Transactional
    public AssignmentResponse transferMarket(
            AssignMarketRequest request
    ) {

        validateRequest(request);

        /*
         * Because employees can now belong to multiple
         * markets, "transfer" is ambiguous.
         *
         * For backward compatibility, this method:
         *
         * 1. Deactivates all current active market
         *    assignments of the employee.
         *
         * 2. Assigns the employee to the new market.
         */

        List<EmployeeMarketAssignment>
                currentAssignments =
                assignmentRepository
                        .findByEmployeeIdAndIsActiveTrue(
                                request.getEmployeeId()
                        );

        for (
                EmployeeMarketAssignment assignment
                : currentAssignments
        ) {

            assignment.setIsActive(
                    false
            );
        }

        assignmentRepository.saveAll(
                currentAssignments
        );

        return assignMarket(
                request
        );
    }

    // =====================================================
    // GET EMPLOYEE MARKETS
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse>
    getEmployeeMarkets(
            UUID employeeId
    ) {

        return assignmentRepository
                .findByEmployeeIdAndIsActiveTrue(
                        employeeId
                )
                .stream()
                .map(
                        mapper::toResponse
                )
                .toList();
    }

    // =====================================================
    // GET MARKET EMPLOYEES
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse>
    getMarketEmployees(
            UUID marketId
    ) {

        if (
                !marketRepository.existsById(
                        marketId
                )
        ) {

            throw new RuntimeException(
                    "Market not found"
            );
        }

        return assignmentRepository
                .findByMarketIdAndIsActiveTrue(
                        marketId
                )
                .stream()
                .map(
                        mapper::toResponse
                )
                .toList();
    }

    // =====================================================
    // UNASSIGN EMPLOYEE FROM MARKET
    // =====================================================

    @Override
    @Transactional
    public void unassignEmployeeFromMarket(
            UUID marketId,
            UUID employeeId
    ) {

        EmployeeMarketAssignment assignment =
                assignmentRepository
                        .findByMarketIdAndEmployeeIdAndIsActiveTrue(
                                marketId,
                                employeeId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Active employee-market assignment not found"
                                )
                        );

        assignment.setIsActive(
                false
        );

        assignmentRepository.save(
                assignment
        );

        auditLogService.log(
                assignment
                        .getEmployee()
                        .getFullName(),
                "UNASSIGN_EMPLOYEE_FROM_MARKET",
                "MARKET",
                marketId.toString()
        );
    }

    // =====================================================
    // VALIDATE REQUEST
    // =====================================================

    private void validateRequest(
            AssignMarketRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Assignment request is required"
            );
        }

        if (request.getMarketId() == null) {

            throw new RuntimeException(
                    "Market ID is required"
            );
        }

        if (request.getEmployeeId() == null) {

            throw new RuntimeException(
                    "Employee ID is required"
            );
        }
    }
}