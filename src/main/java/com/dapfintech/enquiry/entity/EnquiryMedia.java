package com.dapfintech.enquiry.entity;

import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.enquiry.enums.EnquiryMediaType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "enquiry_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnquiryMedia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private Enquiry enquiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 50)
    private EnquiryMediaType mediaType;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;
}