package com.dapfintech.market.dto.request;

import lombok.Data;

@Data
public class CreateMarketRequest {
	
	private String marketName;
	private String city;
	private String state;
	private String description;
}
