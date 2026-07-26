package com.dapfintech.loan.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dapfintech.loan.dto.request.CreateLoanChargeRequest;
import com.dapfintech.loan.dto.request.UpdateLoanChargeRequest;
import com.dapfintech.loan.dto.response.LoanChargeResponse;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.loan.entity.LoanCharge;
import com.dapfintech.loan.mapper.LoanChargeMapper;
import com.dapfintech.loan.repository.LoanChargeRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.service.LoanChargeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanChargeServiceImpl
        implements LoanChargeService {

    private final LoanRepository loanRepository;
    private final LoanChargeRepository loanChargeRepository;
    private final LoanChargeMapper loanChargeMapper;

    @Override
    public LoanChargeResponse createCharge(
            CreateLoanChargeRequest request
    ) {

        Loan loan =
                loanRepository.findById(
                        request.getLoanId()
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "Loan not found"
                        )
                );

        LoanCharge charge =
                LoanCharge.builder()
                        .loan(loan)
                        .chargeType(
                                request.getChargeType()
                        )
                        .chargeAmount(
                                request.getChargeAmount()
                        )
                        .isMandatory(
                                request.getIsMandatory()
                        )
                        .build();

        LoanCharge saved =
                loanChargeRepository.save(charge);

        return loanChargeMapper.toResponse(saved);
    }

    @Override
    public LoanChargeResponse updateCharge(
            UUID chargeId,
            UpdateLoanChargeRequest request
    ) {

        LoanCharge charge =
                loanChargeRepository.findById(chargeId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Charge not found"
                                )
                        );

        charge.setChargeType(
                request.getChargeType()
        );

        charge.setChargeAmount(
                request.getChargeAmount()
        );

        charge.setIsMandatory(
                request.getIsMandatory()
        );

        loanChargeRepository.save(charge);

        return loanChargeMapper.toResponse(charge);
    }

    @Override
    public LoanChargeResponse getChargeById(
            UUID chargeId
    ) {

        LoanCharge charge =
                loanChargeRepository.findById(chargeId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Charge not found"
                                )
                        );

        return loanChargeMapper.toResponse(charge);
    }

    @Override
    public List<LoanChargeResponse> getLoanCharges(
            UUID loanId
    ) {

        return loanChargeRepository
                .findByLoanId(loanId)
                .stream()
                .map(loanChargeMapper::toResponse)
                .toList();
    }

    @Override
    public void deleteCharge(
            UUID chargeId
    ) {

        LoanCharge charge =
                loanChargeRepository.findById(chargeId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Charge not found"
                                )
                        );

        loanChargeRepository.delete(charge);
    }
}