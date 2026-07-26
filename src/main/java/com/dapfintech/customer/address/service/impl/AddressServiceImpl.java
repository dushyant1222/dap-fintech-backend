package com.dapfintech.customer.address.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dapfintech.customer.address.dto.request.CreateAddressRequest;
import com.dapfintech.customer.address.dto.request.UpdateAddressRequest;
import com.dapfintech.customer.address.dto.response.AddressResponse;
import com.dapfintech.customer.address.mapper.AddressMapper;
import com.dapfintech.customer.address.service.AddressService;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.entity.CustomerAddress;
import com.dapfintech.customer.repository.CustomerAddressRepository;
import com.dapfintech.customer.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
	
	private final CustomerRepository customerRepository;
	private final CustomerAddressRepository addressRepository;
	private final AddressMapper addressMapper;
	
	@Override
	public AddressResponse createAddress(CreateAddressRequest request) {
		System.out.println(
		        "CustomerId = " + request.getCustomerId()
		);
		Customer customer = customerRepository.findById(request.getCustomerId())
				.orElseThrow(() -> new RuntimeException("Customer Not Found"));
		
		CustomerAddress address = CustomerAddress.builder()
				.customer(customer)
				.addressType(request.getAddressType())
				.addressLine1(request.getAddressLine1())
				.addressLine2(request.getAddressLine2())
				.city(request.getCity())
				.state(request.getState())
				.pincode(request.getPincode())
				.build();
		
		return addressMapper.toResponse(addressRepository.save(address));
	}
	
	@Override
	public AddressResponse updateAddress(UUID addressId, UpdateAddressRequest request) {
		CustomerAddress address = addressRepository.findById(addressId).orElseThrow(() -> new RuntimeException("Address Not Found"));
		
		address.setAddressType(request.getAddressType());
		address.setAddressLine1(request.getAddressLine1());
		address.setAddressLine2(request.getAddressLine2());
		address.setCity(request.getCity());
		address.setState(request.getState());
		address.setPincode(request.getPincode());
		
		return addressMapper.toResponse(addressRepository.save(address));
	}
	
	@Override
	public AddressResponse getAddressById(UUID addressId) {
		return addressMapper.toResponse(addressRepository.findById(addressId).orElseThrow(
				() -> new RuntimeException("Address not found")));
	}
	
	@Override
	public List<AddressResponse> getCustomerAddresses(UUID customerId){
		return addressRepository
				.findByCustomerId(customerId)
				.stream()
				.map(addressMapper::toResponse)
				.toList();
	}
	
	@Override
	public void deleteAddress(UUID addressId) {
		addressRepository.deleteById(addressId);
	}
	
}
