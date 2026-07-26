package com.dapfintech.customer.history.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.customer.history.enums.CustomerHistoryAction;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerHistoryResponse {

    private UUID id;

    private UUID customerId;

    private CustomerHistoryAction action;

    private String title;

    private String description;

    private String oldValue;

    private String newValue;

    private UUID performedById;

    private String performedByName;

    private LocalDateTime createdAt;
}