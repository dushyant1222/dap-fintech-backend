package com.dapfintech.loan.service.impl;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.loan.service.LoanOverdueService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanOverdueServiceImpl implements LoanOverdueService {

    private final LoanRepaymentScheduleRepository scheduleRepository;

    @Override
    @Transactional
    public void markOverdues() {
        // Single bulk UPDATE instead of loading all rows + N individual saves
        scheduleRepository.bulkMarkOverdue();
    }
}