package com.dapfintech.dashboard.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.collection.repository.CustomerVisitRepository;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.dashboard.dto.response.EmployeeDashboardResponse;
import com.dapfintech.dashboard.service.DashboardService;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.enums.RepaymentStatus;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService{
	
	private final UserRepository userRepository;

	private final LoanCollectionRepository collectionRepository;

	private final CustomerVisitRepository visitRepository;

	private final EmployeeMarketAssignmentRepository assignmentRepository;

	private final CustomerRepository customerRepository;
	private final LoanRepository loanRepository;
	private final LoanRepaymentScheduleRepository repaymentScheduleRepository;
	
	@Override
	public EmployeeDashboardResponse
	getMyDashboard() {

	    Authentication authentication =
	            SecurityContextHolder
	                    .getContext()
	                    .getAuthentication();

	    String mobileNumber =
	            authentication.getName();

	    User employee =
	            userRepository
	                    .findByMobileNumber(
	                            mobileNumber
	                    )
	                    .orElseThrow(
	                            () -> new RuntimeException(
	                                    "Employee not found"
	                            )
	                    );

	    return EmployeeDashboardResponse
	            .builder()
	            .todayCollection(
	                    collectionRepository
	                            .getTodayCollectionByEmployee(
	                                    employee.getId()
	                            )
	            )
	            .monthCollection(
	                    collectionRepository
	                            .getMonthCollectionByEmployee(
	                                    employee.getId()
	                            )
	            )
	            .activeLoans(
	            	    loanRepository.countByLoanStatus(
	            	        LoanStatus.ACTIVE
	            	    )
	            	)

	            	.pendingLoanApprovals(
	            	    loanRepository.countByLoanStatus(
	            	        LoanStatus.PENDING_APPROVAL
	            	    )
	            	)

	            	.activeLoans(
	            		    loanRepository.countActiveLoansByEmployee(
	            		        employee.getId()
	            		    )
	            		)

	            		.completedCollections(
	            		    collectionRepository.countTodayCollectionsByEmployee(
	            		        employee.getId()
	            		    )
	            		)

	            		.pendingCollections(
	            		    repaymentScheduleRepository.countTodayPendingCollections(
	            		        employee.getId()
	            		    )
	            		)

	            		.overdueCustomers(
	            		    repaymentScheduleRepository.countOverdueCustomers(
	            		        employee.getId()
	            		    )
	            		)
	            		
	            .todayVisits(
	                    visitRepository
	                            .countTodayVisits(
	                                    employee.getId()
	                            )
	            )
	            .monthVisits(
	                    visitRepository
	                            .countMonthVisits(
	                                    employee.getId()
	                            )
	            )
	            .promiseToPayCount(
	                    visitRepository
	                            .countPromiseToPay(
	                                    employee.getId()
	                            )
	            )
	            .assignedMarkets(
	                    assignmentRepository
	                            .countByEmployeeIdAndIsActiveTrue(
	                                    employee.getId()
	                            )
	            )
	            .assignedCustomers(
	                    customerRepository
	                            .countAssignedCustomers(
	                                    employee.getId()
	                            )
	            )
	            .build();
	}
	
}
