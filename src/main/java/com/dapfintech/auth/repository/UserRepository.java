package com.dapfintech.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dapfintech.auth.entity.User;
import com.dapfintech.common.enums.UserStatus;

@Repository
public interface UserRepository
        extends JpaRepository<User, UUID>,
                JpaSpecificationExecutor<User> {

    Optional<User> findByMobileNumber(
            String mobileNumber
    );

    List<User> findByOnesignalId(
            String onesignalId
    );

    Optional<User> findByEmail(
            String email
    );

    Optional<User> findByEmailIgnoreCase(
            String email
    );

    List<User> findAllByEmailIgnoreCase(
            String email
    );

    Boolean existsByMobileNumber(
            String mobileNumber
    );

    List<User> findByRoleRoleName(
            String roleName
    );
    
    long countByRoleRoleName(String roleName);

    Page<User> findByRoleRoleName(
            String roleName,
            Pageable pageable
    );

    Long countByStatus(
            UserStatus status
    );
    Optional<User> findByIdAndRoleRoleName(
	        UUID id,
	        String roleName
	);

    @Query(value = """
            SELECT * FROM users
            WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email))
               OR LOWER(CONCAT(mobile_number, '@dapfintech.com')) = LOWER(TRIM(:email))
            LIMIT 1
            """, nativeQuery = true)
    Optional<User> findByEmailOrMobileEmail(@Param("email") String email);
}