package com.dapfintech.customer.address.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.customer.address.dto.request.CreateAddressRequest;
import com.dapfintech.customer.address.dto.request.UpdateAddressRequest;
import com.dapfintech.customer.address.dto.response.AddressResponse;
import com.dapfintech.customer.address.service.AddressService;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customer-addresses")
@RequiredArgsConstructor
public class AddressController {
	
	private final AddressService addressService;
	
	@PostMapping
	public ResponseEntity<AddressResponse> createAddress(@RequestBody CreateAddressRequest request){
		return ResponseEntity.ok(addressService.createAddress(request));
	}
	
	@PutMapping("/{addressId}")
	public ResponseEntity<AddressResponse> updateAddress(@PathVariable UUID addressId, @RequestBody UpdateAddressRequest request){
		return ResponseEntity.ok(addressService.updateAddress(addressId, request));
	}
	
	@GetMapping("/{addressId}")
	public ResponseEntity<AddressResponse> getAddressById(@PathVariable UUID addressId){
		return ResponseEntity.ok(addressService.getAddressById(addressId));
	}
	 @GetMapping("/customer/{customerId}")
	    public ResponseEntity<List<AddressResponse>>
	    getCustomerAddresses(
	            @PathVariable UUID customerId
	    ) {

	        return ResponseEntity.ok(
	                addressService.getCustomerAddresses(
	                        customerId
	                )
	        );
	    }

	    @DeleteMapping("/{addressId}")
	    public ResponseEntity<String> deleteAddress(
	            @PathVariable UUID addressId
	    ) {

	        addressService.deleteAddress(addressId);

	        return ResponseEntity.ok(
	                "Address deleted successfully"
	        );
	    }
}
