package com.dapfintech.customer.history.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.auth.entity.User;
import com.dapfintech.customer.history.dto.response.CustomerHistoryResponse;
import com.dapfintech.customer.history.entity.CustomerHistory;

@Component
public class CustomerHistoryMapper {

    public CustomerHistoryResponse toResponse(
            CustomerHistory history
    ) {

        User performedBy =
                history.getPerformedBy();

        return CustomerHistoryResponse.builder()
                .id(history.getId())
                .customerId(
                        history.getCustomer().getId()
                )
                .action(history.getAction())
                .title(history.getTitle())
                .description(history.getDescription())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .performedById(
                        performedBy != null
                                ? performedBy.getId()
                                : null
                )
                .performedByName(
                        performedBy != null
                                ? performedBy.getFullName()
                                : "System"
                )
                .createdAt(history.getCreatedAt())
                .build();
    }
}