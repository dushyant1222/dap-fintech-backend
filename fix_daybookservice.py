path = 'src/main/java/com/dapfintech/employee/service/DayBookService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

if "getTransactions(UUID employeeId, java.time.LocalDate date)" not in content:
    content = content.replace(
        "List<DayBookResponse> getEmployeeDayBooks(UUID employeeId);",
        "List<DayBookResponse> getEmployeeDayBooks(UUID employeeId);\n    java.util.List<com.dapfintech.employee.entity.DayBookTransaction> getTransactions(UUID employeeId, java.time.LocalDate date);"
    )
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Added getTransactions to DayBookService")
