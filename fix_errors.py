import re

def fix_enquiry():
    path = 'src/main/java/com/dapfintech/enquiry/service/impl/EnquiryServiceImpl.java'
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace('notificationService.notifyAllAdmins(title, message),\n            "ENQUIRY", null\n        );', 'notificationService.notifyAllAdmins(title, message,\n            "ENQUIRY", null\n        );')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
        
def fix_loan():
    path = 'src/main/java/com/dapfintech/loan/service/impl/LoanApprovalServiceImpl.java'
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Replace wrongly placed comma and parenthesis
    content = re.sub(r'\s*\),\s*"LOAN", loan\.getId\(\)\s*\);', ',\n                "LOAN", loan.getId()\n        );', content)
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

fix_enquiry()
fix_loan()
print("Fixed files")
