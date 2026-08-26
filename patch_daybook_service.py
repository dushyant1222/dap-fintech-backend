import re

path = 'src/main/java/com/dapfintech/employee/service/DayBookServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

if "DayBookTransactionRepository" not in content:
    content = content.replace("private InternalTransferRepository internalTransferRepository;", "private InternalTransferRepository internalTransferRepository;\n    @org.springframework.beans.factory.annotation.Autowired\n    private com.dapfintech.employee.repository.DayBookTransactionRepository dayBookTransactionRepository;")
    
    target = """        switch (request.getType().toUpperCase()) {
            case "SPENDS":
                dayBook.setSpends(dayBook.getSpends().add(amount));
                break;
            case "COLLECTIONS":
                dayBook.setCollections(dayBook.getCollections().add(amount));
                break;
            case "LOANS_DISBURSED":
                dayBook.setLoansDisbursed(dayBook.getLoansDisbursed().add(amount));
                break;
            case "OFFICE_REMITTANCE":
                dayBook.setOfficeRemittance(dayBook.getOfficeRemittance().add(amount));
                break;
            default:
                throw new RuntimeException("Invalid transaction type");
        }"""
        
    replacement = """        switch (request.getType().toUpperCase()) {
            case "SPENDS":
                dayBook.setSpends(dayBook.getSpends().add(amount));
                break;
            case "COLLECTIONS":
                dayBook.setCollections(dayBook.getCollections().add(amount));
                break;
            case "LOANS_DISBURSED":
                dayBook.setLoansDisbursed(dayBook.getLoansDisbursed().add(amount));
                break;
            case "OFFICE_REMITTANCE":
                dayBook.setOfficeRemittance(dayBook.getOfficeRemittance().add(amount));
                break;
            default:
                throw new RuntimeException("Invalid transaction type");
        }
        
        com.dapfintech.employee.entity.DayBookTransaction tx = new com.dapfintech.employee.entity.DayBookTransaction();
        tx.setEmployeeId(employeeId);
        tx.setType(request.getType().toUpperCase());
        tx.setAmount(amount);
        tx.setRemarks(request.getRemarks());
        tx.setDayBook(dayBook);
        dayBookTransactionRepository.save(tx);"""
        
    content = content.replace(target, replacement)
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched DayBookServiceImpl")
else:
    print("Already patched")
