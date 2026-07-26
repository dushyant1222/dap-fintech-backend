package com.dapfintech.customer.history.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.history.dto.response.CustomerHistoryResponse;
import com.dapfintech.customer.history.entity.CustomerHistory;
import com.dapfintech.customer.history.enums.CustomerHistoryAction;
import com.dapfintech.customer.history.mapper.CustomerHistoryMapper;
import com.dapfintech.customer.history.repository.CustomerHistoryRepository;
import com.dapfintech.customer.history.service.CustomerHistoryService;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.security.service.AccessControlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerHistoryServiceImpl
        implements CustomerHistoryService {

    private final CustomerHistoryRepository historyRepository;

    private final CustomerHistoryMapper historyMapper;

    private final CustomerRepository customerRepository;

    private final UserRepository userRepository;

    private final AccessControlService accessControlService;


    @Override
    @Transactional
    public void recordHistory(
            Customer customer,
            CustomerHistoryAction action,
            String title,
            String description,
            String oldValue,
            String newValue
    ) {

        User currentUser = getCurrentUser();

        CustomerHistory history =
                CustomerHistory.builder()
                        .customer(customer)
                        .action(action)
                        .title(title)
                        .description(description)
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .performedBy(currentUser)
                        .build();

        historyRepository.save(history);
    }


    @Override
    @Transactional(readOnly = true)
    public List<CustomerHistoryResponse>
    getCustomerHistory(
            UUID customerId
    ) {

        accessControlService.validateCustomerAccess(
                customerId
        );

        customerRepository.findById(customerId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Customer not found"
                        )
                );

        return historyRepository
                .findByCustomerIdOrderByCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(historyMapper::toResponse)
                .toList();
    }


    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return null;
        }

        String username =
                authentication.getName();

        return userRepository
                .findByMobileNumber(username)
                .orElse(null);
    }
}