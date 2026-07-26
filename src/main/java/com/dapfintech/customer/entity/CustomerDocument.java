package com.dapfintech.customer.entity;

import java.util.UUID;

import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.customer.enums.DocumentType;
import com.dapfintech.customer.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "customer_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private Customer customer;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "document_type",
            nullable = false
    )
    private DocumentType documentType;


    @Column(
            name = "file_name",
            nullable = false
    )
    private String fileName;


    @Column(
            name = "file_path",
            nullable = false
    )
    private String filePath;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "verification_status",
            nullable = false
    )
    @Builder.Default
    private VerificationStatus verificationStatus =
            VerificationStatus.PENDING;


    @Column(name = "verification_remark")
    private String verificationRemark;


    @Column(name = "verified_by")
    private UUID verifiedBy;
}