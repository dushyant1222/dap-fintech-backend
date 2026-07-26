package com.dapfintech.loan.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.RepaymentStatus;
import com.dapfintech.loan.repository.LoanRepaymentScheduleRepository;
import com.dapfintech.loan.service.LoanOverdueService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanOverdueServiceImpl
        implements LoanOverdueService {

    private final LoanRepaymentScheduleRepository
            scheduleRepository;

    @Override
    public void markOverdues() {

        List<LoanRepaymentSchedule>
                schedules =
                scheduleRepository
                        .findByRepaymentStatus(
                                RepaymentStatus.PENDING
                        );

        LocalDate today =
                LocalDate.now();

        for (
                LoanRepaymentSchedule schedule
                        : schedules
        ) {

            if (
                    schedule.getDueDate()
                            .isBefore(today)
            ) {

                schedule.setRepaymentStatus(
                        RepaymentStatus.OVERDUE
                );

                scheduleRepository.save(
                        schedule
                );
            }
        }
    }
}