package com.dapfintech.capital.service;

import java.util.List;

import com.dapfintech.capital.dto.CapitalSummaryResponse;
import com.dapfintech.capital.dto.CreateCapitalInRequest;
import com.dapfintech.capital.dto.CreateCashSettlementRequest;
import com.dapfintech.capital.dto.CreateExpenseRequest;
import com.dapfintech.capital.dto.PivotFilterRequest;
import com.dapfintech.capital.dto.PivotTableResponse;
import com.dapfintech.capital.entity.CapitalIn;
import com.dapfintech.capital.entity.CashSettlement;
import com.dapfintech.capital.entity.Expense;

public interface CapitalService {

    CapitalIn addCapitalIn(CreateCapitalInRequest request);

    List<CapitalIn> getAllCapitalIn();

    Expense addExpense(CreateExpenseRequest request);

    List<Expense> getAllExpenses();

    CashSettlement addCashSettlement(CreateCashSettlementRequest request);

    List<CashSettlement> getAllCashSettlements();

    CapitalSummaryResponse getCapitalSummary();

    PivotTableResponse getPivotTable(PivotFilterRequest filter);
}
