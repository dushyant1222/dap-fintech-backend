path = 'src/main/java/com/dapfintech/loan/service/impl/LoanApprovalServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Find remaining unpatched notifyAllAdmins calls (still have 2 args)
# and patch all of them with LOAN + loan.getId()
import re

# Replace any notifyAllAdmins call that ends with        ); (2-arg)
# by adding , "LOAN", loan.getId() before the );
def add_loan_nav(m):
    call = m.group(0)
    if '"LOAN"' in call:
        return call  # already patched
    # Insert , "LOAN", loan.getId() before final );
    return call[:-1] + ',\n                "LOAN", loan.getId()\n        );'

content = re.sub(
    r'notificationService\.notifyAllAdmins\([^;]+?\);',
    add_loan_nav,
    content,
    flags=re.DOTALL
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done patching all notifyAllAdmins calls in LoanApprovalServiceImpl")
