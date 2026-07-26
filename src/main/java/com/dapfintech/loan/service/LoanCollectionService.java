package com.dapfintech.loan.service;

import java.util.List;
import com.dapfintech.loan.dto.response.EmployeeCollectionOverviewResponse;
import java.util.UUID;

import com.dapfintech.loan.dto.request.CreateCollectionRequest;
import com.dapfintech.loan.dto.response.CollectionDashboardResponse;
import com.dapfintech.loan.dto.response.CollectionHistoryResponse;
import com.dapfintech.loan.dto.response.CollectionResponse;
import com.dapfintech.loan.dto.response.OverdueCollectionResponse;
import com.dapfintech.loan.dto.response.PendingCollectionResponse;
import com.dapfintech.loan.dto.response.TodayScheduleResponse;

public interface LoanCollectionService {

	CollectionResponse collectPayment(
	        CreateCollectionRequest request
	);
    List<CollectionResponse> getLoanCollections(UUID loanId);
    CollectionResponse getCollectionById(UUID collectionId);
    CollectionDashboardResponse getDashboard();
    List<TodayScheduleResponse> getTodaySchedule();
    List<PendingCollectionResponse> getPendingCollections();
    List<OverdueCollectionResponse>
    getOverdueCollections();
    List<CollectionHistoryResponse>
    getCollectionHistory();
    List<CollectionHistoryResponse>
    getCollectionHistoryByEmployee(
            UUID employeeId
    );
    EmployeeCollectionOverviewResponse getEmployeeCollectionOverview(
            UUID employeeId
    );
}