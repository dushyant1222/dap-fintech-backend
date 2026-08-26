import sys

with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()

start_idx = -1
end_idx = -1

for i, line in enumerate(lines):
    if "public LoanClosureResponse closeOnSpecialCondition" in line:
        start_idx = i - 1
        break

if start_idx != -1:
    open_brackets = 0
    for i in range(start_idx + 1, len(lines)):
        open_brackets += lines[i].count('{') - lines[i].count('}')
        if open_brackets == 0 and lines[i].strip() == '}':
            end_idx = i
            break

print(f"Start: {start_idx}, End: {end_idx}")

if start_idx != -1 and end_idx != -1:
    new_method = """    @Override
    @Transactional
    public LoanClosureResponse closeOnSpecialCondition(UUID loanId, CloseSpecialLoanRequest request) {
        User currentUser = verifyAdminAccess();
        accessControlService.validateLoanAccess(loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getLoanStatus() == LoanStatus.CLOSED) {
            throw new RuntimeException("This loan is already closed.");
        }

        if (request.getSpecialRemarks() == null || request.getSpecialRemarks().trim().isEmpty()) {
            throw new RuntimeException("Special remarks explaining genuine problem / compromise condition are required.");
        }

        if (loanClosureRepository.existsByLoanId(loanId)) {
            throw new RuntimeException("This loan already has a closure record.");
        }

        if (request.getWaivedPenaltyPercent() != null) {
            loan.setPenaltyWaivedPercent(request.getWaivedPenaltyPercent());
        } else {
            loan.setPenaltyWaivedPercent(BigDecimal.valueOf(100));
        }

        // CREATE LOAN COLLECTION FOR THE SETTLEMENT AMOUNT
        if (request.getSettlementAmountPaid() != null && request.getSettlementAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            LoanCollection collection = LoanCollection.builder()
                    .loan(loan)
                    .collectedBy(loan.getCreatedBy())
                    .collectedAmount(request.getSettlementAmountPaid())
                    .collectionDate(LocalDateTime.now())
                    .collectionMode(CollectionMode.CASH)
                    .collectionStatus(CollectionStatus.SUCCESS)
                    .receiptNumber("SPL-" + System.currentTimeMillis())
                    .remarks("SPECIAL CLOSURE SETTLEMENT")
                    .build();
            loanCollectionRepository.save(collection);

            // UPDATE DAYBOOK OF THE EMPLOYEE WHO CREATED THE LOAN
            if (loan.getCreatedBy() != null && loan.getCreatedBy().getRole().getRoleName().equalsIgnoreCase("EMPLOYEE")) {
                try {
                    dayBookService.addTransaction(loan.getCreatedBy().getId(), "COLLECTIONS", request.getSettlementAmountPaid(), "SPECIAL CLOSURE SETTLEMENT");
                } catch (Exception e) {
                    throw new RuntimeException("Could not add transaction to employee daybook: " + e.getMessage());
                }
            }
        }

        loan.setClosedSpecialCondition(true);
        loan.setSpecialClosureRemarks(request.getSpecialRemarks());

        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        BigDecimal remainingSettlement = request.getSettlementAmountPaid() != null ? request.getSettlementAmountPaid() : BigDecimal.ZERO;

        for (LoanRepaymentSchedule sch : schedules) {
            if (sch.getOutstandingAmount() != null && sch.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (remainingSettlement.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal amountToPay = sch.getOutstandingAmount().min(remainingSettlement);
                    if (sch.getPaidAmount() == null) sch.setPaidAmount(BigDecimal.ZERO);
                    sch.setPaidAmount(sch.getPaidAmount().add(amountToPay));
                    sch.setOutstandingAmount(sch.getOutstandingAmount().subtract(amountToPay));
                    if (sch.getDueAmount() != null) {
                        sch.setDueAmount(sch.getDueAmount().subtract(amountToPay).max(BigDecimal.ZERO));
                    }
                    remainingSettlement = remainingSettlement.subtract(amountToPay);
                }
                
                // If there's still outstanding amount left, we WAIVE it.
                if (sch.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
                    sch.setDueAmount(BigDecimal.ZERO);
                    sch.setOutstandingAmount(BigDecimal.ZERO);
                    sch.setRepaymentStatus(RepaymentStatus.WAIVED);
                } else {
                    sch.setRepaymentStatus(RepaymentStatus.PAID);
                }
                scheduleRepository.save(sch);
            }
        }

        LoanClosure closure = LoanClosure.builder()
                .loan(loan)
                .closureDate(LocalDateTime.now())
                .remarks("SPECIAL CONDITION CLOSURE: " + request.getSpecialRemarks() + " [Penalty Waived: " + loan.getPenaltyWaivedPercent() + "%]")
                .build();

        LoanClosure savedClosure = loanClosureRepository.save(closure);

        loan.setLoanStatus(LoanStatus.CLOSED);
        loanRepository.save(loan);

        auditLogService.log(currentUser.getId().toString(), "CLOSE_LOAN_SPECIAL", "LOAN", loan.getId().toString());
        return new LoanClosureResponse(true, "Loan closed successfully", null);
    }
"""
    with open('src/main/java/com/dapfintech/loan/service/impl/LoanPenaltyServiceImpl.java', 'w', encoding='utf-8') as f:
        f.writelines(lines[:start_idx])
        f.write(new_method)
        f.writelines(lines[end_idx+1:])
    print("Patched closeOnSpecialCondition successfully using python block replace.")
else:
    print("Failed to find bounds.")
