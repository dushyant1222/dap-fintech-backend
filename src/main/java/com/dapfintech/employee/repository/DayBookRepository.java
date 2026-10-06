package com.dapfintech.employee.repository;

import com.dapfintech.employee.entity.DayBook;
import com.dapfintech.employee.enums.DayBookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DayBookRepository extends JpaRepository<DayBook, UUID> {
    @Query("SELECT d FROM DayBook d WHERE d.employeeId = :employeeId AND d.date = :date ORDER BY d.updatedAt DESC NULLS LAST, d.createdAt DESC NULLS LAST, d.id DESC")
    List<DayBook> findAllByEmployeeIdAndDate(@Param("employeeId") UUID employeeId, @Param("date") LocalDate date);

    default Optional<DayBook> findByEmployeeIdAndDate(UUID employeeId, LocalDate date) {
        List<DayBook> list = findAllByEmployeeIdAndDate(employeeId, date);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<DayBook> findByEmployeeIdOrderByDateDesc(UUID employeeId);
    List<DayBook> findByEmployeeIdOrderByDateAsc(UUID employeeId);
    List<DayBook> findByEmployeeIdAndStatusOrderByDateDesc(UUID employeeId, DayBookStatus status);
    List<DayBook> findByEmployeeIdAndStatus(UUID employeeId, DayBookStatus status);
    List<DayBook> findByDateAndStatusNot(LocalDate date, DayBookStatus status);
}
