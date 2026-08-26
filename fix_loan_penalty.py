import re

path = 'src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix 1: Add .build() at line 115 and 136
content = re.sub(
    r'(return OverdueInstallmentPenaltyResponse\.builder\(\)\s+\.installmentId\([^)]+\)\s+\.dueDate\([^)]+\)\s+\.overdueDays\([^)]+\)\s+\.penaltyAmount\([^)]+\)\s+\.isPenaltyPaid\([^)]+\)\s+\.penaltyPaidDate\([^)]*\)\s+\.remarks\([^)]*\)\s*;)',
    lambda m: m.group(1).replace(';', '\n                .build();'),
    content
)

content = re.sub(
    r'(return LoanPenaltySummaryResponse\.builder\(\)\s+\.loanId\([^)]+\)\s+\.totalPenaltyAmount\([^)]+\)\s+\.totalPenaltyPaid\([^)]+\)\s+\.totalPenaltyPending\([^)]+\)\s+\.penaltyDetails\([^)]+\)\s*;)',
    lambda m: m.group(1).replace(';', '\n                .build();'),
    content
)

# Fix 2: Add .build() to LoanCollection
content = re.sub(
    r'(\.remarks\("SPECIAL CLOSURE SETTLEMENT"\)\s*;)',
    r'\1'.replace(';', '\n                    .build();'),
    content
)

# Fix 3: Fix DayBookService call
content = content.replace(
    'dayBookService.addTransaction(loan.getCreatedBy().getId(), "COLLECTIONS", request.getSettlementAmountPaid(), "SPECIAL CLOSURE SETTLEMENT");',
    'com.dapfintech.employee.dto.DayBookTransactionRequest txReq = new com.dapfintech.employee.dto.DayBookTransactionRequest();\n                    txReq.setType("COLLECTIONS");\n                    txReq.setAmount(request.getSettlementAmountPaid());\n                    txReq.setRemarks("SPECIAL CLOSURE SETTLEMENT");\n                    dayBookService.addTransaction(loan.getCreatedBy().getId(), txReq);'
)

# Fix 4: Fix LoanClosureResponse
content = content.replace(
    'return new LoanClosureResponse(true, "Loan closed successfully", null);',
    'return LoanClosureResponse.builder().id(savedClosure.getId()).loanId(loan.getId()).closureDate(savedClosure.getClosureDate()).remarks(savedClosure.getRemarks()).build();'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed LoanPenaltyServiceImpl")

# Add WAIVED to RepaymentStatus
status_path = 'src/main/java/com/dapfintech/loan/enums/RepaymentStatus.java'
with open(status_path, 'r', encoding='utf-8') as f:
    status_content = f.read()

if "WAIVED" not in status_content:
    status_content = status_content.replace('OVERDUE', 'OVERDUE,\n    WAIVED')
    with open(status_path, 'w', encoding='utf-8') as f:
        f.write(status_content)
    print("Added WAIVED to RepaymentStatus")

