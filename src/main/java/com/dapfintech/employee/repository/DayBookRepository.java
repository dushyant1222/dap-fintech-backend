package com.dapfintech.employee.repository;

import com.dapfintech.employee.entity.DayBook;
import com.dapfintech.employee.enums.DayBookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DayBookRepository extends JpaRepository<DayBook, UUID> {
    Optional<DayBook> findByEmployeeIdAndDate(UUID employeeId, LocalDate date);
    List<DayBook> findByEmployeeIdOrderByDateDesc(UUID employeeId);
    List<DayBook> findByEmployeeIdOrderByDateAsc(UUID employeeId);
    List<DayBook> findByEmployeeIdAndStatusOrderByDateDesc(UUID employeeId, DayBookStatus status);
    List<DayBook> findByEmployeeIdAndStatus(UUID employeeId, DayBookStatus status);
    List<DayBook> findByDateAndStatusNot(LocalDate date, DayBookStatus status);
}
