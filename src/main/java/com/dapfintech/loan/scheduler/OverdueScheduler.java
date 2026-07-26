package com.dapfintech.loan.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dapfintech.loan.service.LoanOverdueService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OverdueScheduler {

    private final LoanOverdueService
            overdueService;

    @Scheduled(cron = "0 0 0 * * *")
    public void runDaily() {

        overdueService.markOverdues();
    }
}