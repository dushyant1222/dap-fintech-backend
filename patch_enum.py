import re

path = 'src/main/java/com/dapfintech/loan/enums/RepaymentStatus.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

if 'WAIVED' not in content:
    content = content.replace("PARTIAL", "PARTIAL,\n    WAIVED")

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Patched RepaymentStatus")
