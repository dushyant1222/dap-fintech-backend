package com.dapfintech.enquiry.mapper;

import com.dapfintech.enquiry.dto.EnquiryMediaDto;
import com.dapfintech.enquiry.dto.EnquiryRequest;
import com.dapfintech.enquiry.dto.EnquiryResponse;
import com.dapfintech.enquiry.entity.Enquiry;
import com.dapfintech.enquiry.entity.EnquiryAddress;
import com.dapfintech.enquiry.entity.EnquiryMedia;
import com.dapfintech.enquiry.enums.AddressType;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EnquiryMapper {

    public Enquiry toEntity(EnquiryRequest request) {
        Enquiry enquiry = Enquiry.builder()
                .fullName(request.getFullName())
                .fatherName(request.getFatherName())
                .motherName(request.getMotherName())
                .mobileNumber(request.getMobileNumber())
                .alternateMobile(request.getAlternateMobile())
                .email(request.getEmail())
                .dob(request.getDob())
                .gender(request.getGender())
                .occupation(request.getOccupation())
                .qualification(request.getQualification())
                .businessType(request.getBusinessType())
                .businessName(request.getBusinessName())
                .annualIncome(request.getAnnualIncome())
                .referenceSource(request.getReferenceSource())
                .gpsLatitude(request.getGpsLatitude())
                .gpsLongitude(request.getGpsLongitude())
                .loanDemandAmount(request.getLoanDemandAmount())
                .remarks(request.getRemarks())
                .currentAddress(EnquiryAddress.builder()
                        .addressType(AddressType.CURRENT)
                        .addressLine(request.getCurrentAddress())
                        .build())
                .permanentAddress(EnquiryAddress.builder()
                        .addressType(AddressType.PERMANENT)
                        .addressLine(request.getPermanentAddress())
                        .build())
                .build();

        if (request.getMedia() != null) {
            request.getMedia().forEach(mediaDto -> {
                EnquiryMedia media = EnquiryMedia.builder()
                        .mediaType(mediaDto.getMediaType())
                        .fileUrl(mediaDto.getFileUrl())
                        .build();
                enquiry.addMedia(media);
            });
        }
        return enquiry;
    }

    public EnquiryResponse toResponse(Enquiry enquiry) {
        EnquiryResponse response = new EnquiryResponse();
        response.setId(enquiry.getId());
        response.setFullName(enquiry.getFullName());
        response.setFatherName(enquiry.getFatherName());
        response.setMotherName(enquiry.getMotherName());
        response.setMobileNumber(enquiry.getMobileNumber());
        response.setAlternateMobile(enquiry.getAlternateMobile());
        response.setEmail(enquiry.getEmail());
        response.setDob(enquiry.getDob());
        response.setGender(enquiry.getGender());
        response.setOccupation(enquiry.getOccupation());
        response.setQualification(enquiry.getQualification());
        response.setBusinessType(enquiry.getBusinessType());
        response.setBusinessName(enquiry.getBusinessName());
        response.setAnnualIncome(enquiry.getAnnualIncome());
        response.setReferenceSource(enquiry.getReferenceSource());
        response.setGpsLatitude(enquiry.getGpsLatitude());
        response.setGpsLongitude(enquiry.getGpsLongitude());
        response.setLoanDemandAmount(enquiry.getLoanDemandAmount());
        response.setRemarks(enquiry.getRemarks());
        response.setStatus(enquiry.getStatus());
        response.setCurrentAddress(enquiry.getCurrentAddress().getAddressLine());
        response.setPermanentAddress(enquiry.getPermanentAddress().getAddressLine());
        response.setCreatedAt(enquiry.getCreatedAt());
       
        
        if (enquiry.getEmployee() != null) {
            response.setEmployeeId(enquiry.getEmployee().getId());
            response.setEmployeeName(enquiry.getEmployee().getFullName());
        }
        
        if (enquiry.getMarket() != null) {
            response.setMarketId(enquiry.getMarket().getId());
            response.setMarketName(enquiry.getMarket().getMarketName());
        }

        if (enquiry.getMediaList() != null) {
            response.setMedia(enquiry.getMediaList().stream().map(m -> {
                EnquiryMediaDto dto = new EnquiryMediaDto();
                dto.setMediaType(m.getMediaType());
                dto.setFileUrl(m.getFileUrl());
                return dto;
            }).collect(Collectors.toList()));
        }
        return response;
    }
}