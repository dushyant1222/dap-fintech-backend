package com.dapfintech.capital.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.capital.dto.request.InternalTransferRequest;
import com.dapfintech.capital.dto.response.InternalTransferResponse;
import com.dapfintech.capital.entity.InternalTransfer;
import com.dapfintech.capital.enums.TransferStatus;
import com.dapfintech.capital.repository.InternalTransferRepository;
import com.dapfintech.capital.service.InternalTransferService;
import com.dapfintech.employee.entity.DayBook;
import com.dapfintech.employee.repository.DayBookRepository;
import com.dapfintech.security.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalTransferServiceImpl implements InternalTransferService {

    private final InternalTransferRepository internalTransferRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final DayBookRepository dayBookRepository;

    @Override
    @Transactional
    public InternalTransferResponse initiateTransfer(InternalTransferRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
                
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        InternalTransfer transfer = InternalTransfer.builder()
                .sender(sender)
                .receiver(receiver)
                .amount(request.getAmount())
                .status(TransferStatus.PENDING)
                .transferDate(LocalDateTime.now())
                .category(request.getCategory())
                .transferMode(request.getTransferMode() != null ? com.dapfintech.capital.enums.TransferMode.valueOf(request.getTransferMode()) : com.dapfintech.capital.enums.TransferMode.ONLINE)
                .remarks(request.getRemarks())
                .build();

        // Note: Vault deduction for Admins should be handled dynamically in getCapitalSummary 
        // by looking at outgoing transfers vs incoming transfers. Or we can inject CapitalService.
        
        InternalTransfer saved = internalTransferRepository.save(transfer);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public InternalTransferResponse acceptTransfer(UUID transferId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        
        InternalTransfer transfer = internalTransferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));
                
        if (!transfer.getReceiver().getId().equals(currentUserId)) {
            throw new RuntimeException("Only the receiver can accept this transfer");
        }
        
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Transfer is not in PENDING status");
        }
        
        transfer.setStatus(TransferStatus.ACCEPTED);
        InternalTransfer saved = internalTransferRepository.save(transfer);
        
        // Update DayBook if receiver is an Employee
        if (transfer.getReceiver().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            java.time.LocalDate today = java.time.LocalDate.now();
            dayBookRepository.findByEmployeeIdAndDate(currentUserId, today).ifPresent(dayBook -> {
                if (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH) {
                    if (dayBook.getCashIncomingTransfers() == null) dayBook.setCashIncomingTransfers(java.math.BigDecimal.ZERO);
                    dayBook.setCashIncomingTransfers(dayBook.getCashIncomingTransfers().add(transfer.getAmount()));
                } else {
                    if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(java.math.BigDecimal.ZERO);
                    dayBook.setIncomingTransfers(dayBook.getIncomingTransfers().add(transfer.getAmount()));
                }
                updateDaybookClosingBalance(dayBook);
                dayBookRepository.save(dayBook);
            });
        }
        
        // Update DayBook if sender is an Employee
        if (transfer.getSender().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            java.time.LocalDate today = java.time.LocalDate.now();
            dayBookRepository.findByEmployeeIdAndDate(transfer.getSender().getId(), today).ifPresent(dayBook -> {
                if (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH) {
                    if (dayBook.getCashOutgoingTransfers() == null) dayBook.setCashOutgoingTransfers(java.math.BigDecimal.ZERO);
                    dayBook.setCashOutgoingTransfers(dayBook.getCashOutgoingTransfers().add(transfer.getAmount()));
                } else {
                    if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(java.math.BigDecimal.ZERO);
                    dayBook.setOutgoingTransfers(dayBook.getOutgoingTransfers().add(transfer.getAmount()));
                }
                updateDaybookClosingBalance(dayBook);
                dayBookRepository.save(dayBook);
            });
        }
        
        return mapToResponse(saved);
    }

    private void updateDaybookClosingBalance(DayBook dayBook) {
        if (dayBook.getOpeningBalance() == null) dayBook.setOpeningBalance(java.math.BigDecimal.ZERO);
        if (dayBook.getCollections() == null) dayBook.setCollections(java.math.BigDecimal.ZERO);
        if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(java.math.BigDecimal.ZERO);
        if (dayBook.getCashIncomingTransfers() == null) dayBook.setCashIncomingTransfers(java.math.BigDecimal.ZERO);
        if (dayBook.getSpends() == null) dayBook.setSpends(java.math.BigDecimal.ZERO);
        if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(java.math.BigDecimal.ZERO);
        if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(java.math.BigDecimal.ZERO);
        if (dayBook.getCashOutgoingTransfers() == null) dayBook.setCashOutgoingTransfers(java.math.BigDecimal.ZERO);
        if (dayBook.getOfficeRemittance() == null) dayBook.setOfficeRemittance(java.math.BigDecimal.ZERO);

        java.math.BigDecimal newClosing = dayBook.getOpeningBalance()
                .add(dayBook.getCollections())
                .add(dayBook.getIncomingTransfers())
                .add(dayBook.getCashIncomingTransfers())
                .subtract(dayBook.getSpends())
                .subtract(dayBook.getLoansDisbursed())
                .subtract(dayBook.getOutgoingTransfers())
                .subtract(dayBook.getCashOutgoingTransfers())
                .subtract(dayBook.getOfficeRemittance());
        dayBook.setClosingBalance(newClosing);
    }


    @Override
    @Transactional
    public InternalTransferResponse rejectTransfer(UUID transferId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        
        InternalTransfer transfer = internalTransferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));
                
        if (!transfer.getReceiver().getId().equals(currentUserId)) {
            throw new RuntimeException("Only the receiver can reject this transfer");
        }
        
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Transfer is not in PENDING status");
        }
        
        transfer.setStatus(TransferStatus.REJECTED);
        InternalTransfer saved = internalTransferRepository.save(transfer);
        return mapToResponse(saved);
    }

    @Override
    public List<InternalTransferResponse> getPendingIncomingTransfers() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return internalTransferRepository.findByReceiverIdAndStatusOrderByTransferDateDesc(currentUserId, TransferStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InternalTransferResponse> getMyIncomingTransfers() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return internalTransferRepository.findByReceiverIdOrderByTransferDateDesc(currentUserId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InternalTransferResponse> getMyOutgoingTransfers() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return internalTransferRepository.findBySenderIdOrderByTransferDateDesc(currentUserId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private InternalTransferResponse mapToResponse(InternalTransfer t) {
        return InternalTransferResponse.builder()
                .id(t.getId())
                .senderId(t.getSender().getId())
                .senderName(t.getSender().getFullName())
                .receiverId(t.getReceiver().getId())
                .receiverName(t.getReceiver().getFullName())
                .amount(t.getAmount())
                .status(t.getStatus())
                .transferDate(t.getTransferDate())
                .category(t.getCategory())
                .remarks(t.getRemarks())
                .build();
    }
}

