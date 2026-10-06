package com.dapfintech.capital.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import com.dapfintech.employee.entity.MarketDayBook;
import com.dapfintech.employee.repository.DayBookRepository;
import com.dapfintech.employee.repository.DayBookTransactionRepository;
import com.dapfintech.employee.repository.MarketDayBookRepository;
import com.dapfintech.employee.service.DayBookService;
import com.dapfintech.market.entity.EmployeeMarketAssignment;
import com.dapfintech.market.entity.Market;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
import com.dapfintech.market.repository.MarketRepository;
import com.dapfintech.security.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalTransferServiceImpl implements InternalTransferService {

    private final InternalTransferRepository internalTransferRepository;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final SecurityUtils securityUtils;
    private final DayBookRepository dayBookRepository;
    private final DayBookTransactionRepository dayBookTransactionRepository;
    private final MarketDayBookRepository marketDayBookRepository;
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

        // Resolve Sender Market
        Market senderMarket = null;
        if (request.getSenderMarketId() != null) {
            senderMarket = marketRepository.findById(request.getSenderMarketId()).orElse(null);
        }
        if (senderMarket == null) {
            senderMarket = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(sender.getId())
                    .map(EmployeeMarketAssignment::getMarket).orElse(null);
        }

        // Resolve Receiver & Receiver Market
        Market receiverMarket = null;
        if (request.getReceiverMarketId() != null) {
            receiverMarket = marketRepository.findById(request.getReceiverMarketId()).orElse(null);
        }

        User receiver = null;
        if (request.getReceiverId() != null) {
            receiver = userRepository.findById(request.getReceiverId()).orElse(null);
            if (receiverMarket == null && receiver != null) {
                receiverMarket = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(receiver.getId())
                        .map(EmployeeMarketAssignment::getMarket).orElse(null);
            }
        } else if (receiverMarket != null) {
            // Pick first active employee of the market as receiver reference if available
            List<EmployeeMarketAssignment> assigns = assignmentRepository.findByMarketIdAndIsActiveTrue(receiverMarket.getId());
            if (!assigns.isEmpty()) {
                receiver = assigns.get(0).getEmployee();
            }
        }

        if (receiver == null && receiverMarket == null) {
            throw new RuntimeException("Please select a target market or employee for the transfer.");
        }

        // Calendar Date Resolution: EXACT date selected by user must be preserved!
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
                    txDate = LocalDateTime.now();
                }
            }
        } else {
            txDate = LocalDateTime.now();
        }

        InternalTransfer transfer = InternalTransfer.builder()
                .sender(sender)
                .receiver(receiver)
                .senderMarket(senderMarket)
                .receiverMarket(receiverMarket)
                .amount(request.getAmount())
                .status(TransferStatus.PENDING)
                .transferDate(txDate)
                .category(request.getCategory() != null ? request.getCategory() : "FUNDING")
                .transferMode(request.getTransferMode() != null ? com.dapfintech.capital.enums.TransferMode.valueOf(request.getTransferMode().toUpperCase()) : com.dapfintech.capital.enums.TransferMode.ONLINE)
                .remarks(request.getRemarks())
                .build();

        InternalTransfer saved = internalTransferRepository.save(transfer);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public InternalTransferResponse acceptTransfer(UUID transferId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = securityUtils.getCurrentUser();

        InternalTransfer transfer = internalTransferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Transfer cannot be accepted: it is already " + transfer.getStatus());
        }

        // Authorization check: Receiver user OR employee in receiving market OR admin for office remittance
        boolean isDirectReceiver = transfer.getReceiver() != null && transfer.getReceiver().getId().equals(currentUserId);
        boolean isMarketEmployee = transfer.getReceiverMarket() != null &&
                assignmentRepository.existsByEmployeeIdAndMarketIdAndIsActiveTrue(currentUserId, transfer.getReceiverMarket().getId());
        boolean isOfficeRemitAdmin = "OFFICE_REMITTANCE".equalsIgnoreCase(transfer.getCategory()) && currentUser != null && currentUser.isAdmin();
        boolean isAdmin = currentUser != null && currentUser.isAdmin();

        if (!isDirectReceiver && !isMarketEmployee && !isOfficeRemitAdmin && !isAdmin) {
            throw new RuntimeException("Only members of the receiving market or recipient can accept this transfer.");
        }

        transfer.setStatus(TransferStatus.ACCEPTED);
        InternalTransfer saved = internalTransferRepository.save(transfer);

        // Target Date: STRICTLY respect the transfer date selected during initiation!
        final LocalDate transferDay = transfer.getTransferDate() != null
                ? transfer.getTransferDate().toLocalDate()
                : LocalDate.now();

        String senderMktLabel = transfer.getSenderMarket() != null ? "[" + transfer.getSenderMarket().getMarketName() + "] " : "";
        String receiverMktLabel = transfer.getReceiverMarket() != null ? "[" + transfer.getReceiverMarket().getMarketName() + "] " : "";

        // ── 1. RECEIVER SIDE: Update Market DayBook & DayBook ───────────────
        if (transfer.getReceiverMarket() != null) {
            UUID rxMarketId = transfer.getReceiverMarket().getId();
            MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(rxMarketId, transferDay)
                    .orElseGet(() -> {
                        MarketDayBook newMdb = new MarketDayBook();
                        newMdb.setMarketId(rxMarketId);
                        newMdb.setDate(transferDay);
                        newMdb.setStatus(com.dapfintech.employee.enums.DayBookStatus.OPEN);
                        return marketDayBookRepository.save(newMdb);
                    });

            String txType;
            if (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH) {
                if (mdb.getTotalCashIncomingTransfers() == null) mdb.setTotalCashIncomingTransfers(BigDecimal.ZERO);
                mdb.setTotalCashIncomingTransfers(mdb.getTotalCashIncomingTransfers().add(transfer.getAmount()));
                txType = "CASH_INCOMING_TRANSFER";
            } else {
                if (mdb.getTotalIncomingTransfers() == null) mdb.setTotalIncomingTransfers(BigDecimal.ZERO);
                mdb.setTotalIncomingTransfers(mdb.getTotalIncomingTransfers().add(transfer.getAmount()));
                txType = "INCOMING_TRANSFER";
            }
            updateMarketClosingBalance(mdb);
            marketDayBookRepository.save(mdb);
            propagateMarketClosingBalanceForward(mdb);

            // Record transaction for the market
            DayBookTransaction rxTx = new DayBookTransaction();
            rxTx.setMarketId(rxMarketId);
            rxTx.setMarketDayBook(mdb);
            rxTx.setEmployeeId(currentUserId);
            rxTx.setType(txType);
            rxTx.setAmount(transfer.getAmount());
            String rxRemarks = (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH ? "Cash from: " : "Online from: ")
                    + senderMktLabel + (transfer.getSender() != null ? transfer.getSender().getFullName() : "Admin")
                    + (transfer.getRemarks() != null && !transfer.getRemarks().isBlank() ? " (" + transfer.getRemarks() + ")" : "");
            rxTx.setRemarks(rxRemarks);
            rxTx.setCreatedAt(transfer.getTransferDate() != null ? transfer.getTransferDate() : LocalDateTime.now());
            dayBookTransactionRepository.save(rxTx);
        }

        // Also update receiver employee DayBook if direct receiver is set
        if (transfer.getReceiver() != null && transfer.getReceiver().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(transfer.getReceiver().getId(), transferDay)
                    .orElseGet(() -> {
                        DayBook newDb = new DayBook();
                        newDb.setEmployeeId(transfer.getReceiver().getId());
                        newDb.setDate(transferDay);
                        return dayBookRepository.save(newDb);
                    });
            if (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH) {
                if (dayBook.getCashIncomingTransfers() == null) dayBook.setCashIncomingTransfers(BigDecimal.ZERO);
                dayBook.setCashIncomingTransfers(dayBook.getCashIncomingTransfers().add(transfer.getAmount()));
            } else {
                if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(BigDecimal.ZERO);
                dayBook.setIncomingTransfers(dayBook.getIncomingTransfers().add(transfer.getAmount()));
            }
            updateDaybookClosingBalance(dayBook);
            dayBookRepository.save(dayBook);
            propagateClosingBalanceForward(dayBook);
        }

        // ── 2. SENDER SIDE: Update Market DayBook & DayBook (if not Office Remittance) ───
        if (!"OFFICE_REMITTANCE".equalsIgnoreCase(transfer.getCategory())) {
            if (transfer.getSenderMarket() != null) {
                UUID txMarketId = transfer.getSenderMarket().getId();
                MarketDayBook mdb = marketDayBookRepository.findByMarketIdAndDate(txMarketId, transferDay)
                        .orElseGet(() -> {
                            MarketDayBook newMdb = new MarketDayBook();
                            newMdb.setMarketId(txMarketId);
                            newMdb.setDate(transferDay);
                            newMdb.setStatus(com.dapfintech.employee.enums.DayBookStatus.OPEN);
                            return marketDayBookRepository.save(newMdb);
                        });

                String txType;
                if (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH) {
                    if (mdb.getTotalCashOutgoingTransfers() == null) mdb.setTotalCashOutgoingTransfers(BigDecimal.ZERO);
                    mdb.setTotalCashOutgoingTransfers(mdb.getTotalCashOutgoingTransfers().add(transfer.getAmount()));
                    txType = "CASH_OUTGOING_TRANSFER";
                } else {
                    if (mdb.getTotalOutgoingTransfers() == null) mdb.setTotalOutgoingTransfers(BigDecimal.ZERO);
                    mdb.setTotalOutgoingTransfers(mdb.getTotalOutgoingTransfers().add(transfer.getAmount()));
                    txType = "OUTGOING_TRANSFER";
                }
                updateMarketClosingBalance(mdb);
                marketDayBookRepository.save(mdb);
                propagateMarketClosingBalanceForward(mdb);

                DayBookTransaction txTx = new DayBookTransaction();
                txTx.setMarketId(txMarketId);
                txTx.setMarketDayBook(mdb);
                txTx.setEmployeeId(transfer.getSender().getId());
                txTx.setType(txType);
                txTx.setAmount(transfer.getAmount());
                String txRemarks = (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH ? "Cash to: " : "Online to: ")
                        + receiverMktLabel + (transfer.getReceiver() != null ? transfer.getReceiver().getFullName() : "Market")
                        + (transfer.getRemarks() != null && !transfer.getRemarks().isBlank() ? " (" + transfer.getRemarks() + ")" : "");
                txTx.setRemarks(txRemarks);
                txTx.setCreatedAt(transfer.getTransferDate() != null ? transfer.getTransferDate() : LocalDateTime.now());
                dayBookTransactionRepository.save(txTx);
            }

            if (transfer.getSender().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
                DayBook dayBook = dayBookRepository.findByEmployeeIdAndDate(transfer.getSender().getId(), transferDay)
                        .orElseGet(() -> {
                            DayBook newDb = new DayBook();
                            newDb.setEmployeeId(transfer.getSender().getId());
                            newDb.setDate(transferDay);
                            return dayBookRepository.save(newDb);
                        });
                if (transfer.getTransferMode() == com.dapfintech.capital.enums.TransferMode.CASH) {
                    if (dayBook.getCashOutgoingTransfers() == null) dayBook.setCashOutgoingTransfers(BigDecimal.ZERO);
                    dayBook.setCashOutgoingTransfers(dayBook.getCashOutgoingTransfers().add(transfer.getAmount()));
                } else {
                    if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(BigDecimal.ZERO);
                    dayBook.setOutgoingTransfers(dayBook.getOutgoingTransfers().add(transfer.getAmount()));
                }
                updateDaybookClosingBalance(dayBook);
                dayBookRepository.save(dayBook);
                propagateClosingBalanceForward(dayBook);
            }
        }

        return mapToResponse(saved);
    }

    private void updateMarketClosingBalance(MarketDayBook m) {
        if (m.getTotalOpeningBalance() == null) m.setTotalOpeningBalance(BigDecimal.ZERO);
        if (m.getTotalCollections() == null) m.setTotalCollections(BigDecimal.ZERO);
        if (m.getTotalIncomingTransfers() == null) m.setTotalIncomingTransfers(BigDecimal.ZERO);
        if (m.getTotalCashIncomingTransfers() == null) m.setTotalCashIncomingTransfers(BigDecimal.ZERO);
        if (m.getTotalSpends() == null) m.setTotalSpends(BigDecimal.ZERO);
        if (m.getTotalLoansDisbursed() == null) m.setTotalLoansDisbursed(BigDecimal.ZERO);
        if (m.getTotalOutgoingTransfers() == null) m.setTotalOutgoingTransfers(BigDecimal.ZERO);
        if (m.getTotalCashOutgoingTransfers() == null) m.setTotalCashOutgoingTransfers(BigDecimal.ZERO);
        if (m.getTotalOfficeRemittance() == null) m.setTotalOfficeRemittance(BigDecimal.ZERO);
        if (m.getTotalTransferToOthers() == null) m.setTotalTransferToOthers(BigDecimal.ZERO);

        BigDecimal c = m.getTotalOpeningBalance()
                .add(m.getTotalCollections())
                .add(m.getTotalIncomingTransfers())
                .add(m.getTotalCashIncomingTransfers())
                .subtract(m.getTotalSpends())
                .subtract(m.getTotalLoansDisbursed())
                .subtract(m.getTotalOutgoingTransfers())
                .subtract(m.getTotalCashOutgoingTransfers())
                .subtract(m.getTotalOfficeRemittance())
                .subtract(m.getTotalTransferToOthers());
        m.setTotalClosingBalance(c);
    }

    private void propagateMarketClosingBalanceForward(MarketDayBook mdb) {
        if (mdb == null || mdb.getDate() == null || mdb.getMarketId() == null) return;
        UUID marketId = mdb.getMarketId();
        LocalDate currDate = mdb.getDate();
        BigDecimal prevClosing = mdb.getTotalClosingBalance();

        List<MarketDayBook> subsequent = marketDayBookRepository.findByMarketIdOrderByDateAsc(marketId);
        for (MarketDayBook nextMdb : subsequent) {
            if (nextMdb.getDate().isAfter(currDate)) {
                nextMdb.setTotalOpeningBalance(prevClosing);
                updateMarketClosingBalance(nextMdb);
                marketDayBookRepository.save(nextMdb);
                prevClosing = nextMdb.getTotalClosingBalance();
            }
        }
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
        if (dayBook.getTransferToOthers() == null) dayBook.setTransferToOthers(BigDecimal.ZERO);

        BigDecimal newClosing = dayBook.getOpeningBalance()
                .add(dayBook.getCollections())
                .add(dayBook.getIncomingTransfers())
                .add(dayBook.getCashIncomingTransfers())
                .subtract(dayBook.getSpends())
                .subtract(dayBook.getLoansDisbursed())
                .subtract(dayBook.getOutgoingTransfers())
                .subtract(dayBook.getCashOutgoingTransfers())
                .subtract(dayBook.getOfficeRemittance())
                .subtract(dayBook.getTransferToOthers());
        dayBook.setClosingBalance(newClosing);
    }

    @Override
    @Transactional
    public InternalTransferResponse rejectTransfer(UUID transferId) {
        UUID currentUserId = securityUtils.getCurrentUserId();

        InternalTransfer transfer = internalTransferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));

        User currentUser = securityUtils.getCurrentUser();
        boolean isReceiver = transfer.getReceiver() != null && transfer.getReceiver().getId().equals(currentUserId);
        boolean isMarketEmployee = transfer.getReceiverMarket() != null &&
                assignmentRepository.existsByEmployeeIdAndMarketIdAndIsActiveTrue(currentUserId, transfer.getReceiverMarket().getId());
        boolean isOfficeRemitAdmin = "OFFICE_REMITTANCE".equalsIgnoreCase(transfer.getCategory()) && currentUser != null && currentUser.isAdmin();

        if (!isReceiver && !isMarketEmployee && !isOfficeRemitAdmin && !(currentUser != null && currentUser.isAdmin())) {
            throw new RuntimeException("Only the recipient or market members can reject this transfer");
        }

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Transfer cannot be rejected: it is already " + transfer.getStatus());
        }

        transfer.setStatus(TransferStatus.REJECTED);
        InternalTransfer saved = internalTransferRepository.save(transfer);

        // If an OFFICE_REMITTANCE is rejected, refund the market & employee DayBook
        if ("OFFICE_REMITTANCE".equalsIgnoreCase(transfer.getCategory())) {
            LocalDate transferDay = transfer.getTransferDate() != null
                    ? transfer.getTransferDate().toLocalDate()
                    : LocalDate.now();

            if (transfer.getSenderMarket() != null) {
                marketDayBookRepository.findByMarketIdAndDate(transfer.getSenderMarket().getId(), transferDay).ifPresent(mdb -> {
                    if (mdb.getTotalOfficeRemittance() != null) {
                        mdb.setTotalOfficeRemittance(mdb.getTotalOfficeRemittance().subtract(transfer.getAmount()).max(BigDecimal.ZERO));
                        updateMarketClosingBalance(mdb);
                        marketDayBookRepository.save(mdb);
                        propagateMarketClosingBalanceForward(mdb);
                    }
                });
            }

            if (transfer.getSender() != null) {
                dayBookRepository.findByEmployeeIdAndDate(transfer.getSender().getId(), transferDay).ifPresent(dayBook -> {
                    if (dayBook.getOfficeRemittance() != null) {
                        dayBook.setOfficeRemittance(dayBook.getOfficeRemittance().subtract(transfer.getAmount()).max(BigDecimal.ZERO));
                        updateDaybookClosingBalance(dayBook);
                        dayBookRepository.save(dayBook);
                        propagateClosingBalanceForward(dayBook);
                    }
                });
            }
        }

        return mapToResponse(saved);
    }

    @Override
    public List<InternalTransferResponse> getPendingIncomingTransfers() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = securityUtils.getCurrentUser();

        Set<InternalTransfer> result = new HashSet<>();

        // 1. Direct transfers to user
        result.addAll(internalTransferRepository.findByReceiverIdAndStatusOrderByTransferDateDesc(currentUserId, TransferStatus.PENDING));

        // 2. Transfers to user's assigned markets
        List<EmployeeMarketAssignment> assignments = assignmentRepository.findByEmployeeIdAndIsActiveTrue(currentUserId);
        for (EmployeeMarketAssignment asg : assignments) {
            if (asg.getMarket() != null) {
                result.addAll(internalTransferRepository.findByReceiverMarketIdAndStatusOrderByTransferDateDesc(asg.getMarket().getId(), TransferStatus.PENDING));
            }
        }

        // 3. If admin, also find office remittance pending transfers
        if (currentUser != null && currentUser.isAdmin()) {
            List<InternalTransfer> allPending = internalTransferRepository.findAll().stream()
                    .filter(t -> t.getStatus() == TransferStatus.PENDING && "OFFICE_REMITTANCE".equalsIgnoreCase(t.getCategory()))
                    .collect(Collectors.toList());
            result.addAll(allPending);
        }

        List<InternalTransfer> list = new ArrayList<>(result);
        list.sort((a, b) -> b.getTransferDate().compareTo(a.getTransferDate()));
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<InternalTransferResponse> getMyIncomingTransfers() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Set<InternalTransfer> result = new HashSet<>(internalTransferRepository.findByReceiverIdOrderByTransferDateDesc(currentUserId));

        List<EmployeeMarketAssignment> assignments = assignmentRepository.findByEmployeeIdAndIsActiveTrue(currentUserId);
        for (EmployeeMarketAssignment asg : assignments) {
            if (asg.getMarket() != null) {
                result.addAll(internalTransferRepository.findByReceiverMarketIdOrderByTransferDateDesc(asg.getMarket().getId()));
            }
        }

        List<InternalTransfer> list = new ArrayList<>(result);
        list.sort((a, b) -> b.getTransferDate().compareTo(a.getTransferDate()));
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<InternalTransferResponse> getMyOutgoingTransfers() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        Set<InternalTransfer> result = new HashSet<>(internalTransferRepository.findBySenderIdOrderByTransferDateDesc(currentUserId));

        List<EmployeeMarketAssignment> assignments = assignmentRepository.findByEmployeeIdAndIsActiveTrue(currentUserId);
        for (EmployeeMarketAssignment asg : assignments) {
            if (asg.getMarket() != null) {
                result.addAll(internalTransferRepository.findBySenderMarketIdOrderByTransferDateDesc(asg.getMarket().getId()));
            }
        }

        List<InternalTransfer> list = new ArrayList<>(result);
        list.sort((a, b) -> b.getTransferDate().compareTo(a.getTransferDate()));
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private InternalTransferResponse mapToResponse(InternalTransfer t) {
        String senderMkt = t.getSenderMarket() != null ? t.getSenderMarket().getMarketName() : null;
        UUID senderMktId = t.getSenderMarket() != null ? t.getSenderMarket().getId() : null;
        if (senderMkt == null && t.getSender() != null) {
            EmployeeMarketAssignment asg = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(t.getSender().getId()).orElse(null);
            if (asg != null && asg.getMarket() != null) {
                senderMkt = asg.getMarket().getMarketName();
                senderMktId = asg.getMarket().getId();
            }
        }

        String receiverMkt = t.getReceiverMarket() != null ? t.getReceiverMarket().getMarketName() : null;
        UUID receiverMktId = t.getReceiverMarket() != null ? t.getReceiverMarket().getId() : null;
        if (receiverMkt == null && t.getReceiver() != null) {
            EmployeeMarketAssignment asg = assignmentRepository.findFirstByEmployeeIdAndIsActiveTrue(t.getReceiver().getId()).orElse(null);
            if (asg != null && asg.getMarket() != null) {
                receiverMkt = asg.getMarket().getMarketName();
                receiverMktId = asg.getMarket().getId();
            }
        }

        return InternalTransferResponse.builder()
                .id(t.getId())
                .senderId(t.getSender() != null ? t.getSender().getId() : null)
                .senderName(t.getSender() != null ? t.getSender().getFullName() : (senderMkt != null ? senderMkt : "Admin"))
                .receiverId(t.getReceiver() != null ? t.getReceiver().getId() : null)
                .receiverName(t.getReceiver() != null ? t.getReceiver().getFullName() : (receiverMkt != null ? receiverMkt : "Admin"))
                .senderMarketId(senderMktId)
                .senderMarketName(senderMkt)
                .receiverMarketId(receiverMktId)
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
