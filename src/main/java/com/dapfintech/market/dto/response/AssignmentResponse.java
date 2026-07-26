package com.dapfintech.market.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentResponse {

    private UUID id;

    private UUID marketId;

    private String marketName;

    private UUID employeeId;

    private String employeeName;

    private LocalDateTime assignedDate;
}