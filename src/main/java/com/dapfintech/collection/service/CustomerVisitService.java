package com.dapfintech.collection.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.collection.dto.request.CreateVisitRequest;
import com.dapfintech.collection.dto.response.VisitResponse;

public interface CustomerVisitService {
	
	VisitResponse createVisit(CreateVisitRequest request);
	List<VisitResponse> getCustomerVisits(UUID customerId);
	List<VisitResponse> getEmployeeVisits(UUID employeeId);
}
