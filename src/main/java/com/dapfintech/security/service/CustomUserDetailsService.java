package com.dapfintech.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String mobileNumber) throws UsernameNotFoundException{
    	User user = userRepository.findByMobileNumber(mobileNumber).orElseThrow(() -> new UsernameNotFoundException("user name not found"));

    	return new CustomUserDetails(user);
    }
}