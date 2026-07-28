package com.dapfintech.capital.service;

import java.util.List;
import java.util.UUID;
import com.dapfintech.capital.dto.request.InternalTransferRequest;
import com.dapfintech.capital.dto.response.InternalTransferResponse;

public interface InternalTransferService {
    InternalTransferResponse initiateTransfer(InternalTransferRequest request);
    InternalTransferResponse acceptTransfer(UUID transferId);
    InternalTransferResponse rejectTransfer(UUID transferId);
    
    List<InternalTransferResponse> getPendingIncomingTransfers();
    List<InternalTransferResponse> getMyIncomingTransfers();
    List<InternalTransferResponse> getMyOutgoingTransfers();
}
