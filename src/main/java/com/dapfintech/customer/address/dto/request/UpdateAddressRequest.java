package com.dapfintech.customer.address.dto.request;

import com.dapfintech.customer.enums.AddressType;

import lombok.Data;

@Data
public class UpdateAddressRequest {
	
	private AddressType addressType;
	
	private String addressLine1;
	
	private String addressLine2;
	
	private String city;
	
	private String state;
	
	private String pincode;
	
}
