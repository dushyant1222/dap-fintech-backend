package com.dapfintech.customer.dto.response;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.*;

import com.dapfintech.customer.address.dto.response.AddressResponse;
import com.dapfintech.customer.document.dto.response.DocumentResponse;
import com.dapfintech.customer.enums.CustomerStatus;
import com.dapfintech.customer.enums.Gender;
import com.dapfintech.customer.guarantor.dto.response.GuarantorResponse;
import com.dapfintech.loan.dto.response.LoanResponse;
import com.dapfintech.loan.dto.response.LoanSummaryResponse;


@Data
@Builder
public class CustomerDetailsResponse {
    
    private CustomerResponse customer;

    private List<AddressResponse> addresses;

    private LoanResponse loan;

    private LoanSummaryResponse loanSummary;

    private GuarantorResponse guarantor;

    private List<DocumentResponse> documents;

    

}