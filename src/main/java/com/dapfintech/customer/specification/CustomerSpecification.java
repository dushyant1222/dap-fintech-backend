package com.dapfintech.customer.specification;

import org.springframework.data.jpa.domain.Specification;

import com.dapfintech.customer.dto.request.CustomerFilterRequest;
import com.dapfintech.customer.entity.Customer;

public final class CustomerSpecification {

    private CustomerSpecification() {
    }

    public static Specification<Customer> withFilters(
            CustomerFilterRequest filter
    ) {

        return (root, query, criteriaBuilder) -> {

            var predicate =
                    criteriaBuilder.conjunction();

            //--------------------------------------------------
            // NO FILTER REQUEST
            //--------------------------------------------------

            if (filter == null) {
                return predicate;
            }

            //--------------------------------------------------
            // KEYWORD SEARCH
            //--------------------------------------------------

            if (filter.getKeyword() != null &&
                    !filter.getKeyword()
                            .trim()
                            .isEmpty()) {

                String keyword =
                        "%"
                                + filter.getKeyword()
                                .trim()
                                .toLowerCase()
                                + "%";

                predicate =
                        criteriaBuilder.and(
                                predicate,

                                criteriaBuilder.or(

                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(
                                                        root.get(
                                                                "firstName"
                                                        )
                                                ),
                                                keyword
                                        ),

                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(
                                                        root.get(
                                                                "lastName"
                                                        )
                                                ),
                                                keyword
                                        ),

                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(
                                                        root.get(
                                                                "mobileNumber"
                                                        )
                                                ),
                                                keyword
                                        ),

                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(
                                                        root.get(
                                                                "customerCode"
                                                        )
                                                ),
                                                keyword
                                        ),

                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(
                                                        root.get(
                                                                "email"
                                                        )
                                                ),
                                                keyword
                                        )

                                )
                        );
            }

            //--------------------------------------------------
            // STATUS FILTER
            //--------------------------------------------------

            if (filter.getStatus() != null) {

                predicate =
                        criteriaBuilder.and(
                                predicate,

                                criteriaBuilder.equal(
                                        root.get("status"),
                                        filter.getStatus()
                                )
                        );
            }

            //--------------------------------------------------
            // MARKET FILTER
            //--------------------------------------------------

            if (filter.getMarketId() != null) {

                predicate =
                        criteriaBuilder.and(
                                predicate,

                                criteriaBuilder.equal(
                                        root.get("market")
                                                .get("id"),

                                        filter.getMarketId()
                                )
                        );
            }

            return predicate;
        };
    }
}