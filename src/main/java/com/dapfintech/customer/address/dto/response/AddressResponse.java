package com.dapfintech.customer.address.dto.response;

import java.util.UUID;

import com.dapfintech.customer.enums.AddressType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {
	
	private UUID id;
	
	private UUID customerId;
	
	private AddressType addressType;
	
	private String addressLine1;
	
	private String addressLine2;
	
	private String city;
	
	private String state;
	
	private String pincode;
}
