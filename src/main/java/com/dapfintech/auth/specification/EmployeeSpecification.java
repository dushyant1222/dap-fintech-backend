package com.dapfintech.auth.specification;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.dapfintech.auth.entity.User;
import com.dapfintech.common.enums.UserStatus;
import com.dapfintech.market.entity.EmployeeMarketAssignment;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class EmployeeSpecification {

    private EmployeeSpecification() {
    }

    public static Specification<User> isEmployee() {

        return (root, query, criteriaBuilder) ->

                criteriaBuilder.equal(
                        root.get("role").get("roleName"),
                        "EMPLOYEE"
                );
    }

    public static Specification<User> hasKeyword(
            String keyword
    ) {

        return (root, query, criteriaBuilder) -> {

            if (keyword == null ||
                    keyword.trim().isEmpty()) {

                return criteriaBuilder.conjunction();
            }

            String search =
                    "%" + keyword.trim().toLowerCase() + "%";

            Predicate namePredicate =
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("fullName")
                            ),
                            search
                    );

            Predicate mobilePredicate =
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("mobileNumber")
                            ),
                            search
                    );

            return criteriaBuilder.or(
                    namePredicate,
                    mobilePredicate
            );
        };
    }

    public static Specification<User> hasStatus(
            UserStatus status
    ) {

        return (root, query, criteriaBuilder) -> {

            if (status == null) {

                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("status"),
                    status
            );
        };
    }

    public static Specification<User> assignedToMarket(
            UUID marketId
    ) {

        return (root, query, criteriaBuilder) -> {

            if (marketId == null) {

                return criteriaBuilder.conjunction();
            }

            Subquery<UUID> subquery =
                    query.subquery(UUID.class);

            Root<EmployeeMarketAssignment> assignment =
                    subquery.from(
                            EmployeeMarketAssignment.class
                    );

            subquery.select(
                    assignment
                            .get("employee")
                            .get("id")
            );

            subquery.where(

                    criteriaBuilder.equal(
                            assignment
                                    .get("market")
                                    .get("id"),
                            marketId
                    ),

                    criteriaBuilder.isTrue(
                            assignment.get("isActive")
                    )
            );

            return root.get("id").in(subquery);
        };
    }
}