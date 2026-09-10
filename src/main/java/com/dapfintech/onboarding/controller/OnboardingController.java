package com.dapfintech.onboarding.controller;

import java.io.ByteArrayInputStream;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.loan.dto.response.LoanResponse;
import com.dapfintech.onboarding.dto.request.OnboardSingleLoanRequest;
import com.dapfintech.onboarding.dto.response.OnboardingSummaryResponse;
import com.dapfintech.onboarding.service.OnboardingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() throws Exception {
        ByteArrayInputStream stream = onboardingService.generateOnboardingTemplate();
        byte[] bytes = stream.readAllBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historical_loans_template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OnboardingSummaryResponse>> importExcel(
            @RequestParam("file") MultipartFile file
    ) {
        OnboardingSummaryResponse response = onboardingService.importExcel(file);
        return ResponseEntity.ok(
                ApiResponse.<OnboardingSummaryResponse>builder()
                        .success(true)
                        .message("Data onboarding completed: " + response.getSuccessCount() + " loans onboarded successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/single")
    public ResponseEntity<ApiResponse<LoanResponse>> onboardSingle(
            @Valid @RequestBody OnboardSingleLoanRequest request
    ) {
        LoanResponse response = onboardingService.onboardSingleLoan(request);
        return ResponseEntity.ok(
                ApiResponse.<LoanResponse>builder()
                        .success(true)
                        .message("Historical loan onboarded successfully with Code: " + response.getLoanCode())
                        .data(response)
                        .build()
        );
    }
}
