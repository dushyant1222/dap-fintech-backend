package com.dapfintech.loan.dto.response;

import java.util.List;

import com.dapfintech.customer.address.dto.response.AddressResponse;
import com.dapfintech.customer.document.dto.response.DocumentResponse;
import com.dapfintech.customer.dto.response.CustomerResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanDetailsResponse {

    private LoanResponse loan;

    private LoanSummaryResponse summary;

    private CustomerResponse customer;

    private List<AddressResponse> addresses;

    private List<LoanChargeResponse> charges;

    private List<DocumentResponse> documents;

    private List<RepaymentScheduleResponse> repaymentSchedule;

    private List<LoanApprovalResponse> approvalHistory;
}