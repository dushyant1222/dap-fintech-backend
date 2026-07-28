package com.dapfintech.wallet.repository;

import com.dapfintech.wallet.entity.PersonalLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PersonalLedgerRepository extends JpaRepository<PersonalLedger, Long> {
    List<PersonalLedger> findByAdminIdOrderByTransactionDateDesc(UUID adminId);
}
