package com.dapfintech.customer.address.dto.request;

import java.util.UUID;

import com.dapfintech.customer.enums.AddressType;

import lombok.Data;

@Data
public class CreateAddressRequest {
	
	private UUID customerId;
	
	private AddressType addressType;
	
	private String addressLine1;
	
	private String addressLine2;
	
	private String city;
	
	private String state;
	
	private String pincode;
}
