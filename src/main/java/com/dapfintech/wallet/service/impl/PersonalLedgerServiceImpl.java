package com.dapfintech.wallet.service.impl;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.exception.ResourceNotFoundException;
import com.dapfintech.wallet.dto.LedgerSummaryResponse;
import com.dapfintech.wallet.dto.PersonalLedgerRequest;
import com.dapfintech.wallet.dto.PersonalLedgerResponse;
import com.dapfintech.wallet.entity.PersonalLedger;
import com.dapfintech.wallet.enums.TransactionType;
import com.dapfintech.wallet.repository.PersonalLedgerRepository;
import com.dapfintech.wallet.service.PersonalLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalLedgerServiceImpl implements PersonalLedgerService {

    private final PersonalLedgerRepository repository;
    private final UserRepository userRepository;

    @Override
    public PersonalLedgerResponse addTransaction(PersonalLedgerRequest request) {
        User admin = userRepository.findById(request.getAdminId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        PersonalLedger ledger = new PersonalLedger();
        ledger.setAdmin(admin);
        ledger.setTransactionType(request.getTransactionType());
        ledger.setAmount(request.getAmount());
        ledger.setCategory(request.getCategory());
        ledger.setRemarks(request.getRemarks());
        ledger.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now());

        ledger = repository.save(ledger);

        return mapToResponse(ledger);
    }

    @Override
    public List<PersonalLedgerResponse> getMyLedger(UUID adminId) {
        return repository.findByAdminIdOrderByTransactionDateDesc(adminId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LedgerSummaryResponse getSummary(UUID adminId) {
        List<PersonalLedger> ledgers = repository.findByAdminIdOrderByTransactionDateDesc(adminId);
        
        BigDecimal totalIncome = ledgers.stream()
                .filter(l -> l.getTransactionType() == TransactionType.CREDIT)
                .map(PersonalLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalExpenses = ledgers.stream()
                .filter(l -> l.getTransactionType() == TransactionType.DEBIT)
                .map(PersonalLedger::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);
        
        return new LedgerSummaryResponse(totalIncome, totalExpenses, netBalance);
    }

    private PersonalLedgerResponse mapToResponse(PersonalLedger ledger) {
        PersonalLedgerResponse response = new PersonalLedgerResponse();
        response.setId(ledger.getId());
        response.setAdminId(ledger.getAdmin().getId());
        response.setTransactionType(ledger.getTransactionType());
        response.setAmount(ledger.getAmount());
        response.setCategory(ledger.getCategory());
        response.setRemarks(ledger.getRemarks());
        response.setTransactionDate(ledger.getTransactionDate());
        return response;
    }
}
