package com.dapfintech.auth.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.auth.dto.response.EmployeeResponse;
import com.dapfintech.auth.entity.User;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(
            User user
    ) {

        return EmployeeResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .mobileNumber(user.getMobileNumber())
                .email(user.getEmail() != null && !user.getEmail().trim().isEmpty() 
                        ? user.getEmail() 
                        : (user.getFullName() != null ? user.getFullName().toLowerCase().replaceAll("[^a-z0-9]", "") + "@dapfintech.com" : "employee@dapfintech.com"))
                .role(
                        user.getRole()
                                .getRoleName()
                )
                .status(
                        user.getStatus()
                )
                .build();
    }
}