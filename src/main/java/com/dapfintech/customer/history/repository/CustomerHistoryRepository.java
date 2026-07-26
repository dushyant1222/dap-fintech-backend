package com.dapfintech.customer.history.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.customer.history.entity.CustomerHistory;

@Repository
public interface CustomerHistoryRepository
        extends JpaRepository<CustomerHistory, UUID> {

    List<CustomerHistory>
    findByCustomerIdOrderByCreatedAtDesc(
            UUID customerId
    );
}