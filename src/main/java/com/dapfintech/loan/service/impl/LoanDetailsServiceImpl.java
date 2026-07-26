package com.dapfintech.loan.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dapfintech.customer.address.dto.response.AddressResponse;
import com.dapfintech.customer.address.service.AddressService;
import com.dapfintech.customer.document.dto.response.DocumentResponse;
import com.dapfintech.customer.document.service.DocumentService;
import com.dapfintech.customer.dto.response.CustomerResponse;
import com.dapfintech.customer.service.CustomerService;
import com.dapfintech.loan.dto.response.LoanApprovalResponse;
import com.dapfintech.loan.dto.response.LoanChargeResponse;
import com.dapfintech.loan.dto.response.LoanDetailsResponse;
import com.dapfintech.loan.dto.response.LoanResponse;
import com.dapfintech.loan.dto.response.LoanSummaryResponse;
import com.dapfintech.loan.dto.response.RepaymentScheduleResponse;
import com.dapfintech.loan.service.LoanApprovalService;
import com.dapfintech.loan.service.LoanChargeService;
import com.dapfintech.loan.service.LoanDetailsService;
import com.dapfintech.loan.service.LoanRepaymentScheduleService;
import com.dapfintech.loan.service.LoanService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanDetailsServiceImpl
        implements LoanDetailsService {

    private final LoanService loanService;

    private final CustomerService customerService;

    private final AddressService addressService;

    private final LoanChargeService loanChargeService;

    private final LoanRepaymentScheduleService
            repaymentScheduleService;

    private final LoanApprovalService
            loanApprovalService;

    private final DocumentService documentService;


    @Override
    public LoanDetailsResponse getLoanDetails(
            UUID loanId
    ) {

        // -----------------------------------------------------
        // 1. GET LOAN
        // -----------------------------------------------------

        LoanResponse loan =
                loanService.getLoanById(
                        loanId
                );


        // -----------------------------------------------------
        // 2. GET CUSTOMER ID FROM LOAN RESPONSE
        // -----------------------------------------------------

        UUID customerId =
                loan.getCustomerId();


        // -----------------------------------------------------
        // 3. GET COMPLETE CUSTOMER INFORMATION
        // -----------------------------------------------------

        CustomerResponse customer =
                customerService.getCustomerById(
                        customerId
                );


        // -----------------------------------------------------
        // 4. GET CUSTOMER ADDRESSES
        // -----------------------------------------------------

        List<AddressResponse> addresses =
                addressService.getCustomerAddresses(
                        customerId
                );


        // -----------------------------------------------------
        // 5. GET LOAN SUMMARY
        // -----------------------------------------------------

        LoanSummaryResponse summary =
                loanService.getLoanSummary(
                        loanId
                );


        // -----------------------------------------------------
        // 6. GET LOAN CHARGES
        // -----------------------------------------------------

        List<LoanChargeResponse> charges =
                loanChargeService.getLoanCharges(
                        loanId
                );


        // -----------------------------------------------------
        // 7. GET REPAYMENT SCHEDULE
        // -----------------------------------------------------

        List<RepaymentScheduleResponse>
                repaymentSchedule =

                repaymentScheduleService
                        .getLoanSchedule(
                                loanId
                        );


        // -----------------------------------------------------
        // 8. GET APPROVAL HISTORY
        // -----------------------------------------------------

        List<LoanApprovalResponse>
                approvalHistory =

                loanApprovalService
                        .getApprovalHistory(
                                loanId
                        );


        // -----------------------------------------------------
        // 9. GET CUSTOMER DOCUMENTS
        // -----------------------------------------------------

        List<DocumentResponse> documents =
                documentService
                        .getCustomerDocuments(
                                customerId
                        );


        // -----------------------------------------------------
        // 10. BUILD COMPLETE LOAN DETAILS RESPONSE
        // -----------------------------------------------------

        return LoanDetailsResponse
                .builder()

                .loan(
                        loan
                )

                .summary(
                        summary
                )

                .customer(
                        customer
                )

                .addresses(
                        addresses
                )

                .charges(
                        charges
                )

                .documents(
                        documents
                )

                .repaymentSchedule(
                        repaymentSchedule
                )

                .approvalHistory(
                        approvalHistory
                )

                .build();
    }
}