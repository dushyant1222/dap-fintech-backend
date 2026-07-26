package com.dapfintech.customer.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dapfintech.customer.entity.CustomerGuarantor;

@Repository
public interface CustomerGuarantorRepository
        extends JpaRepository<CustomerGuarantor, UUID> {

    List<CustomerGuarantor> findByCustomerId(
            UUID customerId
    );

    Optional<CustomerGuarantor> findByIdAndCustomerId(
            UUID guarantorId,
            UUID customerId
    );
}