import re

path = 'src/main/java/com/dapfintech/enquiry/service/impl/EnquiryServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add CustomerRepository
if "private final com.dapfintech.customer.repository.CustomerRepository customerRepository;" not in content:
    content = content.replace(
        "private final CustomerService customerService;",
        "private final CustomerService customerService;\n    private final com.dapfintech.customer.repository.CustomerRepository customerRepository;"
    )
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed EnquiryServiceImpl.java")
else:
    print("Already fixed")
