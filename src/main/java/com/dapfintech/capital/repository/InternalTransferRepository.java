package com.dapfintech.capital.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dapfintech.capital.entity.InternalTransfer;
import com.dapfintech.capital.enums.TransferStatus;

public interface InternalTransferRepository extends JpaRepository<InternalTransfer, UUID> {
    List<InternalTransfer> findByReceiverIdAndStatusOrderByTransferDateDesc(UUID receiverId, TransferStatus status);
    List<InternalTransfer> findBySenderIdOrderByTransferDateDesc(UUID senderId);
    List<InternalTransfer> findByReceiverIdOrderByTransferDateDesc(UUID receiverId);
}
