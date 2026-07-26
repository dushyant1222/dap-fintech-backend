package com.dapfintech.collection.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.collection.dto.request.CreateVisitRequest;
import com.dapfintech.collection.dto.response.VisitResponse;
import com.dapfintech.collection.entity.CustomerVisit;
import com.dapfintech.collection.enums.VisitStatus;
import com.dapfintech.collection.mapper.CustomerVisitMapper;
import com.dapfintech.collection.repository.CustomerVisitRepository;
import com.dapfintech.collection.service.CustomerVisitService;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.security.service.AccessControlService;
import com.dapfintech.sync.service.SyncLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerVisitServiceImpl implements CustomerVisitService{
	
	private final CustomerVisitRepository visitRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;
	private final CustomerVisitMapper mapper;
	private final AccessControlService accessControlService;
	private final SyncLogService syncLogService;
	
	
	
	@Override
	public VisitResponse createVisit(
	        CreateVisitRequest request
	) {

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

	    Customer customer =
	            customerRepository
	                    .findById(
	                            request.getCustomerId()
	                    )
	                    .orElseThrow(
	                            () -> new RuntimeException(
	                                    "Customer not found"
	                            )
	                    );

	    if(
	        request.getVisitStatus()
	        == VisitStatus.PROMISED_TO_PAY
	    ) {

	        if(
	            request.getPromiseAmount()
	            == null
	        ) {

	            throw new RuntimeException(
	                    "Promise amount required"
	            );
	        }

	        if(
	            request.getPromiseDate()
	            == null
	        ) {

	            throw new RuntimeException(
	                    "Promise date required"
	            );
	        }
	    }

	    CustomerVisit visit =
	            CustomerVisit.builder()
	                    .customer(customer)
	                    .employee(employee)
	                    .visitDate(
	                            java.time.LocalDateTime.now()
	                    )
	                    .visitStatus(
	                            request.getVisitStatus()
	                    )
	                    .remarks(
	                            request.getRemarks()
	                    )
	                    .promiseAmount(
	                            request.getPromiseAmount()
	                    )
	                    .promiseDate(
	                            request.getPromiseDate()
	                    )
	                    .latitude(
	                            request.getLatitude()
	                    )
	                    .longitude(
	                            request.getLongitude()
	                    )
	                    .build();

	    visit =
	            visitRepository.save(
	                    visit
	            );
	    syncLogService.logSync(
	            "VISIT",
	            visit.getId().toString()
	    );

	    return mapper.toResponse(
	            visit
	    );
	}
	
	@Override
	public List<VisitResponse>
	getCustomerVisits(
	        UUID customerId
	) {
		accessControlService
        .validateCustomerAccess(
                customerId
        );

	    return visitRepository
	            .findByCustomerId(
	                    customerId
	            )
	            .stream()
	            .map(
	                    mapper::toResponse
	            )
	            .toList();
	}
	
	@Override
	public List<VisitResponse>
	getEmployeeVisits(
	        UUID employeeId
	) {
		
		accessControlService
        .validateEmployeeAccess(
                employeeId
        );

	    return visitRepository
	            .findByEmployeeId(
	                    employeeId
	            )
	            .stream()
	            .map(
	                    mapper::toResponse
	            )
	            .toList();
	}
	
	
}
