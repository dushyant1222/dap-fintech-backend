import sys

with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the messy patched LoanClosure lines
target = r"LoanClosure closure = new LoanClosure\(\)\s*;\s*LoanClosure savedClosure = loanClosureRepository\.save\(closure\);"
replacement = """LoanClosure closure = new LoanClosure();
        closure.setLoan(loan);
        closure.setClosureDate(LocalDateTime.now());
        closure.setRemarks("SPECIAL CONDITION CLOSURE: " + request.getSpecialRemarks() + " [Penalty Waived: " + loan.getPenaltyWaivedPercent() + "%]");
        LoanClosure savedClosure = loanClosureRepository.save(closure);"""

import re
content = re.sub(target, replacement, content)

with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Added manual setters for LoanClosure")
