package com.dapfintech.employee.repository;

import com.dapfintech.employee.entity.DayBookTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

public interface DayBookTransactionRepository extends JpaRepository<DayBookTransaction, UUID> {
    List<DayBookTransaction> findByEmployeeIdAndCreatedAtBetween(UUID employeeId, LocalDateTime start, LocalDateTime end);
}
