package com.dapfintech.capital.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import com.dapfintech.market.entity.EmployeeMarketAssignment;
import com.dapfintech.market.entity.Market;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
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
    private final EmployeeMarketAssignmentRepository assignmentRepository;

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

        // Resolve active markets
        Market senderMarket = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(sender.getId())
                .map(EmployeeMarketAssignment::getMarket).orElse(null);
        Market receiverMarket = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(receiver.getId())
                .map(EmployeeMarketAssignment::getMarket).orElse(null);

        // Date resolution: if transferDate is explicitly specified, use it. Otherwise use active sequential daybook date!
        LocalDate senderActiveDate = dayBookService.getActiveDayBookDate(sender.getId());
        LocalDateTime txDate;
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
                    txDate = LocalDate.parse(dStr).atTime(LocalTime.now());
                }
                if (txDate.toLocalTime().equals(LocalTime.MIDNIGHT)) {
                    txDate = txDate.toLocalDate().atTime(LocalTime.now());
                }
            } catch (Exception e) {
                try {
                    txDate = java.time.OffsetDateTime.parse(request.getTransferDate()).toLocalDateTime();
                } catch (Exception ex) {
                    txDate = senderActiveDate.atTime(LocalTime.now());
                }
            }
            if (txDate.toLocalDate().isBefore(senderActiveDate) ||
                    dayBookRepository.findByEmployeeIdAndDate(sender.getId(), txDate.toLocalDate())
                            .map(d -> d.getStatus() == com.dapfintech.employee.enums.DayBookStatus.CLOSED).orElse(false)) {
                txDate = senderActiveDate.atTime(txDate.toLocalTime());
            }
        } else {
            txDate = senderActiveDate.atTime(LocalTime.now());
        }

        InternalTransfer transfer = InternalTransfer.builder()
                .sender(sender)
                .receiver(receiver)
                .senderMarket(senderMarket)
                .receiverMarket(receiverMarket)
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

        User currentUser = securityUtils.getCurrentUser();
        boolean isReceiver = transfer.getReceiver().getId().equals(currentUserId);
        boolean isOfficeRemitAdmin = "OFFICE_REMITTANCE".equalsIgnoreCase(transfer.getCategory()) && currentUser != null && currentUser.isAdmin();

        if (!isReceiver && !isOfficeRemitAdmin) {
            throw new RuntimeException("Only the receiver can accept this transfer");
        }

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Transfer is not in PENDING status");
        }

        transfer.setStatus(TransferStatus.ACCEPTED);
        InternalTransfer saved = internalTransferRepository.save(transfer);

        LocalDate senderActiveDate = dayBookService.getActiveDayBookDate(transfer.getSender().getId());
        LocalDate transferDay = transfer.getTransferDate() != null
                ? transfer.getTransferDate().toLocalDate()
                : senderActiveDate;
        if (transferDay.isBefore(senderActiveDate) ||
                dayBookRepository.findByEmployeeIdAndDate(transfer.getSender().getId(), transferDay)
                        .map(d -> d.getStatus() == com.dapfintech.employee.enums.DayBookStatus.CLOSED).orElse(false)) {
            transferDay = senderActiveDate;
        }

        LocalDate receiverActiveDate = dayBookService.getActiveDayBookDate(transfer.getReceiver().getId());
        LocalDate receiverDay = transferDay;
        if (receiverDay.isBefore(receiverActiveDate) ||
                dayBookRepository.findByEmployeeIdAndDate(transfer.getReceiver().getId(), receiverDay)
                        .map(d -> d.getStatus() == com.dapfintech.employee.enums.DayBookStatus.CLOSED).orElse(false)) {
            receiverDay = receiverActiveDate;
        }

        final LocalDate finalReceiverDay = receiverDay;
        final LocalDate finalTransferDay = transferDay;

        String senderMktLabel = transfer.getSenderMarket() != null ? "[" + transfer.getSenderMarket().getMarketName() + "] " : "";
        String receiverMktLabel = transfer.getReceiverMarket() != null ? "[" + transfer.getReceiverMarket().getMarketName() + "] " : "";

        // Update DayBook if receiver is an Employee
        if (transfer.getReceiver().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(transfer.getReceiver().getId(), finalReceiverDay)
                    .orElseGet(() -> {
                        dayBookService.getOrCreateDayBook(transfer.getReceiver().getId(), finalReceiverDay);
                        return dayBookRepository.findByEmployeeIdAndDate(transfer.getReceiver().getId(), finalReceiverDay).orElse(null);
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
                rxTx.setEmployeeId(transfer.getReceiver().getId());
                rxTx.setType(txType);
                rxTx.setAmount(transfer.getAmount());
                String remarks = (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH ? "Cash from: " : "Online from: ")
                        + senderMktLabel + transfer.getSender().getFullName()
                        + (transfer.getRemarks() != null && !transfer.getRemarks().isBlank() ? " (" + transfer.getRemarks() + ")" : "");
                rxTx.setRemarks(remarks);
                rxTx.setCreatedAt(transfer.getTransferDate() != null ? transfer.getTransferDate() : LocalDateTime.now());
                dayBookTransactionRepository.save(rxTx);

                if (transfer.getReceiverMarket() != null) {
                    dayBookService.syncMarketDayBook(transfer.getReceiverMarket().getId(), finalReceiverDay);
                }
            }
        }

        // Update DayBook if sender is an Employee
        // CRITICAL FIX: If category is OFFICE_REMITTANCE, it was ALREADY deducted from sender's DayBook
        // under 'officeRemittance' when initiated! DO NOT add a duplicate OUTGOING_TRANSFER!
        if (transfer.getSender().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")
                && !"OFFICE_REMITTANCE".equalsIgnoreCase(transfer.getCategory())) {

            DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(transfer.getSender().getId(), finalTransferDay)
                    .orElseGet(() -> {
                        dayBookService.getOrCreateDayBook(transfer.getSender().getId(), finalTransferDay);
                        return dayBookRepository.findByEmployeeIdAndDate(transfer.getSender().getId(), finalTransferDay).orElse(null);
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
                        + receiverMktLabel + transfer.getReceiver().getFullName()
                        + (transfer.getRemarks() != null && !transfer.getRemarks().isBlank() ? " (" + transfer.getRemarks() + ")" : "");
                txTx.setRemarks(remarks);
                txTx.setCreatedAt(transfer.getTransferDate() != null ? transfer.getTransferDate() : LocalDateTime.now());
                dayBookTransactionRepository.save(txTx);

                if (transfer.getSenderMarket() != null) {
                    dayBookService.syncMarketDayBook(transfer.getSenderMarket().getId(), transferDay);
                }
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

        User currentUser = securityUtils.getCurrentUser();
        boolean isReceiver = transfer.getReceiver().getId().equals(currentUserId);
        boolean isOfficeRemitAdmin = "OFFICE_REMITTANCE".equalsIgnoreCase(transfer.getCategory()) && currentUser != null && currentUser.isAdmin();

        if (!isReceiver && !isOfficeRemitAdmin) {
            throw new RuntimeException("Only the receiver can reject this transfer");
        }

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Transfer is not in PENDING status");
        }

        transfer.setStatus(TransferStatus.REJECTED);
        InternalTransfer saved = internalTransferRepository.save(transfer);

        // If an OFFICE_REMITTANCE is rejected, refund the employee's DayBook
        if ("OFFICE_REMITTANCE".equalsIgnoreCase(transfer.getCategory())) {
            LocalDate transferDay = transfer.getTransferDate() != null
                    ? transfer.getTransferDate().toLocalDate()
                    : LocalDate.now();
            dayBookRepository.findByEmployeeIdAndDate(transfer.getSender().getId(), transferDay).ifPresent(dayBook -> {
                if (dayBook.getOfficeRemittance() != null) {
                    dayBook.setOfficeRemittance(dayBook.getOfficeRemittance().subtract(transfer.getAmount()).max(BigDecimal.ZERO));
                    updateDaybookClosingBalance(dayBook);
                    dayBookRepository.save(dayBook);
                    propagateClosingBalanceForward(dayBook);
                    if (transfer.getSenderMarket() != null) {
                        dayBookService.syncMarketDayBook(transfer.getSenderMarket().getId(), transferDay);
                    }
                }
            });
        }

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
        String senderMkt = t.getSenderMarket() != null ? t.getSenderMarket().getMarketName() : null;
        if (senderMkt == null && t.getSender() != null) {
            senderMkt = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(t.getSender().getId())
                    .map(a -> a.getMarket().getMarketName()).orElse(null);
        }
        String receiverMkt = t.getReceiverMarket() != null ? t.getReceiverMarket().getMarketName() : null;
        if (receiverMkt == null && t.getReceiver() != null) {
            receiverMkt = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(t.getReceiver().getId())
                    .map(a -> a.getMarket().getMarketName()).orElse(null);
        }

        return InternalTransferResponse.builder()
                .id(t.getId())
                .senderId(t.getSender().getId())
                .senderName(t.getSender().getFullName())
                .receiverId(t.getReceiver().getId())
                .receiverName(t.getReceiver().getFullName())
                .senderMarketName(senderMkt)
                .receiverMarketName(receiverMkt)
                .amount(t.getAmount())
                .status(t.getStatus())
                .transferDate(t.getTransferDate())
                .category(t.getCategory())
                .transferMode(t.getTransferMode() != null ? t.getTransferMode().name() : null)
                .remarks(t.getRemarks())
                .build();
    }
}
