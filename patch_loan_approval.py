import re

path = 'src/main/java/com/dapfintech/loan/service/impl/LoanApprovalServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Find all notifyAllAdmins calls and add navigationType="LOAN", loan.getId() 
# Pattern: notificationService.notifyAllAdmins(\n...title...\n...message...\n        );
def patch_notify(content, old_call, loan_var='loan.getId()'):
    # Replace 2-arg version with 4-arg version including navigation data
    # We need to insert , "LOAN", loan_var before the closing );
    # Find the pattern and manually replace
    return content

# Simpler approach: just do text replacement for each specific call
replacements = [
    (
        'notificationService.notifyAllAdmins(\n                "Loan Application Submitted",\n                "A new loan application has been submitted for approval. Loan ID: " + loan.getId()\n        );',
        'notificationService.notifyAllAdmins(\n                "Loan Application Submitted",\n                "A new loan application has been submitted for approval. Loan ID: " + loan.getId(),\n                "LOAN", loan.getId()\n        );'
    ),
    (
        'notificationService.notifyAllAdmins(\n                "Loan Approved",\n                "Loan " +\n                loan.getId() +\n                " has been approved and disbursed."\n        );',
        'notificationService.notifyAllAdmins(\n                "Loan Approved",\n                "Loan " +\n                loan.getId() +\n                " has been approved and disbursed.",\n                "LOAN", loan.getId()\n        );'
    ),
    (
        'notificationService.notifyAllAdmins(\n                "Loan Rejected",\n                "Loan " +\n                loan.getId() +\n                " has been rejected."\n        );',
        'notificationService.notifyAllAdmins(\n                "Loan Rejected",\n                "Loan " +\n                loan.getId() +\n                " has been rejected.",\n                "LOAN", loan.getId()\n        );'
    ),
    (
        'notificationService.notifyAllAdmins(\n                "Loan Resubmitted for Approval",\n                "Loan " +\n                        loan.getId() +\n                        " has been resubmitted for approval."\n        );',
        'notificationService.notifyAllAdmins(\n                "Loan Resubmitted for Approval",\n                "Loan " +\n                        loan.getId() +\n                        " has been resubmitted for approval.",\n                "LOAN", loan.getId()\n        );'
    ),
]

for old, new in replacements:
    if old in content:
        content = content.replace(old, new)
        print(f"Patched: {old[:60]}")
    else:
        print(f"NOT FOUND: {old[:60]}")

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done")
