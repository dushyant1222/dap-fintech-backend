import re

path = 'src/main/java/com/dapfintech/enquiry/service/impl/EnquiryServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

target = """        // Check if already a customer
        if (enquiry.getStatus() == EnquiryStatus.APPROVED) {
            boolean exists = customerRepository.existsByFirstNameIgnoreCaseAndMobileNumber(
                    enquiry.getFullName().split(" ")[0],
                    enquiry.getMobileNumber()
            );
            if (exists) {
                dto.setStatus("CONVERTED");
            }
        }"""
replacement = """        // Check if already a customer by mobile number (since names can have split issues)
        if (enquiry.getStatus() == EnquiryStatus.APPROVED) {
            boolean exists = customerRepository.existsByMobileNumber(enquiry.getMobileNumber());
            if (exists) {
                dto.setStatus("CONVERTED");
            }
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched EnquiryServiceImpl to check by mobileNumber")
else:
    print("Target not found in EnquiryServiceImpl")
