# Patch EnquiryServiceImpl
path = 'src/main/java/com/dapfintech/enquiry/service/impl/EnquiryServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

import re
def add_enquiry_nav(m):
    call = m.group(0)
    if '"ENQUIRY"' in call or '"LOAN"' in call:
        return call
    return call[:-1] + ',\n            "ENQUIRY", null\n        );'

content = re.sub(
    r'notificationService\.notifyAllAdmins\([^;]+?\);',
    add_enquiry_nav,
    content,
    flags=re.DOTALL
)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done patching EnquiryServiceImpl")
