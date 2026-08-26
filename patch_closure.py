import sys

with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("LoanClosure.builder()", "new LoanClosure()")
content = content.replace(".loan(loan)", "")
content = content.replace('.closureDate(LocalDateTime.now())', '')
content = content.replace('.remarks("SPECIAL CONDITION CLOSURE: " + request.getSpecialRemarks() + " [Penalty Waived: " + loan.getPenaltyWaivedPercent() + "%]")', '')
content = content.replace(".build()", "")

with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("Removed builder from LoanClosure")
