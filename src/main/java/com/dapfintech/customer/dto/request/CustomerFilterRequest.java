package com.dapfintech.customer.dto.request;

import java.util.UUID;

import com.dapfintech.customer.enums.CustomerStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFilterRequest {

    /*
     * Searches:
     *
     * firstName
     * lastName
     * mobileNumber
     * customerCode
     * email
     */
    private String keyword;

    /*
     * ACTIVE / SUSPENDED / etc.
     */
    private CustomerStatus status;

    /*
     * Mainly useful for admin.
     *
     * Employee access will still be restricted
     * by backend access-control rules.
     */
    private UUID marketId;

}