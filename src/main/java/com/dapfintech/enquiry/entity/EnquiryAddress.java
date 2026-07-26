package com.dapfintech.enquiry.entity;

import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.enquiry.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "enquiry_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnquiryAddress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType;

    @Column(name = "address_line", nullable = false, columnDefinition = "TEXT")
    private String addressLine;
}