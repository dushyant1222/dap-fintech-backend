package com.dapfintech.customer.address.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dapfintech.customer.address.dto.request.CreateAddressRequest;
import com.dapfintech.customer.address.dto.request.UpdateAddressRequest;
import com.dapfintech.customer.address.dto.response.AddressResponse;

public interface AddressService {
	
	AddressResponse createAddress(CreateAddressRequest request);
	
	AddressResponse updateAddress(UUID addressId, UpdateAddressRequest request);
	
	AddressResponse getAddressById(UUID addressId);
	
	List<AddressResponse> getCustomerAddresses(UUID customerId);
	
	void deleteAddress(UUID addressId);
}