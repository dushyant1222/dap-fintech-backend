import re

path = 'src/main/java/com/dapfintech/employee/controller/DayBookController.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

target = """    @PostMapping("/today/transactions")
    public ResponseEntity<DayBookResponse> addTransaction(
            @PathVariable UUID employeeId,"""

replacement = """    @GetMapping("/today/transactions")
    public ResponseEntity<java.util.List<com.dapfintech.employee.entity.DayBookTransaction>> getTodayTransactions(
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(dayBookService.getTransactions(employeeId, java.time.LocalDate.now()));
    }

    @GetMapping("/by-date/transactions")
    public ResponseEntity<java.util.List<com.dapfintech.employee.entity.DayBookTransaction>> getByDateTransactions(
            @PathVariable UUID employeeId,
            @org.springframework.web.bind.annotation.RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        return ResponseEntity.ok(dayBookService.getTransactions(employeeId, date));
    }

    @PostMapping("/today/transactions")
    public ResponseEntity<DayBookResponse> addTransaction(
            @PathVariable UUID employeeId,"""

if target in content:
    content = content.replace(target, replacement)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched DayBookController")
else:
    print("Target not found")
