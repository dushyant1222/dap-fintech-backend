package com.dapfintech.auth.service.impl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.dapfintech.auth.dto.request.EmployeeFilterRequest;
import com.dapfintech.auth.dto.response.EmployeePageResponse;
import com.dapfintech.auth.specification.EmployeeSpecification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.dapfintech.auth.dto.response.MyProfileResponse;
import com.dapfintech.auth.entity.User;
import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.auth.dto.request.CreateEmployeeRequest;
import com.dapfintech.auth.dto.request.UpdateEmployeeRequest;
import com.dapfintech.auth.dto.response.EmployeeResponse;
import com.dapfintech.auth.entity.Role;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.mapper.EmployeeMapper;
import com.dapfintech.auth.repository.RoleRepository;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.auth.service.EmployeeService;
import com.dapfintech.common.enums.UserStatus;
import com.dapfintech.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
	
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmployeeMapper employeeMapper;
	private final AuditLogService auditLogService;
	private final NotificationService notificationService;
	
	@Override
	public EmployeePageResponse filterEmployees(
	        EmployeeFilterRequest request,
	        int page,
	        int size
	) {

	    if (page < 0) {
	        throw new RuntimeException(
	                "Page number cannot be negative"
	        );
	    }

	    if (size < 1 || size > 100) {
	        throw new RuntimeException(
	                "Page size must be between 1 and 100"
	        );
	    }

	    Pageable pageable =
	            PageRequest.of(
	                    page,
	                    size,
	                    Sort.by(
	                            Sort.Direction.DESC,
	                            "createdAt"
	                    )
	            );

	    Specification<User> specification =
	            EmployeeSpecification.isEmployee()
	                    .and(
	                            EmployeeSpecification.hasKeyword(
	                                    request.getKeyword()
	                            )
	                    )
	                    .and(
	                            EmployeeSpecification.hasStatus(
	                                    request.getStatus()
	                            )
	                    )
	                    .and(
	                            EmployeeSpecification.assignedToMarket(
	                                    request.getMarketId()
	                            )
	                    );

	    Page<User> employeePage =
	            userRepository.findAll(
	                    specification,
	                    pageable
	            );

	    return EmployeePageResponse.builder()
	            .content(
	                    employeePage
	                            .getContent()
	                            .stream()
	                            .map(employeeMapper::toResponse)
	                            .toList()
	            )
	            .pageNumber(
	                    employeePage.getNumber()
	            )
	            .pageSize(
	                    employeePage.getSize()
	            )
	            .totalElements(
	                    employeePage.getTotalElements()
	            )
	            .totalPages(
	                    employeePage.getTotalPages()
	            )
	            .first(
	                    employeePage.isFirst()
	            )
	            .last(
	                    employeePage.isLast()
	            )
	            .build();
	}
	
	@Override
	public MyProfileResponse getMyProfile() {

	    Authentication authentication =
	            SecurityContextHolder
	                    .getContext()
	                    .getAuthentication();

	    String mobileNumber =
	            authentication.getName();

	    User user =
	            userRepository
	                    .findByMobileNumber(
	                            mobileNumber
	                    )
	                    .orElseThrow(
	                            () -> new RuntimeException(
	                                    "User not found"
	                            )
	                    );

	    return MyProfileResponse.builder()

	            .id(
	                    user.getId()
	            )

	            .fullName(
	                    user.getFullName()
	            )

	            .mobileNumber(
	                    user.getMobileNumber()
	            )

	            .email(
	                    user.getEmail() != null && !user.getEmail().trim().isEmpty()
	                            ? user.getEmail()
	                            : (user.getFullName() != null ? user.getFullName().toLowerCase().replaceAll("[^a-z0-9]", "") + "@dapfintech.com" : "employee@dapfintech.com")
	            )

	            .role(
	                    user.getRole()
	                            .getRoleName()
	            )

	            .status(
	                    user.getStatus()
	                            .name()
	            )

	            .build();

	}
	
	@Override
	public EmployeeResponse createEmployee(
	        CreateEmployeeRequest request
	) {

	    // ==========================================
	    // VALIDATE FULL NAME
	    // ==========================================

	    if (request.getFullName() == null ||
	            request.getFullName().trim().isEmpty()) {

	        throw new RuntimeException(
	                "Employee name is required"
	        );
	    }

	    // ==========================================
	    // VALIDATE MOBILE NUMBER
	    // ==========================================

	    if (request.getMobileNumber() == null ||
	            !request.getMobileNumber().matches("\\d{10}")) {

	        throw new RuntimeException(
	                "Mobile number must contain exactly 10 digits"
	        );
	    }

	    // ==========================================
	    // VALIDATE PASSWORD
	    // ==========================================

	    if (request.getPassword() == null ||
	            request.getPassword().length() < 6) {

	        throw new RuntimeException(
	                "Password must contain at least 6 characters"
	        );
	    }

	    // ==========================================
	    // CHECK DUPLICATE MOBILE NUMBER
	    // ==========================================

	    if (userRepository.existsByMobileNumber(
	            request.getMobileNumber()
	    )) {

	        throw new RuntimeException(
	                "Mobile number already exists"
	        );
	    }

	    // ==========================================
	    // GET EMPLOYEE ROLE
	    // ==========================================

	    Role employeeRole =
	            roleRepository
	                    .findByRoleName("EMPLOYEE")
	                    .orElseThrow(
	                            () -> new RuntimeException(
	                                    "EMPLOYEE role not found"
	                            )
	                    );

	    // ==========================================
	    // CREATE EMPLOYEE
	    // ==========================================

	    User employee =
	            User.builder()
	                    .employeeCode(generateEmployeeCode())
	                    .fullName(
	                            request.getFullName().trim()
	                    )
	                    .mobileNumber(
	                            request.getMobileNumber().trim()
	                    )
	                    .email(
	                            request.getEmail() != null && !request.getEmail().trim().isEmpty()
	                                    ? request.getEmail().trim()
	                                    : request.getFullName().toLowerCase().replaceAll("[^a-z0-9]", "") + "@dapfintech.com"
	                    )
	                    .passwordHash(
	                            passwordEncoder.encode(
	                                    request.getPassword()
	                            )
	                    )
	                    .role(employeeRole)
	                    .status(UserStatus.ACTIVE)
	                    .build();

	    employee =
	            userRepository.save(employee);

	    // ==========================================
	    // AUDIT LOG
	    // ==========================================

	    auditLogService.log(
	            "SYSTEM",
	            "CREATE_EMPLOYEE",
	            "EMPLOYEE",
	            employee.getId().toString()
	    );

	    // ==========================================
	    // NOTIFICATION
	    // ==========================================

	    notificationService.createNotification(
	            "Employee Created",
	            "New employee " +
	                    employee.getFullName() +
	                    " has been created."
	    );

	    return employeeMapper.toResponse(employee);
	}
	
	@Override
	public EmployeeResponse updateEmployee(
	        UUID employeeId,
	        UpdateEmployeeRequest request
	) {

	    User employee = userRepository
	            .findById(employeeId)
	            .orElseThrow(
	                    () -> new RuntimeException(
	                            "Employee not found"
	                    )
	            );

	    /*
	     * Only employees can be updated through
	     * the employee management module.
	     */
	    if (employee.getRole() == null ||
	            !"EMPLOYEE".equalsIgnoreCase(
	                    employee.getRole().getRoleName()
	            )) {

	        throw new RuntimeException(
	                "Selected user is not an employee"
	        );
	    }

	    /*
	     * Validate required fields.
	     */
	    if (request.getFullName() == null ||
	            request.getFullName().trim().isEmpty()) {

	        throw new RuntimeException(
	                "Employee name is required"
	        );
	    }

	    if (request.getMobileNumber() == null ||
	            request.getMobileNumber().trim().isEmpty()) {

	        throw new RuntimeException(
	                "Mobile number is required"
	        );
	    }

	    if (!request.getMobileNumber()
	            .trim()
	            .matches("\\d{10}")) {

	        throw new RuntimeException(
	                "Mobile number must contain exactly 10 digits"
	        );
	    }

	    if (request.getStatus() == null) {

	        throw new RuntimeException(
	                "Employee status is required"
	        );
	    }

	    /*
	     * Check duplicate mobile number.
	     *
	     * If another user already owns this mobile number,
	     * prevent the update.
	     */
	    userRepository
	            .findByMobileNumber(
	                    request.getMobileNumber().trim()
	            )
	            .ifPresent(existingUser -> {

	                if (!existingUser.getId()
	                        .equals(employeeId)) {

	                    throw new RuntimeException(
	                            "Mobile number already exists"
	                    );
	                }
	            });

	    employee.setFullName(
	            request.getFullName().trim()
	    );

	    employee.setMobileNumber(
	            request.getMobileNumber().trim()
	    );

	    if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
	        employee.setEmail(request.getEmail().trim());
	    } else if (employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
	        employee.setEmail(employee.getFullName().toLowerCase().replaceAll("[^a-z0-9]", "") + "@dapfintech.com");
	    }

	    employee.setStatus(
	            request.getStatus()
	    );

	    employee = userRepository.save(employee);

	    auditLogService.log(
	            "SYSTEM",
	            "UPDATE_EMPLOYEE",
	            "EMPLOYEE",
	            employee.getId().toString()
	    );

	    return employeeMapper.toResponse(
	            employee
	    );
	}
	@Override
	public EmployeeResponse getEmployeeById(
	        UUID employeeId
	) {

	    User employee =
	            userRepository.findById(employeeId)
	                    .orElseThrow(
	                            () -> new RuntimeException(
	                                    "Employee not found"
	                            )
	                    );

	    if (employee.getRole() == null ||
	            !"EMPLOYEE".equals(
	                    employee.getRole().getRoleName()
	            )) {

	        throw new RuntimeException(
	                "Employee not found"
	        );
	    }

	    return employeeMapper.toResponse(
	            employee
	    );
	}
	@Override
	public List<EmployeeResponse>
	getAllEmployees() {

	    return userRepository
	            .findByRoleRoleName(
	                    "EMPLOYEE"
	            )
	            .stream()
	            .map(
	                    employeeMapper::toResponse
	            )
	            .toList();
	}

	@Override
	public List<EmployeeResponse> getTransferReceivers() {
	    List<User> employees = userRepository.findByRoleRoleName("EMPLOYEE");
	    List<User> admins = userRepository.findByRoleRoleName("ADMIN");
	    List<User> all = new java.util.ArrayList<>();
	    all.addAll(employees);
	    all.addAll(admins);
	    return all.stream()
	            .map(employeeMapper::toResponse)
	            .toList();
	}
	@Override
	public void activateEmployee(
	        UUID employeeId
	) {
		

		User employee =
		        userRepository
		                .findByIdAndRoleRoleName(
		                        employeeId,
		                        "EMPLOYEE"
		                )
		                .orElseThrow(
		                        () -> new RuntimeException(
		                                "Employee not found"
		                        )
		                );

	    employee.setStatus(
	            UserStatus.ACTIVE
	    );

	    employee =
	            userRepository.save(employee);

	    auditLogService.log(
	            "SYSTEM",
	            "ACTIVATE_EMPLOYEE",
	            "EMPLOYEE",
	            employee.getId().toString()
	    );
	}
	@Override
	public void deactivateEmployee(
	        UUID employeeId
	) {

	    User employee =
	            userRepository.findById(employeeId)
	                    .orElseThrow(
	                            () -> new RuntimeException(
	                                    "Employee not found"
	                            )
	                    );

	    employee.setStatus(
	            UserStatus.INACTIVE
	    );

	    employee =
	            userRepository.save(employee);

	    auditLogService.log(
	            "SYSTEM",
	            "DEACTIVATE_EMPLOYEE",
	            "EMPLOYEE",
	            employee.getId().toString()
	    );
	}
	
	@Override
	public void deleteEmployee(UUID employeeId) {
	    User employee = userRepository.findById(employeeId)
	            .orElseThrow(() -> new RuntimeException("Employee not found"));
	    
	    userRepository.delete(employee);
	    
	    auditLogService.log(
	            "SYSTEM",
	            "DELETE_EMPLOYEE",
	            "EMPLOYEE",
	            employee.getId().toString()
	    );
	}

	private String generateEmployeeCode() {
	    long count = userRepository.countByRoleRoleName("EMPLOYEE");
	    return String.format("DAP-EMP-%03d", count + 1);
	}
}
