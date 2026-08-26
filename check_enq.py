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
replacement = """        // Check if already a customer (check BOTH full name properly if split, OR just compare first name but we can just use existsByFirstName...)
        // Since we want to check accurately, let's check first name and mobile. Wait, the user said we should take full name.
        if (enquiry.getStatus() == EnquiryStatus.APPROVED) {
            boolean exists = customerRepository.existsByFirstNameIgnoreCaseAndMobileNumber(
                    enquiry.getFullName().split(" ")[0],
                    enquiry.getMobileNumber()
            );
            if (exists) {
                dto.setStatus("CONVERTED");
            }
        }"""

# Oh, Customer only has `firstName` and `lastName`. Enquiry only has `fullName`.
# The current check splits `fullName` by space and checks `existsByFirstNameIgnoreCaseAndMobileNumber`.
# Why is this failing? Because maybe `add_customer.dart` isn't updating it properly?
