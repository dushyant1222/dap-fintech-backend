package com.dapfintech.customer.address.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.customer.address.dto.response.AddressResponse;
import com.dapfintech.customer.entity.CustomerAddress;

@Component
public class AddressMapper {
	
	public AddressResponse toResponse(CustomerAddress address) {
		
		return AddressResponse.builder()
				.id(address.getId())
				.customerId(address.getCustomer().getId())
				.addressType(address.getAddressType())
				.addressLine1(address.getAddressLine1())
				.addressLine2(address.getAddressLine2())
				.city(address.getCity())
				.state(address.getState())
				.pincode(address.getPincode())
				.build();
	}
	
}
