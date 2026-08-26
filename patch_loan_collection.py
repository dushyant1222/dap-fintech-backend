import re

path = 'src/main/java/com/dapfintech/loan/service/impl/LoanCollectionServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add DayBookService field
if 'private final com.dapfintech.employee.service.DayBookService dayBookService;' not in content:
    target = 'private final DayBookRepository dayBookRepository;'
    replacement = 'private final DayBookRepository dayBookRepository;\n    private final com.dapfintech.employee.service.DayBookService dayBookService;'
    content = content.replace(target, replacement)

# Replace the block
target2 = """        // Update DayBook if collected by an employee
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

replacement2 = """        // Update DayBook if collected by an employee
        if (loggedInEmployee != null && loggedInEmployee.getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
            try {
                com.dapfintech.employee.dto.request.DayBookTransactionRequest dbReq = new com.dapfintech.employee.dto.request.DayBookTransactionRequest();
                dbReq.setType("COLLECTIONS");
                dbReq.setAmount(collection.getCollectedAmount());
                dbReq.setRemarks("EMI Collected: " + loan.getLoanCode());
                dayBookService.addTransaction(loggedInEmployee.getId(), dbReq);
            } catch(Exception e) {
                // If it fails (e.g. daybook closed), we don't block the collection, just log it.
                e.printStackTrace();
            }
        }"""

if target2 in content:
    content = content.replace(target2, replacement2)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched LoanCollectionServiceImpl")
else:
    print("Target2 not found")
