import os

for root, dirs, files in os.walk('src'):
    for file in files:
        if file.endswith('.java'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
                if "existsBy" in content and "customerRepository" in content and "EnquiryStatus.APPROVED" in content:
                    print(f"Found in {path}")
