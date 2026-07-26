package com.dapfintech.customer.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import com.dapfintech.customer.dto.request.CreateCustomerRequest;
import com.dapfintech.customer.dto.request.CustomerFilterRequest;
import com.dapfintech.customer.dto.request.UpdateCustomerRequest;
import com.dapfintech.customer.dto.request.UpdateCustomerStatusRequest;
import com.dapfintech.customer.dto.response.CustomerDetailsResponse;
import com.dapfintech.customer.dto.response.CustomerResponse;
import com.dapfintech.customer.dto.response.DeleteCustomerResponse;

public interface CustomerService {
	
	void deleteCustomer(UUID customerId);

    CustomerResponse createCustomer(
            CreateCustomerRequest request
    );

    CustomerResponse updateCustomer(
            UUID customerId,
            UpdateCustomerRequest request
    );
    CustomerResponse updateCustomerStatus(
            UUID customerId,
            UpdateCustomerStatusRequest request
    );
    DeleteCustomerResponse deleteCustomer(
            UUID customerId,
            UUID deletedBy
    );
    CustomerResponse getCustomerById(
            UUID customerId
    );
    Page<CustomerResponse> filterCustomers(
            CustomerFilterRequest filter,
            int page,
            int size
    );

    Page<CustomerResponse> getAllCustomers(
            int page,
            int size
    );

    Page<CustomerResponse> searchCustomers(
            String keyword,
            int page,
            int size
    );
    CustomerDetailsResponse getCustomerDetails(
            UUID customerId
    );
}