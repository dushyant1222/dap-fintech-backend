package com.dapfintech.audit.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.audit.dto.response.EmployeeActivityTimelineResponse;
import com.dapfintech.audit.service.EmployeeActivityTimelineService;
import com.dapfintech.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(
        "/api/v1/employees/{employeeId}/activity-timeline"
)
@RequiredArgsConstructor
public class EmployeeActivityTimelineController {

    private final EmployeeActivityTimelineService
            employeeActivityTimelineService;

    @GetMapping
    public ResponseEntity<
            ApiResponse<EmployeeActivityTimelineResponse>>
    getEmployeeActivityTimeline(
            @PathVariable UUID employeeId
    ) {

        EmployeeActivityTimelineResponse response =
                employeeActivityTimelineService
                        .getEmployeeActivityTimeline(
                                employeeId
                        );

        return ResponseEntity.ok(

                ApiResponse
                        .<EmployeeActivityTimelineResponse>
                                builder()

                        .success(true)

                        .message(
                                "Employee activity timeline fetched successfully"
                        )

                        .data(response)

                        .build()
        );
    }
}