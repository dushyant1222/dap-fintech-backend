package com.dapfintech.auth.entity;

import java.util.UUID;

import com.dapfintech.common.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "permission_key", nullable = false, unique = true)
    private String permissionKey;

    @Column(name = "module_name",nullable = false)
    private String moduleName;

    @Column(name = "description",nullable = false)
    private String description;
}