package com.dapfintech.enquiry.dto;

import com.dapfintech.enquiry.enums.EnquiryMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnquiryMediaDto {
    @NotNull(message = "Media type is required")
    private EnquiryMediaType mediaType;

    @NotBlank(message = "File URL is required")
    private String fileUrl;
}