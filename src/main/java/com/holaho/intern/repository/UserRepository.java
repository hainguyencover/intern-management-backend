package com.holaho.intern.repository;

import com.holaho.intern.shared.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.holaho.intern.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
        Optional<User> findByEmail(String email);

        boolean existsByEmail(String email);

        List<User> findByStatus(UserStatus status);

        @Query("SELECT u FROM User u JOIN u.roles r WHERE r.code = :roleCode")
        List<User> findByRoleCode(@Param("roleCode") String roleCode);

        @Query("select case when count(r)>0 then true else false end from User u join u.roles r where u.id = :userId and (upper(r.code) = upper(:roleCode) or upper(r.code) = concat('ROLE_', upper(:roleCode)))")
        boolean existsByIdAndRoleCode(@Param("userId") Long userId, @Param("roleCode") String roleCode);

        @Query("SELECT u FROM User u JOIN u.roles r WHERE r.code = :roleCode AND u.status = :status")
        List<User> findByRoleCodeAndStatus(@Param("roleCode") String roleCode, @Param("status") UserStatus status);

        @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        @Query("SELECT u FROM User u JOIN u.roles r WHERE r.code IN :roleCodes")
        Page<User> findByRoleCodeIn(@Param("roleCodes") List<String> roleCodes, Pageable pageable);

        long countByStatus(UserStatus status);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "roles")
        @Query("SELECT u FROM User u WHERE (:keyword IS NULL OR :keyword = '' OR lower(u.fullName) LIKE lower(concat('%', :keyword, '%')) OR lower(u.email) LIKE lower(concat('%', :keyword, '%')))")
        org.springframework.data.domain.Page<User> findAllWithRoles(@Param("keyword") String keyword,
                        org.springframework.data.domain.Pageable pageable);

        @Query("SELECT u FROM User u WHERE " +
                        "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
                        "(:fullName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))) AND " +
                        "(:status IS NULL OR u.status = :status)")
        Page<User> searchUsers(
                        @Param("email") String email,
                        @Param("fullName") String fullName,
                        @Param("status") UserStatus status,
                        Pageable pageable);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "roles")
        @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE " +
                        "(:role IS NULL OR :role = '' OR r.code = :role) AND " +
                        "(:status IS NULL OR u.status = :status) AND " +
                        "(:keyword IS NULL OR :keyword = '' OR lower(u.fullName) LIKE lower(concat('%', :keyword, '%')) OR lower(u.email) LIKE lower(concat('%', :keyword, '%')))")
        Page<User> searchByRoleAndKeyword(@Param("role") String role, @Param("status") UserStatus status,
                        @Param("keyword") String keyword, Pageable pageable);
}

