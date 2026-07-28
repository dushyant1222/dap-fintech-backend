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
import com.dapfintech.security.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalTransferServiceImpl implements InternalTransferService {

    private final InternalTransferRepository internalTransferRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

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
        
        // TODO: Update DayBook if receiver is an Employee
        
        return mapToResponse(saved);
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
