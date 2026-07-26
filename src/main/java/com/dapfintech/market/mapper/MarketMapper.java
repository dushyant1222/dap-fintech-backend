package com.dapfintech.market.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.market.dto.response.MarketResponse;
import com.dapfintech.market.entity.Market;

@Component
public class MarketMapper {
	
	public MarketResponse toResponse(Market market) {
		
		return MarketResponse.builder()
				.id(market.getId())
				.marketCode(market.getMarketCode())
				.marketName(market.getMarketName())
				.city(market.getCity())
				.state(market.getState())
				.description(market.getDescription())
				.status(market.getStatus())
				.build();
	}
	
}
