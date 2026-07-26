package com.dapfintech.loan.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeCollectionOverviewResponse {

    private UUID employeeId;

    private String employeeName;

    private String marketName;

    private BigDecimal todayCollection;

    private Long todaySuccessfulPayments;

    private Long todaySchedule;

    private Long pendingCollections;

    private Long overdueCustomers;

    private List<CollectionHistoryResponse> collectionHistory;

    private List<TodayScheduleResponse> todayScheduleList;

}