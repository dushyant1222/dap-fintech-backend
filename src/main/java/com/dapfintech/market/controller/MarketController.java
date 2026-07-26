package com.dapfintech.market.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.market.dto.request.CreateMarketRequest;
import com.dapfintech.market.dto.request.UpdateMarketRequest;
import com.dapfintech.market.dto.response.MarketDashboardResponse;
import com.dapfintech.market.dto.response.MarketDetailsResponse;
import com.dapfintech.market.dto.response.MarketResponse;
import com.dapfintech.market.service.MarketService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/markets")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    
    @GetMapping("/{marketId}/dashboard")
    public ResponseEntity<MarketDashboardResponse>
    getDashboard(
            @PathVariable UUID marketId
    ){

        return ResponseEntity.ok(

                marketService.getDashboard(
                        marketId
                )

        );
    }

    // =====================================================
    // CREATE MARKET
    // =====================================================

    @PostMapping
    public ResponseEntity<
            ApiResponse<MarketResponse>>
    createMarket(
            @RequestBody
            CreateMarketRequest request
    ) {

        MarketResponse response =
                marketService.createMarket(
                        request
                );

        return ResponseEntity.ok(

                ApiResponse
                        .<MarketResponse>builder()
                        .success(true)
                        .message(
                                "Market created successfully"
                        )
                        .data(response)
                        .build()
        );
    }

    // =====================================================
    // GET ALL MARKETS
    // =====================================================

    @GetMapping
    public ResponseEntity<
            ApiResponse<List<MarketResponse>>>
    getAllMarkets() {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<MarketResponse>>builder()
                        .success(true)
                        .message(
                                "Markets fetched successfully"
                        )
                        .data(
                                marketService
                                        .getAllMarkets()
                        )
                        .build()
        );
    }

    // =====================================================
    // GET ACTIVE MARKETS
    // =====================================================

    @GetMapping("/active")
    public ResponseEntity<
            ApiResponse<List<MarketResponse>>>
    getActiveMarkets() {

        return ResponseEntity.ok(

                ApiResponse
                        .<List<MarketResponse>>builder()
                        .success(true)
                        .message(
                                "Active markets fetched successfully"
                        )
                        .data(
                                marketService
                                        .getActiveMarkets()
                        )
                        .build()
        );
    }

    // =====================================================
    // GET MARKET BY ID
    // =====================================================

    @GetMapping("/{marketId}")

    public ResponseEntity<ApiResponse<MarketDetailsResponse>>
    getMarketDetails(
            @PathVariable UUID marketId
    ){

        return ResponseEntity.ok(

                ApiResponse
                        .<MarketDetailsResponse>builder()

                        .success(true)

                        .message("Market details fetched successfully")

                        .data(
                                marketService.getMarketDetails(
                                        marketId
                                )
                        )

                        .build()

        );

    }

    // =====================================================
    // UPDATE MARKET
    // =====================================================

    @PutMapping("/{marketId}")
    public ResponseEntity<
            ApiResponse<MarketResponse>>
    updateMarket(
            @PathVariable UUID marketId,
            @RequestBody
            UpdateMarketRequest request
    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<MarketResponse>builder()
                        .success(true)
                        .message(
                                "Market updated successfully"
                        )
                        .data(
                                marketService
                                        .updateMarket(
                                                marketId,
                                                request
                                        )
                        )
                        .build()
        );
    }

    // =====================================================
    // ACTIVATE MARKET
    // =====================================================

    @PatchMapping("/{marketId}/activate")
    public ResponseEntity<
            ApiResponse<MarketResponse>>
    activateMarket(
            @PathVariable UUID marketId
    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<MarketResponse>builder()
                        .success(true)
                        .message(
                                "Market activated successfully"
                        )
                        .data(
                                marketService
                                        .activateMarket(
                                                marketId
                                        )
                        )
                        .build()
        );
    }

    // =====================================================
    // DEACTIVATE MARKET
    // =====================================================

    @PatchMapping("/{marketId}/deactivate")
    public ResponseEntity<
            ApiResponse<MarketResponse>>
    deactivateMarket(
            @PathVariable UUID marketId
    ) {

        return ResponseEntity.ok(

                ApiResponse
                        .<MarketResponse>builder()
                        .success(true)
                        .message(
                                "Market deactivated successfully"
                        )
                        .data(
                                marketService
                                        .deactivateMarket(
                                                marketId
                                        )
                        )
                        .build()
        );
    }

    // =====================================================
    // DELETE MARKET
    // =====================================================

    @DeleteMapping("/{marketId}")
    public ResponseEntity<
            ApiResponse<String>>
    deleteMarket(
            @PathVariable UUID marketId
    ) {

        marketService.deleteMarket(
                marketId
        );

        return ResponseEntity.ok(

                ApiResponse
                        .<String>builder()
                        .success(true)
                        .message(
                                "Market deleted successfully"
                        )
                        .data("SUCCESS")
                        .build()
        );
    }
}