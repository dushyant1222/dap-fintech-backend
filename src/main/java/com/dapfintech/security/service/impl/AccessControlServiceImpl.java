package com.dapfintech.security.service.impl;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
import com.dapfintech.security.service.AccessControlService;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanCollection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessControlServiceImpl
        implements AccessControlService {

    private final CustomerRepository customerRepository;

    private final UserRepository userRepository;

    private final EmployeeMarketAssignmentRepository assignmentRepository;
    private final LoanRepository loanRepository;
    private final LoanCollectionRepository collectionRepository;
    
    @Override
    public void validateEmployeeAccess(
            UUID employeeId
    ) {

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

        if(user.getRole()
                .getRoleName()
                .equalsIgnoreCase(
                        "ADMIN"
                )) {

            return;
        }

        if(!user.getId().equals(
                employeeId
        )) {

            throw new RuntimeException(
                    "Access denied"
            );
        }
    }
    
    
    @Override
    public void validateCollectionAccess(
            UUID collectionId
    ) {

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

        if(user.getRole()
                .getRoleName()
                .equalsIgnoreCase(
                        "ADMIN"
                )) {

            return;
        }

        LoanCollection collection =
                collectionRepository
                        .findById(
                                collectionId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Collection not found"
                                )
                        );

        if (collection.getLoan().getCustomer().getMarket() == null) {
            throw new RuntimeException("Access denied. Record is not assigned to a field market.");
        }

        boolean assigned =
                assignmentRepository
                        .existsByEmployeeIdAndMarketIdAndIsActiveTrue(
                                user.getId(),
                                collection.getLoan()
                                          .getCustomer()
                                          .getMarket()
                                          .getId()
                        );

        if(!assigned) {

            throw new RuntimeException(
                    "Access denied"
            );
        }
    }

    
    @Override
    public void validateLoanAccess(
            UUID loanId
    ) {

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

        if(user.getRole()
                .getRoleName()
                .equalsIgnoreCase(
                        "ADMIN"
                )) {

            return;
        }

        Loan loan =
                loanRepository
                        .findById(
                                loanId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Loan not found"
                                )
                        );

        if (loan.getCustomer().getMarket() == null) {
            throw new RuntimeException("Access denied. Record is not assigned to a field market.");
        }

        boolean assigned =
                assignmentRepository
                        .existsByEmployeeIdAndMarketIdAndIsActiveTrue(
                                user.getId(),
                                loan.getCustomer()
                                        .getMarket()
                                        .getId()
                        );

        if(!assigned) {

            throw new RuntimeException(
                    "Access denied"
            );
        }
    }
    
    @Override
    public void validateCustomerAccess(
            UUID customerId
    ) {

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

        // Admin can access everything
        if(user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            return;
        }

        Customer customer =
                customerRepository
                        .findById(
                                customerId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Customer not found"
                                )
                        );

        if (customer.getMarket() == null) {
            throw new RuntimeException("Access denied. Customer does not belong to your assigned market.");
        }

        boolean assigned =
                assignmentRepository
                        .existsByEmployeeIdAndMarketIdAndIsActiveTrue(
                                user.getId(),
                                customer.getMarket().getId()
                        );

        if(!assigned) {

            throw new RuntimeException(
                    "Access denied. Customer does not belong to your assigned market."
            );
        }
    }
}