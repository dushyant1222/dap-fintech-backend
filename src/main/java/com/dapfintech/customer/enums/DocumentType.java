package com.dapfintech.customer.enums;

public enum DocumentType {

    // ================================
    // MANDATORY DOCUMENTS
    // ================================

    AADHAAR_FRONT(true),

    AADHAAR_BACK(true),

    PAN_CARD(true),

    CUSTOMER_SELFIE(true),

    SHOP_PHOTO(true),


    // ================================
    // OPTIONAL DOCUMENTS
    // ================================

    BUSINESS_PROOF(false),

    ADDRESS_PROOF(false),

    INCOME_PROOF(false);


    private final boolean mandatory;


    DocumentType(boolean mandatory) {

        this.mandatory = mandatory;

    }


    public boolean isMandatory() {

        return mandatory;

    }
}