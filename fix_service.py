import re

with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'r') as f:
    content = f.read()

# Add import
imports_to_add = "import com.dapfintech.employee.service.DayBookService;\n"
content = content.replace("import com.dapfintech.employee.repository.DayBookRepository;", "import com.dapfintech.employee.repository.DayBookRepository;\n" + imports_to_add)

# Add dependency
deps_to_add = "    private final DayBookService dayBookService;\n"
content = content.replace("private final DayBookRepository dayBookRepository;", "private final DayBookRepository dayBookRepository;\n" + deps_to_add)

# In logic
old_logic = "dayBookRepository.findByEmployeeIdAndDate(loan.getCreatedBy().getId(), today).ifPresent(dayBook -> {"
new_logic = """dayBookService.getOrCreateTodayDayBook(loan.getCreatedBy().getId());
                dayBookRepository.findByEmployeeIdAndDate(loan.getCreatedBy().getId(), today).ifPresent(dayBook -> {"""
content = content.replace(old_logic, new_logic)

with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'w') as f:
    f.write(content)
print("Injected DayBookService and added getOrCreateTodayDayBook call")
