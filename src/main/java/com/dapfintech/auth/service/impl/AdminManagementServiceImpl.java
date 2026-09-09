package com.dapfintech.auth.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.auth.dto.request.CreateAdminRequest;
import com.dapfintech.auth.dto.response.AdminResponse;
import com.dapfintech.auth.entity.Role;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.RoleRepository;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.auth.service.AdminManagementService;
import com.dapfintech.common.enums.UserStatus;
import com.dapfintech.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminManagementServiceImpl implements AdminManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Unauthorized: No active session");
        }
        return userRepository.findByMobileNumber(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isMasterAdmin(User user) {
        if (user == null) return false;
        return "9999999999".equals(user.getMobileNumber()) ||
               (user.getRole() != null && "MASTER_ADMIN".equalsIgnoreCase(user.getRole().getRoleName()));
    }

    @Override
    @Transactional
    public AdminResponse createAdmin(CreateAdminRequest request) {
        User currentUser = getCurrentUser();
        if (!isMasterAdmin(currentUser)) {
            throw new RuntimeException("Permission denied: Only the Master Admin can create new Admins");
        }

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new RuntimeException("Full name is required");
        }
        if (request.getMobileNumber() == null || request.getMobileNumber().trim().length() != 10) {
            throw new RuntimeException("Valid 10-digit mobile number is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        String mobile = request.getMobileNumber().trim();
        if (userRepository.existsByMobileNumber(mobile)) {
            throw new RuntimeException("A user with this mobile number already exists");
        }

        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> {
                    Role r = Role.builder().roleName("ADMIN").roleDescription("System Administrator").build();
                    return roleRepository.save(r);
                });

        String email = request.getEmail() != null && !request.getEmail().trim().isEmpty()
                ? request.getEmail().trim()
                : request.getFullName().trim().toLowerCase().replaceAll("[^a-z0-9]", "") + "@dapfintech.com";

        User newAdmin = User.builder()
                .fullName(request.getFullName().trim())
                .mobileNumber(mobile)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(adminRole)
                .status(UserStatus.ACTIVE)
                .build();

        newAdmin = userRepository.save(newAdmin);

        auditLogService.log(
                "ADMIN",
                "CREATE_ADMIN",
                "ADMIN",
                newAdmin.getId().toString()
        );

        notificationService.createNotification(
                "Admin Created",
                "New Admin " + newAdmin.getFullName() + " created by Master Admin"
        );

        return toResponse(newAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminResponse> getAllAdmins() {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(u -> u.getRole() != null && 
                             ("ADMIN".equalsIgnoreCase(u.getRole().getRoleName()) || 
                              "MASTER_ADMIN".equalsIgnoreCase(u.getRole().getRoleName()) ||
                              "9999999999".equals(u.getMobileNumber())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void toggleAdminStatus(UUID adminId) {
        User currentUser = getCurrentUser();
        if (!isMasterAdmin(currentUser)) {
            throw new RuntimeException("Permission denied: Only the Master Admin can manage Admin statuses");
        }

        User target = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (isMasterAdmin(target)) {
            throw new RuntimeException("The Master Admin account cannot be deactivated");
        }

        target.setStatus(target.getStatus() == UserStatus.ACTIVE ? UserStatus.INACTIVE : UserStatus.ACTIVE);
        userRepository.save(target);

        auditLogService.log(
                "ADMIN",
                "TOGGLE_ADMIN_STATUS",
                "ADMIN",
                target.getId().toString()
        );
    }

    private AdminResponse toResponse(User u) {
        boolean isMaster = isMasterAdmin(u);
        return AdminResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .mobileNumber(u.getMobileNumber())
                .email(u.getEmail())
                .role(isMaster ? "MASTER_ADMIN" : "ADMIN")
                .status(u.getStatus() != null ? u.getStatus().name() : "ACTIVE")
                .isMasterAdmin(isMaster)
                .build();
    }
}
