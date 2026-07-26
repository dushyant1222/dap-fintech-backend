package com.dapfintech.collection.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.collection.dto.request.CreateVisitRequest;
import com.dapfintech.collection.dto.response.VisitResponse;
import com.dapfintech.collection.service.CustomerVisitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
public class CustomerVisitController {

    private final CustomerVisitService service;

    @PostMapping
    public ResponseEntity<VisitResponse>
    createVisit(
            @RequestBody
            CreateVisitRequest request
    ) {

        return ResponseEntity.ok(
                service.createVisit(
                        request
                )
        );
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<
            List<VisitResponse>>
    getCustomerVisits(
            @PathVariable UUID customerId
    ) {

        return ResponseEntity.ok(
                service.getCustomerVisits(
                        customerId
                )
        );
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<
            List<VisitResponse>>
    getEmployeeVisits(
            @PathVariable UUID employeeId
    ) {

        return ResponseEntity.ok(
                service.getEmployeeVisits(
                        employeeId
                )
        );
    }
}