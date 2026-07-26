package com.dapfintech.report.service;

import java.util.List;

import com.dapfintech.report.dto.response.RecoveryEfficiencyResponse;

public interface RecoveryEfficiencyReportService {

    List<RecoveryEfficiencyResponse>
    getRecoveryEfficiencyReport();
}