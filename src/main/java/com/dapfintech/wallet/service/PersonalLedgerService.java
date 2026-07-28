package com.dapfintech.wallet.service;

import com.dapfintech.wallet.dto.LedgerSummaryResponse;
import com.dapfintech.wallet.dto.PersonalLedgerRequest;
import com.dapfintech.wallet.dto.PersonalLedgerResponse;

import java.util.List;
import java.util.UUID;

public interface PersonalLedgerService {
    PersonalLedgerResponse addTransaction(PersonalLedgerRequest request);
    List<PersonalLedgerResponse> getMyLedger(UUID adminId);
    LedgerSummaryResponse getSummary(UUID adminId);
}
