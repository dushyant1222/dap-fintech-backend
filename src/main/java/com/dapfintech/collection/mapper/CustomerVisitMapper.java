package com.dapfintech.collection.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.collection.dto.response.VisitResponse;
import com.dapfintech.collection.entity.CustomerVisit;

@Component
public class CustomerVisitMapper {

    public VisitResponse toResponse(
            CustomerVisit visit
    ) {

        return VisitResponse.builder()
                .id(visit.getId())
                .customerId(
                        visit.getCustomer().getId()
                )
                .customerName(
                        visit.getCustomer().getFirstName()
                        + " "
                        + visit.getCustomer().getLastName()
                )
                .employeeId(
                        visit.getEmployee().getId()
                )
                .employeeName(
                        visit.getEmployee().getFullName()
                )
                .visitDate(
                        visit.getVisitDate()
                )
                .visitStatus(
                        visit.getVisitStatus()
                )
                .remarks(
                        visit.getRemarks()
                )
                .promiseAmount(
                        visit.getPromiseAmount()
                )
                .promiseDate(
                        visit.getPromiseDate()
                )
                .build();
    }
}