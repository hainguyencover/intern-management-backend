package com.example.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.backend.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select case when count(r)>0 then true else false end from User u join u.roles r where u.id = :userId and (upper(r.code) = upper(:roleCode) or upper(r.code) = concat('ROLE_', upper(:roleCode)))")
    boolean existsByIdAndRoleCode(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
