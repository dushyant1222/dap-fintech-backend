package com.dapfintech.audit.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeActivityTimelineResponse {

    private UUID employeeId;

    private String employeeName;

    private int totalActivities;

    private List<EmployeeActivityItemResponse> activities;
}