package com.holaho.intern.repository;

import com.holaho.intern.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Find department by name
     */
    Optional<Department> findByName(String name);

    @Query("SELECT d FROM Department d " +
            "WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(d.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Department> searchByKeyword(@Param("keyword") String keyword);

    List<Department> findAllByOrderByNameAsc();
}

