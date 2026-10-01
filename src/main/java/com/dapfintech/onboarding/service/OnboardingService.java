package com.dapfintech.onboarding.service;

import java.io.ByteArrayInputStream;

import org.springframework.web.multipart.MultipartFile;

import com.dapfintech.loan.dto.response.LoanResponse;
import com.dapfintech.onboarding.dto.request.OnboardSingleLoanRequest;
import com.dapfintech.onboarding.dto.response.OnboardingSummaryResponse;

public interface OnboardingService {

    ByteArrayInputStream generateOnboardingTemplate();

    OnboardingSummaryResponse importExcel(MultipartFile file);

    OnboardingSummaryResponse importExcel(MultipartFile file, java.util.UUID marketId, String marketName, java.time.LocalDate asOfDate);

    LoanResponse onboardSingleLoan(OnboardSingleLoanRequest request);
}
