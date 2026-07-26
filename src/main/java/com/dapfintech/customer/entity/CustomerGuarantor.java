package com.dapfintech.customer.entity;

import java.util.UUID;

import com.dapfintech.common.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_guarantors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerGuarantor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "guarantor_name")
    private String guarantorName;

    @Column(name = "mobile_number")
    private String mobileNumber;

    private String relationship;

    private String address;
}