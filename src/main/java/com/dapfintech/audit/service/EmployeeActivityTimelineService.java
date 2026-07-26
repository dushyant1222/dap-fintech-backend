package com.dapfintech.audit.service;

import java.util.UUID;

import com.dapfintech.audit.dto.response.EmployeeActivityTimelineResponse;

public interface EmployeeActivityTimelineService {

    EmployeeActivityTimelineResponse getEmployeeActivityTimeline(UUID employeeId);
}