import re

path = 'src/main/java/com/dapfintech/loan/service/impl/LoanDisbursementServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Make sure DayBookService is available
if 'DayBookService' not in content:
    target = 'private final DayBookRepository dayBookRepository;'
    replacement = 'private final DayBookRepository dayBookRepository;\n    private final com.dapfintech.employee.service.DayBookService dayBookService;'
    content = content.replace(target, replacement)

target2 = """        dayBookRepository.findByEmployeeIdAndDate(loggedInUser.getId(), today).ifPresent(dayBook -> {
            if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(java.math.BigDecimal.ZERO);
            dayBook.setLoansDisbursed(dayBook.getLoansDisbursed().add(netDisbursedAmount));

            if (dayBook.getOpeningBalance() == null) dayBook.setOpeningBalance(java.math.BigDecimal.ZERO);
            if (dayBook.getCollections() == null) dayBook.setCollections(java.math.BigDecimal.ZERO);
            if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(java.math.BigDecimal.ZERO);
            if (dayBook.getSpends() == null) dayBook.setSpends(java.math.BigDecimal.ZERO);
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
        });"""

replacement2 = """        try {
            com.dapfintech.employee.dto.request.DayBookTransactionRequest dbReq = new com.dapfintech.employee.dto.request.DayBookTransactionRequest();
            dbReq.setType("LOANS_DISBURSED");
            dbReq.setAmount(netDisbursedAmount);
            dbReq.setRemarks("Loan Disbursed: " + loan.getLoanCode());
            dayBookService.addTransaction(loggedInUser.getId(), dbReq);
        } catch(Exception e) {
            e.printStackTrace();
        }"""

if target2 in content:
    content = content.replace(target2, replacement2)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched LoanDisbursementServiceImpl")
else:
    print("Target2 not found")
