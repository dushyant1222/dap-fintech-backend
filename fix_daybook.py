import re

with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'r') as f:
    content = f.read()

bad_logic = """
                    BigDecimal newClosing = dayBook.getOpeningBalance()
                            .add(dayBook.getCollections())
                            .add(dayBook.getIncomingTransfers())
                            .subtract(dayBook.getSpends())
                            .subtract(dayBook.getLoansDisbursed())
                            .subtract(dayBook.getOutgoingTransfers())
                            .subtract(dayBook.getOfficeRemittance());
"""

good_logic = """
                    if (dayBook.getCashIncomingTransfers() == null) dayBook.setCashIncomingTransfers(BigDecimal.ZERO);
                    if (dayBook.getCashOutgoingTransfers() == null) dayBook.setCashOutgoingTransfers(BigDecimal.ZERO);
                    
                    BigDecimal newClosing = dayBook.getOpeningBalance()
                            .add(dayBook.getCollections())
                            .add(dayBook.getIncomingTransfers())
                            .add(dayBook.getCashIncomingTransfers())
                            .subtract(dayBook.getSpends())
                            .subtract(dayBook.getLoansDisbursed())
                            .subtract(dayBook.getOutgoingTransfers())
                            .subtract(dayBook.getCashOutgoingTransfers())
                            .subtract(dayBook.getOfficeRemittance());
"""

content = content.replace(bad_logic, good_logic)
with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'w') as f:
    f.write(content)
print("Updated DayBook closing balance calculation")
