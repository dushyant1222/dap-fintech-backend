import re, os

patches = {
    'src/main/java/com/dapfintech/loan/service/impl/LoanApprovalServiceImpl.java': [
        # approve
        ('notificationService.notifyAllAdmins(\n                "Loan Approved",',
         'notificationService.notifyAllAdmins(\n                "Loan Approved",'),
        # reject  
        ('notificationService.notifyAllAdmins(\n                "Loan Rejected",',
         'notificationService.notifyAllAdmins(\n                "Loan Rejected",'),
    ],
    'src/main/java/com/dapfintech/enquiry/service/impl/EnquiryServiceImpl.java': None,
}

# Patch LoanApprovalServiceImpl - add LOAN navigationType with loan.getId()
path = 'src/main/java/com/dapfintech/loan/service/impl/LoanApprovalServiceImpl.java'
with open(path, 'r') as f:
    content = f.read()

# Replace notifyAllAdmins calls to include navigationType=LOAN
# Pattern: notificationService.notifyAllAdmins(\n...\n...loan.getId()...\n);
content = re.sub(
    r'notificationService\.notifyAllAdmins\(\s*\n(\s*"([^"]+)",\s*\n\s*"([^"]+)" \+\s*\n\s*loan\.getId\(\) \+\s*\n\s*"([^"]+)"\s*\n\s*\)',
    lambda m: f'notificationService.notifyAllAdmins(\n{m.group(1).split(chr(10))[0]}\"{m.group(2)}\",\n' + 
              m.group(1).split(chr(10))[0] + f'\"{ m.group(3)}\" + loan.getId() + \"{m.group(4)}\",\n' + 
              m.group(1).split(chr(10))[0] + '"LOAN", loan.getId()\n' + 
              m.group(1).split(chr(10))[0].rstrip() + ')',
    content
)

with open(path, 'w') as f:
    f.write(content)
    
print("Done patching LoanApprovalServiceImpl")
