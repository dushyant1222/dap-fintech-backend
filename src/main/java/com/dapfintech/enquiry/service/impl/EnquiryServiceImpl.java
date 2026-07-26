package com.dapfintech.enquiry.service.impl;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.enquiry.dto.EnquiryRequest;
import com.dapfintech.enquiry.dto.EnquiryResponse;
import com.dapfintech.enquiry.dto.EnquiryStatusUpdateRequest;
import com.dapfintech.enquiry.entity.Enquiry;
import com.dapfintech.enquiry.entity.EnquiryHistory;
import com.dapfintech.enquiry.enums.EnquiryStatus;
import com.dapfintech.enquiry.mapper.EnquiryMapper;
import com.dapfintech.enquiry.repository.EnquiryRepository;
import com.dapfintech.enquiry.service.EnquiryService;
import com.dapfintech.exception.ResourceNotFoundException;
import com.dapfintech.market.entity.Market;
import com.dapfintech.market.repository.MarketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnquiryServiceImpl implements EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final EnquiryMapper enquiryMapper;

    @Override
    @Transactional
    public EnquiryResponse createEnquiry(EnquiryRequest request, UUID employeeId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Market market = marketRepository.findById(request.getMarketId())
                .orElseThrow(() -> new ResourceNotFoundException("Market not found"));

        Enquiry enquiry = enquiryMapper.toEntity(request);
        enquiry.setEmployee(employee);
        enquiry.setMarket(market);
        enquiry.setStatus(EnquiryStatus.NEW);

        EnquiryHistory history = EnquiryHistory.builder()
                .newStatus(EnquiryStatus.NEW)
                .remarks("Enquiry created via field visit")
                .actionBy(employee)
                .build();
        
        enquiry.addHistory(history);

        Enquiry savedEnquiry = enquiryRepository.save(enquiry);
        return enquiryMapper.toResponse(savedEnquiry);
    }

    @Override
    @Transactional(readOnly = true)
    public EnquiryResponse getEnquiryById(UUID id) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));
        return enquiryMapper.toResponse(enquiry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnquiryResponse> getAllEnquiries(Pageable pageable) {
        return enquiryRepository.findAll(pageable).map(enquiryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnquiryResponse> getEnquiriesByEmployee(UUID employeeId, Pageable pageable) {
        return enquiryRepository.findByEmployeeId(employeeId, pageable).map(enquiryMapper::toResponse);
    }

    @Override
    @Transactional
    public EnquiryResponse updateEnquiryStatus(UUID id, EnquiryStatusUpdateRequest request, UUID actionById) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));

        User actionBy = userRepository.findById(actionById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EnquiryStatus oldStatus = enquiry.getStatus();
        enquiry.setStatus(request.getStatus());

        EnquiryHistory history = EnquiryHistory.builder()
                .previousStatus(oldStatus)
                .newStatus(request.getStatus())
                .remarks(request.getRemarks())
                .actionBy(actionBy)
                .build();

        enquiry.addHistory(history);

        Enquiry savedEnquiry = enquiryRepository.save(enquiry);
        return enquiryMapper.toResponse(savedEnquiry);
    }
}