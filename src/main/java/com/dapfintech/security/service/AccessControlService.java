package com.dapfintech.security.service;

import java.util.UUID;

public interface AccessControlService {

	void validateCustomerAccess(UUID customerId);
	void validateLoanAccess(UUID loanId);
	void validateCollectionAccess(UUID collectionId);
	void validateEmployeeAccess(
	        UUID employeeId
	);
}