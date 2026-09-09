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

        var existingUserOpt = userRepository.findByMobileNumber("9999999999");
        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            existing.setFullName("Master Admin");
            existing.setRole(masterAdminRole);
            existing.setStatus(UserStatus.ACTIVE);
            userRepository.save(existing);
            System.out.println("Default Master Admin updated to MASTER_ADMIN role");
            return;
        }

        User admin = User.builder()
                .fullName("Master Admin")
                .mobileNumber("9999999999")
                .passwordHash(
                        passwordEncoder.encode(
                                "Admin@123"
                        )
                )
                .role(masterAdminRole)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        System.out.println("Default Master Admin created");
    }
}