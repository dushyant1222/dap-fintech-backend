with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'r') as f:
    content = f.read()
content = content.replace('CollectionStatus.VERIFIED', 'CollectionStatus.SUCCESS')
with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'w') as f:
    f.write(content)
print('Fixed VERIFIED to SUCCESS')
