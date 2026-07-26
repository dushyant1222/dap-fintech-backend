package com.dapfintech.report.dto.request;

import java.time.LocalDate;

import lombok.Data;

@Data
public class DateRangeReportRequest {
	private LocalDate fromDate;
	private LocalDate toDate;
}
