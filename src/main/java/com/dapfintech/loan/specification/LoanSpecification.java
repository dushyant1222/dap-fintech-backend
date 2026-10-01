package com.dapfintech.loan.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.dapfintech.auth.entity.User;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.loan.dto.request.LoanFilterRequest;
import com.dapfintech.loan.entity.Loan;
import com.dapfintech.market.entity.Market;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class LoanSpecification {

    private LoanSpecification() {
    }

    public static Specification<Loan> withFilters(
            LoanFilterRequest filter
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Explicit LEFT JOINs to avoid dropping loans with null createdBy or null customer/market
            Join<Loan, Customer> customerJoin = root.join("customer", JoinType.LEFT);
            Join<Customer, Market> marketJoin = customerJoin.join("market", JoinType.LEFT);
            Join<Loan, User> createdByJoin = root.join("createdBy", JoinType.LEFT);

            //--------------------------------------------------
            // KEYWORD SEARCH
            //--------------------------------------------------
            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                String keyword = "%" + filter.getKeyword().trim().toLowerCase() + "%";

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("loanCode"), "")),
                                        keyword
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(criteriaBuilder.coalesce(customerJoin.get("firstName"), "")),
                                        keyword
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(criteriaBuilder.coalesce(customerJoin.get("lastName"), "")),
                                        keyword
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                criteriaBuilder.concat(
                                                        criteriaBuilder.concat(
                                                                criteriaBuilder.coalesce(customerJoin.get("firstName"), ""),
                                                                " "
                                                        ),
                                                        criteriaBuilder.coalesce(customerJoin.get("lastName"), "")
                                                )
                                        ),
                                        keyword
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(criteriaBuilder.coalesce(customerJoin.get("mobileNumber"), "")),
                                        keyword
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(criteriaBuilder.coalesce(customerJoin.get("customerCode"), "")),
                                        keyword
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(criteriaBuilder.coalesce(marketJoin.get("marketName"), "")),
                                        keyword
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(criteriaBuilder.coalesce(createdByJoin.get("fullName"), "")),
                                        keyword
                                )
                        )
                );
            }

            //--------------------------------------------------
            // STATUS
            //--------------------------------------------------
            if (filter.getStatus() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("loanStatus"),
                                filter.getStatus()
                        )
                );
            }

            //--------------------------------------------------
            // LOAN TYPE
            //--------------------------------------------------
            if (filter.getLoanType() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("loanType"),
                                filter.getLoanType()
                        )
                );
            }

            //--------------------------------------------------
            // EMPLOYEE
            //--------------------------------------------------
            if (filter.getEmployeeId() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                createdByJoin.get("id"),
                                filter.getEmployeeId()
                        )
                );
            }

            //--------------------------------------------------
            // MARKET
            //--------------------------------------------------
            if (filter.getMarketId() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                marketJoin.get("id"),
                                filter.getMarketId()
                        )
                );
            }

            //--------------------------------------------------
            // MINIMUM AMOUNT
            //--------------------------------------------------
            if (filter.getMinAmount() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("loanAmount"),
                                filter.getMinAmount()
                        )
                );
            }

            //--------------------------------------------------
            // MAXIMUM AMOUNT
            //--------------------------------------------------
            if (filter.getMaxAmount() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("loanAmount"),
                                filter.getMaxAmount()
                        )
                );
            }

            //--------------------------------------------------
            // FROM DATE
            //--------------------------------------------------
            if (filter.getFromDate() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("applicationDate"),
                                filter.getFromDate().atStartOfDay()
                        )
                );
            }

            //--------------------------------------------------
            // TO DATE
            //--------------------------------------------------
            if (filter.getToDate() != null) {
                predicates.add(
                        criteriaBuilder.lessThan(
                                root.get("applicationDate"),
                                filter.getToDate().plusDays(1).atStartOfDay()
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}