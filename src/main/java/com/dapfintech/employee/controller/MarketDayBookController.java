package com.dapfintech.employee.controller;

import com.dapfintech.employee.dto.MarketDayBookResponse;
import com.dapfintech.employee.entity.DayBookTransaction;
import com.dapfintech.employee.service.DayBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/markets/{marketId}/daybooks")
public class MarketDayBookController {

    @Autowired
    private DayBookService dayBookService;

    @GetMapping("/today")
    public ResponseEntity<MarketDayBookResponse> getTodayMarketDayBook(@PathVariable UUID marketId) {
        MarketDayBookResponse response = dayBookService.getTodayMarketDayBook(marketId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-date")
    public ResponseEntity<MarketDayBookResponse> getMarketDayBookByDate(
            @PathVariable UUID marketId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MarketDayBookResponse response = dayBookService.getOrCreateMarketDayBook(marketId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<DayBookTransaction>> getMarketTransactions(
            @PathVariable UUID marketId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DayBookTransaction> list = dayBookService.getMarketTransactions(marketId, date);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/approve")
    public ResponseEntity<MarketDayBookResponse> approveMarketClosure(
            @PathVariable UUID marketId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MarketDayBookResponse response = dayBookService.approveMarketClosure(marketId, date);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/reject")
    public ResponseEntity<MarketDayBookResponse> rejectMarketClosure(
            @PathVariable UUID marketId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MarketDayBookResponse response = dayBookService.rejectMarketClosure(marketId, date);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/reopen")
    public ResponseEntity<MarketDayBookResponse> reopenMarketDayBook(
            @PathVariable UUID marketId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MarketDayBookResponse response = dayBookService.reopenMarketDayBook(marketId, date);
        return ResponseEntity.ok(response);
    }
}
