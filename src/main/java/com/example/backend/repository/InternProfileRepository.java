package com.example.backend.repository;

import com.example.backend.entity.InternProfile;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InternProfileRepository
        extends JpaRepository<InternProfile, Long>, JpaSpecificationExecutor<InternProfile> {

    @Override
    @EntityGraph(attributePaths = { "user" })
    Page<InternProfile> findAll(Specification<InternProfile> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "user" })
    Optional<InternProfile> findById(Long id);

    @EntityGraph(attributePaths = { "user" })
    Optional<InternProfile> findByUser_Id(Long userId);

    @EntityGraph(attributePaths = { "user" })
    Optional<InternProfile> findByUser_Email(String email);

    @Query("select distinct ip.university from InternProfile ip where ip.university is not null")
    List<String> findAllUniversities();

    @Query("select distinct ip.major from InternProfile ip where ip.major is not null")
    List<String> findAllMajors();
}