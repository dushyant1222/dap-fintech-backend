import re

path = 'src/main/java/com/dapfintech/loan/service/impl/LoanCollectionServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

target = """        // Update DayBook if collected by an employee
        if (loggedInEmployee != null && loggedInEmployee.getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            java.time.LocalDate today = java.time.LocalDate.now();
            final java.math.BigDecimal finalCollectedAmount = collection.getCollectedAmount();
            dayBookRepository.findByEmployeeIdAndDate(loggedInEmployee.getId(), today).ifPresent(dayBook -> {
                if (dayBook.getCollections() == null) {
                    dayBook.setCollections(java.math.BigDecimal.ZERO);
                }
                dayBook.setCollections(dayBook.getCollections().add(finalCollectedAmount));
                
                // Recalculate closing balance
                if (dayBook.getOpeningBalance() == null) dayBook.setOpeningBalance(java.math.BigDecimal.ZERO);
                if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(java.math.BigDecimal.ZERO);
                if (dayBook.getSpends() == null) dayBook.setSpends(java.math.BigDecimal.ZERO);
                if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(java.math.BigDecimal.ZERO);
                if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(java.math.BigDecimal.ZERO);
                if (dayBook.getOfficeRemittance() == null) dayBook.setOfficeRemittance(java.math.BigDecimal.ZERO);
                
                java.math.BigDecimal newClosing = dayBook.getOpeningBalance()
                        .add(dayBook.getCollections())
                        .add(dayBook.getIncomingTransfers())
                        .subtract(dayBook.getSpends())
                        .subtract(dayBook.getLoansDisbursed())
                        .subtract(dayBook.getOutgoingTransfers())
                        .subtract(dayBook.getOfficeRemittance());
                
                dayBook.setClosingBalance(newClosing);
                dayBookRepository.save(dayBook);
            });
        }"""

replacement = """        // Update DayBook if collected by an employee
        if (loggedInEmployee != null && loggedInEmployee.getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            try {
                com.dapfintech.employee.dto.request.DayBookTransactionRequest dbReq = new com.dapfintech.employee.dto.request.DayBookTransactionRequest();
                dbReq.setType("COLLECTIONS");
                dbReq.setAmount(collection.getCollectedAmount());
                dbReq.setRemarks("EMI Collected: " + loan.getLoanCode());
                // We use DayBookService here to ensure a DayBookTransaction is properly logged.
                // Assuming we can instantiate or autowire it, but we can't easily autowire in string replace. 
                // Wait! Is dayBookService autowired in LoanCollectionServiceImpl?
            } catch(Exception e) {
                log.error("Failed to add DayBook transaction", e);
            }
        }"""

if target in content:
    print("Target found")
else:
    print("Target not found")
