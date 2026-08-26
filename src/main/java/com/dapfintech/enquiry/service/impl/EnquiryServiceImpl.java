package com.dapfintech.enquiry.service.impl;

import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.customer.dto.request.CreateCustomerRequest;
import com.dapfintech.customer.dto.response.CustomerResponse;
import com.dapfintech.customer.enums.CustomerStatus;
import com.dapfintech.customer.enums.Gender;
import com.dapfintech.customer.service.CustomerService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import com.dapfintech.notification.service.NotificationService;

@Service
@RequiredArgsConstructor
public class EnquiryServiceImpl implements EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final EnquiryMapper enquiryMapper;
    private final CustomerService customerService;
    private final com.dapfintech.customer.repository.CustomerRepository customerRepository;
    private final NotificationService notificationService;

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
        
        // Notify all ADMIN users
        String title = "New Enquiry Received";
        String message = String.format("A new enquiry has been submitted by %s for market %s.", employee.getFullName(), market.getMarketName());
        notificationService.notifyAllAdmins(title, message,
            "ENQUIRY", null
        );

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
        enquiry.setRemarks(request.getRemarks());
        
        if (request.getStatus() == EnquiryStatus.APPROVED && request.getApprovedLoanAmount() != null) {
            enquiry.setApprovedLoanAmount(request.getApprovedLoanAmount());
        }

        EnquiryHistory history = EnquiryHistory.builder()
                .previousStatus(oldStatus)
                .newStatus(request.getStatus())
                .remarks(request.getRemarks())
                .actionBy(actionBy)
                .build();

        enquiry.addHistory(history);

        Enquiry savedEnquiry = enquiryRepository.save(enquiry);
        
        if (savedEnquiry.getEmployee() != null) {
            notificationService.createNotificationForUser(
                "Enquiry Status Updated",
                "Your enquiry for " + savedEnquiry.getFullName() + " is now " + request.getStatus().name(),
                savedEnquiry.getEmployee()
            );
        }
        
        return enquiryMapper.toResponse(savedEnquiry);
    }

    @Override
    @Transactional
    public CustomerResponse convertEnquiryToCustomer(UUID enquiryId, UUID actionById) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));

        if (enquiry.getStatus() != EnquiryStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED enquiries can be converted to customers. Current status: " + enquiry.getStatus()
            );
        }

        User actionBy = userRepository.findById(actionById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Split fullName into firstName / lastName
        String fullName = enquiry.getFullName() != null ? enquiry.getFullName().trim() : "";
        String firstName;
        String lastName;
        int spaceIdx = fullName.indexOf(' ');
        if (spaceIdx > 0) {
            firstName = fullName.substring(0, spaceIdx);
            lastName = fullName.substring(spaceIdx + 1);
        } else {
            firstName = fullName;
            lastName = "";
        }

        // Convert annual income -> monthly income
        BigDecimal monthlyIncome = null;
        if (enquiry.getAnnualIncome() != null && enquiry.getAnnualIncome().compareTo(BigDecimal.ZERO) > 0) {
            monthlyIncome = enquiry.getAnnualIncome()
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }

        // Map Gender enum
        Gender gender = null;
        if (enquiry.getGender() != null) {
            try {
                gender = Gender.valueOf(enquiry.getGender().name());
            } catch (IllegalArgumentException ignored) {
                // leave null if gender doesn't map
            }
        }

        CreateCustomerRequest customerRequest = new CreateCustomerRequest();
        customerRequest.setFirstName(firstName);
        customerRequest.setLastName(lastName.isEmpty() ? null : lastName);
        customerRequest.setMobileNumber(enquiry.getMobileNumber());
        customerRequest.setAlternateMobileNumber(enquiry.getAlternateMobile());
        customerRequest.setEmail(enquiry.getEmail());
        customerRequest.setDateOfBirth(enquiry.getDob());
        customerRequest.setGender(gender);
        customerRequest.setOccupation(enquiry.getOccupation());
        customerRequest.setMonthlyIncome(monthlyIncome);
        customerRequest.setStatus(CustomerStatus.ACTIVE);
        customerRequest.setMarketId(enquiry.getMarket().getId());
        // Address fields from enquiry
        if (enquiry.getCurrentAddress() != null) {
            customerRequest.setCurrentAddress(
                enquiry.getCurrentAddress().getAddressLine()
            );
        }
        if (enquiry.getPermanentAddress() != null) {
            customerRequest.setPermanentAddress(
                enquiry.getPermanentAddress().getAddressLine()
            );
        }

        CustomerResponse customerResponse = customerService.createCustomer(customerRequest);

        // Mark enquiry as CONVERTED and add history
        enquiry.setStatus(EnquiryStatus.CONVERTED);
        EnquiryHistory history = EnquiryHistory.builder()
                .previousStatus(EnquiryStatus.APPROVED)
                .newStatus(EnquiryStatus.CONVERTED)
                .remarks("Converted to Customer ID: " + customerResponse.getId())
                .actionBy(actionBy)
                .build();
        enquiry.addHistory(history);
        enquiryRepository.save(enquiry);

        return customerResponse;
    }
}