import re

path = 'src/main/java/com/dapfintech/security/config/SecurityConfig.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('"/webjars/**","/api/v1/files/view/**", "/error")', '"/webjars/**", "/api/v1/files/view/**", "/api/v1/files/local/**", "/error")')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated SecurityConfig")
