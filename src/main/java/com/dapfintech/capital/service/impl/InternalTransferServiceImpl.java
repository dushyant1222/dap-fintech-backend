package com.dapfintech.capital.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
import com.dapfintech.employee.entity.DayBookTransaction;
import com.dapfintech.employee.repository.DayBookRepository;
import com.dapfintech.employee.repository.DayBookTransactionRepository;
import com.dapfintech.employee.service.DayBookService;
import com.dapfintech.security.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalTransferServiceImpl implements InternalTransferService {

    private final InternalTransferRepository internalTransferRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final DayBookRepository dayBookRepository;
    private final DayBookTransactionRepository dayBookTransactionRepository;

    @Lazy
    @Autowired
    private DayBookService dayBookService;

    @Override
    @Transactional
    public InternalTransferResponse initiateTransfer(InternalTransferRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
                
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        LocalDateTime txDate = LocalDateTime.now();
        if (request.getTransferDate() != null && !request.getTransferDate().trim().isEmpty()) {
            try {
                String dStr = request.getTransferDate().trim();
                if (dStr.contains("T")) {
                    if (dStr.length() >= 19) {
                        txDate = LocalDateTime.parse(dStr.substring(0, 19));
                    } else {
                        txDate = LocalDateTime.parse(dStr);
                    }
                } else {
                    txDate = LocalDate.parse(dStr).atTime(12, 0);
                }
            } catch (Exception e) {
                try {
                    txDate = java.time.OffsetDateTime.parse(request.getTransferDate()).toLocalDateTime();
                } catch (Exception ex) {
                    txDate = LocalDateTime.now();
                }
            }
        }

        InternalTransfer transfer = InternalTransfer.builder()
                .sender(sender)
                .receiver(receiver)
                .amount(request.getAmount())
                .status(TransferStatus.PENDING)
                .transferDate(txDate)
                .category(request.getCategory())
                .transferMode(request.getTransferMode() != null ? com.dapfintech.capital.enums.TransferMode.valueOf(request.getTransferMode()) : com.dapfintech.capital.enums.TransferMode.ONLINE)
                .remarks(request.getRemarks())
                .build();

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
        
        LocalDate transferDay = transfer.getTransferDate() != null ? transfer.getTransferDate().toLocalDate() : LocalDate.now();

        // Update DayBook if receiver is an Employee
        if (transfer.getReceiver().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(currentUserId, transferDay)
                    .orElseGet(() -> {
                        dayBookService.getOrCreateDayBook(currentUserId, transferDay);
                        return dayBookRepository.findByEmployeeIdAndDate(currentUserId, transferDay).orElse(null);
                    });

            if (dayBook != null) {
                String txType;
                if (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH) {
                    if (dayBook.getCashIncomingTransfers() == null) dayBook.setCashIncomingTransfers(BigDecimal.ZERO);
                    dayBook.setCashIncomingTransfers(dayBook.getCashIncomingTransfers().add(transfer.getAmount()));
                    txType = "CASH_INCOMING_TRANSFER";
                } else {
                    if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(BigDecimal.ZERO);
                    dayBook.setIncomingTransfers(dayBook.getIncomingTransfers().add(transfer.getAmount()));
                    txType = "INCOMING_TRANSFER";
                }
                updateDaybookClosingBalance(dayBook);
                dayBookRepository.save(dayBook);
                propagateClosingBalanceForward(dayBook);

                DayBookTransaction rxTx = new DayBookTransaction();
                rxTx.setDayBook(dayBook);
                rxTx.setEmployeeId(currentUserId);
                rxTx.setType(txType);
                rxTx.setAmount(transfer.getAmount());
                String remarks = (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH ? "Cash from: " : "Online from: ")
                        + transfer.getSender().getFullName()
                        + (transfer.getRemarks() != null && !transfer.getRemarks().isBlank() ? " (" + transfer.getRemarks() + ")" : "");
                rxTx.setRemarks(remarks);
                rxTx.setCreatedAt(transfer.getTransferDate() != null ? transfer.getTransferDate() : LocalDateTime.now());
                dayBookTransactionRepository.save(rxTx);
            }
        }
        
        // Update DayBook if sender is an Employee
        if (transfer.getSender().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(transfer.getSender().getId(), transferDay)
                    .orElseGet(() -> {
                        dayBookService.getOrCreateDayBook(transfer.getSender().getId(), transferDay);
                        return dayBookRepository.findByEmployeeIdAndDate(transfer.getSender().getId(), transferDay).orElse(null);
                    });

            if (dayBook != null) {
                String txType;
                if (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH) {
                    if (dayBook.getCashOutgoingTransfers() == null) dayBook.setCashOutgoingTransfers(BigDecimal.ZERO);
                    dayBook.setCashOutgoingTransfers(dayBook.getCashOutgoingTransfers().add(transfer.getAmount()));
                    txType = "CASH_OUTGOING_TRANSFER";
                } else {
                    if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(BigDecimal.ZERO);
                    dayBook.setOutgoingTransfers(dayBook.getOutgoingTransfers().add(transfer.getAmount()));
                    txType = "OUTGOING_TRANSFER";
                }
                updateDaybookClosingBalance(dayBook);
                dayBookRepository.save(dayBook);
                propagateClosingBalanceForward(dayBook);

                DayBookTransaction txTx = new DayBookTransaction();
                txTx.setDayBook(dayBook);
                txTx.setEmployeeId(transfer.getSender().getId());
                txTx.setType(txType);
                txTx.setAmount(transfer.getAmount());
                String remarks = (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH ? "Cash to: " : "Online to: ")
                        + transfer.getReceiver().getFullName()
                        + (transfer.getRemarks() != null && !transfer.getRemarks().isBlank() ? " (" + transfer.getRemarks() + ")" : "");
                txTx.setRemarks(remarks);
                txTx.setCreatedAt(transfer.getTransferDate() != null ? transfer.getTransferDate() : LocalDateTime.now());
                dayBookTransactionRepository.save(txTx);
            }
        }
        
        return mapToResponse(saved);
    }

    private void propagateClosingBalanceForward(DayBook startDayBook) {
        if (startDayBook == null || startDayBook.getDate() == null) return;
        UUID empId = startDayBook.getEmployeeId();
        LocalDate currDate = startDayBook.getDate();
        BigDecimal prevClosing = startDayBook.getClosingBalance();

        List<DayBook> subsequentDayBooks = dayBookRepository.findByEmployeeIdOrderByDateAsc(empId);
        for (DayBook nextDb : subsequentDayBooks) {
            if (nextDb.getDate().isAfter(currDate)) {
                nextDb.setOpeningBalance(prevClosing);
                updateDaybookClosingBalance(nextDb);
                dayBookRepository.save(nextDb);
                prevClosing = nextDb.getClosingBalance();
            }
        }
    }

    private void updateDaybookClosingBalance(DayBook dayBook) {
        if (dayBook.getOpeningBalance() == null) dayBook.setOpeningBalance(BigDecimal.ZERO);
        if (dayBook.getCollections() == null) dayBook.setCollections(BigDecimal.ZERO);
        if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(BigDecimal.ZERO);
        if (dayBook.getCashIncomingTransfers() == null) dayBook.setCashIncomingTransfers(BigDecimal.ZERO);
        if (dayBook.getSpends() == null) dayBook.setSpends(BigDecimal.ZERO);
        if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(BigDecimal.ZERO);
        if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(BigDecimal.ZERO);
        if (dayBook.getCashOutgoingTransfers() == null) dayBook.setCashOutgoingTransfers(BigDecimal.ZERO);
        if (dayBook.getOfficeRemittance() == null) dayBook.setOfficeRemittance(BigDecimal.ZERO);

        BigDecimal newClosing = dayBook.getOpeningBalance()
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
                .transferMode(t.getTransferMode() != null ? t.getTransferMode().name() : null)
                .remarks(t.getRemarks())
                .build();
    }
}
