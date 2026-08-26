import re

path1 = 'src/main/java/com/dapfintech/employee/service/DayBookService.java'
with open(path1, 'r', encoding='utf-8') as f:
    content1 = f.read()

target1 = "DayBookResponse getDayBookByDate(UUID employeeId, LocalDate date);"
replacement1 = target1 + "\n    java.util.List<com.dapfintech.employee.entity.DayBookTransaction> getTransactions(UUID employeeId, LocalDate date);"
content1 = content1.replace(target1, replacement1)
with open(path1, 'w', encoding='utf-8') as f:
    f.write(content1)


path2 = 'src/main/java/com/dapfintech/employee/service/DayBookServiceImpl.java'
with open(path2, 'r', encoding='utf-8') as f:
    content2 = f.read()

target2 = "public DayBookResponse getDayBookByDate(UUID employeeId, LocalDate date) {"
replacement2 = """    @Override
    public java.util.List<com.dapfintech.employee.entity.DayBookTransaction> getTransactions(UUID employeeId, LocalDate date) {
        java.time.LocalDateTime start = date.atStartOfDay();
        java.time.LocalDateTime end = date.plusDays(1).atStartOfDay();
        return dayBookTransactionRepository.findByEmployeeIdAndCreatedAtBetween(employeeId, start, end);
    }
    
    @Override
    public DayBookResponse getDayBookByDate(UUID employeeId, LocalDate date) {"""

content2 = content2.replace(target2, replacement2)
with open(path2, 'w', encoding='utf-8') as f:
    f.write(content2)

print("Patched DayBookService interface and impl")
