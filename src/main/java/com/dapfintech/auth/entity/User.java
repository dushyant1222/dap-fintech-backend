package com.dapfintech.auth.entity;

import java.util.UUID;

import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.common.enums.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User  extends BaseEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name="full_name", nullable = false)
	private String fullName;

	@Column(name="mobile_number", nullable = false, unique = true)
	private String mobileNumber;

	@Column(name="email")
	private String email;

	@Column(name="employee_code", unique = true)
	private String employeeCode;

	@Column(name="password_hash", nullable = false)
	private String passwordHash;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "role_id")
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private UserStatus status;




}
