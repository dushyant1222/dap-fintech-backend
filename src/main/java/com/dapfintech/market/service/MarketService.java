package com.dapfintech.market.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.market.dto.request.CreateMarketRequest;
import com.dapfintech.market.dto.request.UpdateMarketRequest;
import com.dapfintech.market.dto.response.MarketResponse;
import com.dapfintech.market.dto.response.MarketDashboardResponse;
import com.dapfintech.market.dto.response.MarketDetailsResponse;

public interface MarketService {

    MarketResponse createMarket(
            CreateMarketRequest request
    );

    MarketResponse updateMarket(
            UUID marketId,
            UpdateMarketRequest request
    );

    MarketDetailsResponse getMarketDetails(UUID id);

    List<MarketResponse> getAllMarkets();

    List<MarketResponse> getActiveMarkets();
    MarketDashboardResponse
    getDashboard(UUID marketId);

    MarketResponse activateMarket(
            UUID marketId
    );

    MarketResponse deactivateMarket(
            UUID marketId
    );

    void deleteMarket(
            UUID marketId
    );
}