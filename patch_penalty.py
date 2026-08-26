import re

with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'r') as f:
    content = f.read()

# Add imports
imports_to_add = """
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.entity.LoanCollection;
import com.dapfintech.loan.enums.CollectionStatus;
import com.dapfintech.loan.enums.CollectionMode;
import com.dapfintech.employee.repository.DayBookRepository;
"""
content = content.replace("import com.dapfintech.loan.repository.LoanRepository;", "import com.dapfintech.loan.repository.LoanRepository;\n" + imports_to_add)

# Add dependencies
deps_to_add = """
    private final LoanCollectionRepository loanCollectionRepository;
    private final DayBookRepository dayBookRepository;
"""
content = content.replace("private final AccessControlService accessControlService;", "private final AccessControlService accessControlService;\n" + deps_to_add)

# Replace closeOnSpecialCondition logic
logic_to_add = """
        // CREATE LOAN COLLECTION FOR THE SETTLEMENT AMOUNT
        if (request.getSettlementAmountPaid() != null && request.getSettlementAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            LoanCollection collection = LoanCollection.builder()
                    .loan(loan)
                    .collectedBy(loan.getCreatedBy())
                    .collectedAmount(request.getSettlementAmountPaid())
                    .collectionDate(LocalDateTime.now())
                    .collectionMode(CollectionMode.CASH)
                    .collectionStatus(CollectionStatus.VERIFIED)
                    .receiptNumber("SPL-" + System.currentTimeMillis())
                    .remarks("SPECIAL CLOSURE SETTLEMENT")
                    .build();
            loanCollectionRepository.save(collection);

            // UPDATE DAYBOOK OF THE EMPLOYEE WHO CREATED THE LOAN
            if (loan.getCreatedBy() != null && loan.getCreatedBy().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
                LocalDate today = LocalDate.now();
                dayBookRepository.findByEmployeeIdAndDate(loan.getCreatedBy().getId(), today).ifPresent(dayBook -> {
                    if (dayBook.getCollections() == null) dayBook.setCollections(BigDecimal.ZERO);
                    dayBook.setCollections(dayBook.getCollections().add(request.getSettlementAmountPaid()));
                    
                    if (dayBook.getOpeningBalance() == null) dayBook.setOpeningBalance(BigDecimal.ZERO);
                    if (dayBook.getIncomingTransfers() == null) dayBook.setIncomingTransfers(BigDecimal.ZERO);
                    if (dayBook.getSpends() == null) dayBook.setSpends(BigDecimal.ZERO);
                    if (dayBook.getLoansDisbursed() == null) dayBook.setLoansDisbursed(BigDecimal.ZERO);
                    if (dayBook.getOutgoingTransfers() == null) dayBook.setOutgoingTransfers(BigDecimal.ZERO);
                    if (dayBook.getOfficeRemittance() == null) dayBook.setOfficeRemittance(BigDecimal.ZERO);
                    
                    BigDecimal newClosing = dayBook.getOpeningBalance()
                            .add(dayBook.getCollections())
                            .add(dayBook.getIncomingTransfers())
                            .subtract(dayBook.getSpends())
                            .subtract(dayBook.getLoansDisbursed())
                            .subtract(dayBook.getOutgoingTransfers())
                            .subtract(dayBook.getOfficeRemittance());
                    dayBook.setClosingBalance(newClosing);
                    dayBookRepository.save(dayBook);
                });
            }
        }
"""
content = content.replace("loan.setClosedSpecialCondition(true);", logic_to_add + "\n        loan.setClosedSpecialCondition(true);")

with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'w') as f:
    f.write(content)
print("Patched successfully")
