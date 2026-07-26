package com.dapfintech.security.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(
                        authentication.getPrincipal()
                )) {

            throw new RuntimeException(
                    "Authenticated user not found"
            );
        }

        String mobileNumber =
                authentication.getName();

        return userRepository
                .findByMobileNumber(
                        mobileNumber
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Authenticated user not found"
                        )
                );
    }
}