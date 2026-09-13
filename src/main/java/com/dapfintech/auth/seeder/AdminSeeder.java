package com.dapfintech.auth.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.dapfintech.auth.entity.Role;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.RoleRepository;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.common.enums.UserStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createRoles();
        createDefaultMasterAdmin();
    }

    private void createRoles() {
        if (roleRepository.findByRoleName("MASTER_ADMIN").isEmpty()) {
            Role masterRole = Role.builder()
                    .roleName("MASTER_ADMIN")
                    .roleDescription("Master Administrator")
                    .build();
            roleRepository.save(masterRole);
            System.out.println("MASTER_ADMIN role created");
        }

        if (roleRepository.findByRoleName("ADMIN").isEmpty()) {
            Role adminRole = Role.builder()
                    .roleName("ADMIN")
                    .roleDescription("System Administrator")
                    .build();
            roleRepository.save(adminRole);
            System.out.println("ADMIN role created");
        }
    }

    private void createDefaultMasterAdmin() {
        Role masterAdminRole = roleRepository.findByRoleName("MASTER_ADMIN")
                .orElseGet(() -> roleRepository.findByRoleName("ADMIN").orElseThrow());

        // Check if 9311111335 already exists
        var user9311 = userRepository.findByMobileNumber("9311111335");
        if (user9311.isPresent()) {
            User existing = user9311.get();
            existing.setFullName("Master Admin");
            existing.setRole(masterAdminRole);
            existing.setStatus(UserStatus.ACTIVE);
            userRepository.save(existing);
            System.out.println("Default Master Admin (9311111335) verified as MASTER_ADMIN");
            return;
        }

        // Migrate 9999999999 to 9311111335 if found
        var user9999 = userRepository.findByMobileNumber("9999999999");
        if (user9999.isPresent()) {
            User existing = user9999.get();
            existing.setFullName("Master Admin");
            existing.setMobileNumber("9311111335");
            existing.setPasswordHash(passwordEncoder.encode("Admin@123"));
            existing.setRole(masterAdminRole);
            existing.setStatus(UserStatus.ACTIVE);
            userRepository.save(existing);
            System.out.println("Master Admin migrated from 9999999999 to 9311111335 successfully");
            return;
        }

        // Create new default master admin with 9311111335
        User admin = User.builder()
                .fullName("Master Admin")
                .mobileNumber("9311111335")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .role(masterAdminRole)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        System.out.println("Default Master Admin (9311111335) created");
    }
}