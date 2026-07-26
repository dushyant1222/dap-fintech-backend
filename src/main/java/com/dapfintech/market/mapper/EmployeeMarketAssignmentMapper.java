package com.dapfintech.market.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.market.dto.response.AssignmentResponse;
import com.dapfintech.market.entity.EmployeeMarketAssignment;

@Component
public class EmployeeMarketAssignmentMapper {

    public AssignmentResponse toResponse(
            EmployeeMarketAssignment assignment
    ) {

        return AssignmentResponse
                .builder()
                .id(
                        assignment.getId()
                )
                .marketId(
                        assignment.getMarket().getId()
                )
                .marketName(
                        assignment.getMarket()
                                .getMarketName()
                )
                .employeeId(
                        assignment.getEmployee()
                                .getId()
                )
                .employeeName(
                        assignment.getEmployee()
                                .getFullName()
                )
                .assignedDate(
                        assignment.getAssignedDate()
                )
                .build();
    }
}