path = 'src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix 1
content = content.replace(
    ".compoundPenalty(compoundPenalty)\n                        );",
    ".compoundPenalty(compoundPenalty)\n                        .build());"
)

# Fix 2
content = content.replace(
    ".totalPayableWithPenalty(totalPayable)\n                ;",
    ".totalPayableWithPenalty(totalPayable)\n                .build();"
)

# Fix 3
content = content.replace(
    ".remarks(\"SPECIAL CLOSURE SETTLEMENT\")\n                    ;",
    ".loan(loan)\n                    .remarks(\"SPECIAL CLOSURE SETTLEMENT\")\n                    .build();"
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed LoanPenaltyServiceImpl properly")
