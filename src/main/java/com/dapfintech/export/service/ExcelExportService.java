package com.dapfintech.export.service;

import org.springframework.core.io.ByteArrayResource;

public interface ExcelExportService {

    ByteArrayResource exportOverdueCustomers();
    ByteArrayResource exportCollections();
}