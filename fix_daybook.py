import re

path = 'src/main/java/com/dapfintech/employee/service/DayBookServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("    @Override\n        @Override", "    @Override")

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed double Override in DayBookServiceImpl")
