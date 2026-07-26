package com.dapfintech.security.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.dapfintech.auth.entity.User;
import com.dapfintech.common.enums.UserStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomUserDetails
        implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of();
    }

    @Override
    public String getPassword() {

        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {

        return user.getMobileNumber();
    }

    @Override
    public boolean isAccountNonExpired() {

        return true;
    }

    @Override
    public boolean isAccountNonLocked() {

        return user.getStatus()
                != UserStatus.BLOCKED;
    }
    @Override
    public boolean isCredentialsNonExpired() {

        return true;
    }

    @Override
    public boolean isEnabled() {

        return user.getStatus()
                == UserStatus.ACTIVE;
    }
}