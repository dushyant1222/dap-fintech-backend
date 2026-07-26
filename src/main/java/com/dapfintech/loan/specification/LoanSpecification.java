package com.dapfintech.loan.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.dapfintech.loan.dto.request.LoanFilterRequest;
import com.dapfintech.loan.entity.Loan;

import jakarta.persistence.criteria.Predicate;

public class LoanSpecification {

    private LoanSpecification() {
    }


    public static Specification<Loan> withFilters(
            LoanFilterRequest filter
    ) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates =
                    new ArrayList<>();


            //--------------------------------------------------
            // KEYWORD SEARCH
            //--------------------------------------------------

            if (filter.getKeyword() != null &&
                    !filter.getKeyword()
                            .trim()
                            .isEmpty()) {

                String keyword =
                        "%"
                                + filter
                                .getKeyword()
                                .trim()
                                .toLowerCase()
                                + "%";


                predicates.add(

                        criteriaBuilder.or(

                                criteriaBuilder.like(

                                        criteriaBuilder.lower(
                                                root
                                                        .get("customer")
                                                        .get("firstName")
                                        ),

                                        keyword

                                ),

                                criteriaBuilder.like(

                                        criteriaBuilder.lower(
                                                root
                                                        .get("customer")
                                                        .get("lastName")
                                        ),

                                        keyword

                                ),

                                criteriaBuilder.like(

                                        criteriaBuilder.lower(
                                                root
                                                        .get("customer")
                                                        .get("mobileNumber")
                                        ),

                                        keyword

                                ),

                                criteriaBuilder.like(

                                        criteriaBuilder.lower(
                                                root
                                                        .get("customer")
                                                        .get("customerCode")
                                        ),

                                        keyword

                                ),

                                criteriaBuilder.like(

                                        criteriaBuilder.lower(
                                                root
                                                        .get("createdBy")
                                                        .get("fullName")
                                        ),

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

                                root
                                        .get("createdBy")
                                        .get("id"),

                                filter.getEmployeeId()

                        )

                );

            }


            //--------------------------------------------------
            // MINIMUM AMOUNT
            //--------------------------------------------------

            if (filter.getMinAmount() != null) {

                predicates.add(

                        criteriaBuilder
                                .greaterThanOrEqualTo(

                                        root.get(
                                                "loanAmount"
                                        ),

                                        filter.getMinAmount()

                                )

                );

            }


            //--------------------------------------------------
            // MAXIMUM AMOUNT
            //--------------------------------------------------

            if (filter.getMaxAmount() != null) {

                predicates.add(

                        criteriaBuilder
                                .lessThanOrEqualTo(

                                        root.get(
                                                "loanAmount"
                                        ),

                                        filter.getMaxAmount()

                                )

                );

            }


            //--------------------------------------------------
            // FROM DATE
            //--------------------------------------------------

            if (filter.getFromDate() != null) {

                predicates.add(

                        criteriaBuilder
                                .greaterThanOrEqualTo(

                                        root.get(
                                                "applicationDate"
                                        ),

                                        filter
                                                .getFromDate()
                                                .atStartOfDay()

                                )

                );

            }


            //--------------------------------------------------
            // TO DATE
            //--------------------------------------------------

            if (filter.getToDate() != null) {

                predicates.add(

                        criteriaBuilder
                                .lessThan(

                                        root.get(
                                                "applicationDate"
                                        ),

                                        filter
                                                .getToDate()
                                                .plusDays(1)
                                                .atStartOfDay()

                                )

                );

            }


            return criteriaBuilder.and(

                    predicates.toArray(
                            new Predicate[0]
                    )

            );

        };

    }

}