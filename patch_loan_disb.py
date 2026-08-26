import re

path = 'src/main/java/com/dapfintech/loan/service/impl/LoanDisbursementServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Make sure DayBookService is available
if 'DayBookService' not in content:
    target = 'private final DayBookRepository dayBookRepository;'
    replacement = 'private final DayBookRepository dayBookRepository;\n    private final com.dapfintech.employee.service.DayBookService dayBookService;'
    content = content.replace(target, replacement)

pattern = r'dayBookRepository\.findByEmployeeIdAndDate\(loggedInUser\.getId\(\), today\)\.ifPresent\(dayBook -> \{.*?\n        \}\);'
replacement2 = """try {
            com.dapfintech.employee.dto.request.DayBookTransactionRequest dbReq = new com.dapfintech.employee.dto.request.DayBookTransactionRequest();
            dbReq.setType("LOANS_DISBURSED");
            dbReq.setAmount(netDisbursedAmount);
            dbReq.setRemarks("Loan Disbursed: " + loan.getLoanCode());
            dayBookService.addTransaction(loggedInUser.getId(), dbReq);
        } catch(Exception e) {
            e.printStackTrace();
        }"""

content = re.sub(pattern, replacement2, content, flags=re.DOTALL)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Patched LoanDisbursementServiceImpl using regex")
