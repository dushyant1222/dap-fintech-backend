package com.dapfintech.loan.mapper;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.dapfintech.loan.dto.response.RepaymentScheduleResponse;
import com.dapfintech.loan.entity.LoanRepaymentSchedule;
import com.dapfintech.loan.enums.RepaymentStatus;

@Component
public class LoanRepaymentScheduleMapper {
	private String getDisplayStatus(
	        LoanRepaymentSchedule schedule
	) {

	    if (schedule.getRepaymentStatus()
	            == RepaymentStatus.PAID) {

	        return "PAID";
	    }

	    LocalDate today = LocalDate.now();

	    if (schedule.getDueDate().isBefore(today)) {

	        return "OVERDUE";
	    }

	    if (schedule.getDueDate().isEqual(today)) {

	        return "TODAY";
	    }

	    return "UPCOMING";

	}

    public RepaymentScheduleResponse toResponse(
            LoanRepaymentSchedule schedule
    ) {

        return RepaymentScheduleResponse.builder()
                .id(schedule.getId())
                .installmentNumber(
                        schedule.getInstallmentNumber()
                )
                .dueDate(
                        schedule.getDueDate()
                )
                .displayStatus(

                        getDisplayStatus(
                                schedule
                        )

                )
                .principalAmount(
                        schedule.getPrincipalAmount()
                )
                .interestAmount(
                        schedule.getInterestAmount()
                )
                .installmentAmount(
                        schedule.getInstallmentAmount()
                )
                .dueAmount(
                        schedule.getDueAmount()
                )
                .paidAmount(
                        schedule.getPaidAmount()
                )
                .outstandingAmount(
                        schedule.getOutstandingAmount()
                )
                .repaymentStatus(
                        schedule.getRepaymentStatus()
                )
                .build();
    }
}