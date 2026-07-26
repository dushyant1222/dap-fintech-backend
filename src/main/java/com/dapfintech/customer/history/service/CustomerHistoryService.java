package com.dapfintech.customer.history.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.history.dto.response.CustomerHistoryResponse;
import com.dapfintech.customer.history.enums.CustomerHistoryAction;

public interface CustomerHistoryService {

    void recordHistory(
            Customer customer,
            CustomerHistoryAction action,
            String title,
            String description,
            String oldValue,
            String newValue
    );

    List<CustomerHistoryResponse>
    getCustomerHistory(UUID customerId);
}