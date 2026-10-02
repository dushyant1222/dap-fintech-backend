package com.dapfintech.auth.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.dapfintech.auth.dto.response.EmployeeResponse;
import com.dapfintech.auth.entity.User;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
import com.dapfintech.market.entity.EmployeeMarketAssignment;
import java.util.UUID;

@Component
public class EmployeeMapper {

    @Autowired(required = false)
    private EmployeeMarketAssignmentRepository assignmentRepository;

    public EmployeeResponse toResponse(
            User user
    ) {

        String empCode = user.getEmployeeCode();
        if (empCode == null || empCode.trim().isEmpty()) {
            if (user.getId() != null) {
                String idClean = user.getId().toString().replace("-", "").toUpperCase();
                empCode = "DAP-EMP-" + (idClean.length() >= 4 ? idClean.substring(0, 4) : idClean);
            } else {
                empCode = "DAP-EMP-001";
            }
        }

        UUID marketId = null;
        String marketName = null;
        if (assignmentRepository != null && user.getId() != null) {
            try {
                EmployeeMarketAssignment asg = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(user.getId()).orElse(null);
                if (asg != null && asg.getMarket() != null) {
                    marketId = asg.getMarket().getId();
                    marketName = asg.getMarket().getMarketName();
                }
            } catch (Exception ignored) {}
        }

        return EmployeeResponse.builder()
                .id(user.getId())
                .employeeCode(empCode)
                .fullName(user.getFullName())
                .mobileNumber(user.getMobileNumber())
                .email(user.getEmail() != null && !user.getEmail().trim().isEmpty() 
                        ? user.getEmail() 
                        : (user.getFullName() != null ? user.getFullName().toLowerCase().replaceAll("[^a-z0-9]", "") + "@dapfintech.com" : "employee@dapfintech.com"))
                .role(
                        user.getRole()
                                .getRoleName()
                )
                .status(
                        user.getStatus()
                )
                .marketId(marketId)
                .marketName(marketName)
                .build();
    }
}