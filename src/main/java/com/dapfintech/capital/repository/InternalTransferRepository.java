package com.dapfintech.capital.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dapfintech.capital.entity.InternalTransfer;
import com.dapfintech.capital.enums.TransferStatus;

public interface InternalTransferRepository extends JpaRepository<InternalTransfer, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM InternalTransfer t WHERE t.id = :id")
    Optional<InternalTransfer> findByIdForUpdate(@Param("id") UUID id);

    List<InternalTransfer> findByReceiverIdAndStatusOrderByTransferDateDesc(UUID receiverId, TransferStatus status);
    List<InternalTransfer> findByReceiverMarketIdAndStatusOrderByTransferDateDesc(UUID marketId, TransferStatus status);
    List<InternalTransfer> findByReceiverMarketIdInAndStatusOrderByTransferDateDesc(List<UUID> marketIds, TransferStatus status);
    List<InternalTransfer> findBySenderIdOrderByTransferDateDesc(UUID senderId);
    List<InternalTransfer> findByReceiverIdOrderByTransferDateDesc(UUID receiverId);
    List<InternalTransfer> findBySenderMarketIdOrderByTransferDateDesc(UUID marketId);
    List<InternalTransfer> findByReceiverMarketIdOrderByTransferDateDesc(UUID marketId);

    List<InternalTransfer> findByReceiverIdAndStatusAndTransferDateBetween(UUID receiverId, TransferStatus status, LocalDateTime start, LocalDateTime end);
    List<InternalTransfer> findBySenderIdAndStatusAndTransferDateBetween(UUID senderId, TransferStatus status, LocalDateTime start, LocalDateTime end);

    List<InternalTransfer> findByReceiverMarketIdAndStatusAndTransferDateBetween(UUID marketId, TransferStatus status, LocalDateTime start, LocalDateTime end);
    List<InternalTransfer> findBySenderMarketIdAndStatusAndTransferDateBetween(UUID marketId, TransferStatus status, LocalDateTime start, LocalDateTime end);

    // ── Dashboard aggregate queries ──────────────
    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM internal_transfers t
            JOIN users r ON t.receiver_id = r.id
            JOIN roles rr ON r.role_id = rr.id
            WHERE t.status = 'ACCEPTED'
            AND UPPER(rr.role_name) IN ('ADMIN', 'SUPER_ADMIN')
            """, nativeQuery = true)
    BigDecimal getAdminIncomingTransfers();

    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM internal_transfers t
            JOIN users s ON t.sender_id = s.id
            JOIN roles rs ON s.role_id = rs.id
            WHERE t.status = 'ACCEPTED'
            AND UPPER(rs.role_name) IN ('ADMIN', 'SUPER_ADMIN')
            """, nativeQuery = true)
    BigDecimal getAdminOutgoingTransfers();
}
