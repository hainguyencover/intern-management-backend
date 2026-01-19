package com.example.backend.repository;

import com.example.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT r FROM Role r JOIN FETCH r.permissions WHERE r.code = :code")
    Optional<Role> findByCodeWithPermissions(@Param("code") String code);

    @Query("SELECT r FROM Role r JOIN FETCH r.permissions")
    List<Role> findAllWithPermissions();

    List<Role> findByCodeIn(List<String> codes);
}
