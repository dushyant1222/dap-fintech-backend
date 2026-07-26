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

        createAdminRole();

        createDefaultAdmin();
    }

    private void createAdminRole() {

        if (roleRepository
                .findByRoleName("ADMIN")
                .isPresent()) {

            return;
        }

        Role role = Role.builder()
                .roleName("ADMIN")
                .roleDescription("System Administrator")
                .build();

        roleRepository.save(role);

        System.out.println(
                "ADMIN role created"
        );
    }

    private void createDefaultAdmin() {

        if (userRepository
                .existsByMobileNumber(
                        "9999999999"
                )) {

            return;
        }

        Role adminRole =
                roleRepository
                        .findByRoleName("ADMIN")
                        .orElseThrow();

        User admin = User.builder()
                .fullName("System Admin")
                .mobileNumber("9999999999")
                .passwordHash(
                        passwordEncoder.encode(
                                "Admin@123"
                        )
                )
                .role(adminRole)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);

        System.out.println(
                "Default admin created"
        );
    }
}