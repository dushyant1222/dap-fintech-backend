package com.dapfintech.dashboard.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeDashboardResponse {
	
	private Long activeLoans;

	private Long pendingEmi;

	private Long overdueEmi;

	private Long pendingLoanApprovals;

	private BigDecimal todayCollection;
	private Long completedCollections;
	private Long pendingCollections;
	private Long overdueCustomers;
	private BigDecimal monthCollection;
	
	private Long todayVisits;
	
	private Long monthVisits;
	
	private Long promiseToPayCount;
	
	private Long assignedMarkets;
	
	private Long assignedCustomers;
}
