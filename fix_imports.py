import os

files = [
    'src/main/java/com/dapfintech/loan/service/impl/LoanCollectionServiceImpl.java',
    'src/main/java/com/dapfintech/loan/service/impl/LoanDisbursementServiceImpl.java'
]

for path in files:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    content = content.replace('com.dapfintech.employee.dto.request.DayBookTransactionRequest', 'com.dapfintech.employee.dto.DayBookTransactionRequest')
    content = content.replace('import com.dapfintech.employee.dto.request.DayBookTransactionRequest;', 'import com.dapfintech.employee.dto.DayBookTransactionRequest;')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Fixed imports")
