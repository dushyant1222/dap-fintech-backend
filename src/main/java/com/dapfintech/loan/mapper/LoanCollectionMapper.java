package com.dapfintech.loan.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.loan.dto.response.CollectionResponse;
import com.dapfintech.loan.entity.LoanCollection;

@Component
public class LoanCollectionMapper {

    public CollectionResponse toResponse(
            LoanCollection collection
    ) {

        return CollectionResponse.builder()
                .id(collection.getId())
                .loanId(
                        collection.getLoan().getId()
                )
                .receiptNumber(
                        collection.getReceiptNumber()
                )
                .collectedAmount(
                        collection.getCollectedAmount()
                )
                .collectionDate(
                        collection.getCollectionDate()
                )
                .collectionMode(
                        collection.getCollectionMode()
                )
                .collectionStatus(
                        collection.getCollectionStatus()
                )
                .remarks(
                        collection.getRemarks()
                )
                .build();
    }
}