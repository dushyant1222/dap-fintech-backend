package com.dapfintech.security.utils;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Extracts the User entity of the currently authenticated user from the Security Context.
     * @return User entity of the current user
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Unauthorized: No authenticated user found in context");
        }

        String username;
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getPrincipal().toString();
        }

        return userRepository.findByMobileNumber(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));
    }

    /**
     * Extracts the UUID of the currently authenticated user from the Security Context.
     * @return UUID of the current user
     */
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}