package com.dapfintech.market.dto.response;

import java.util.UUID;

import com.dapfintech.market.enums.MarketStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarketResponse {
	private UUID id;
	private String marketCode;
	private String marketName;
	private String city;
	private String state;
	private String description;
	private MarketStatus status;
}
