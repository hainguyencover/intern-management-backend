package com.example.backend.repository;

import com.example.backend.entity.Application;
import com.example.backend.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // để mapping app.getIntern().getUser() không bị lazy/N+1 quá nặng
    @EntityGraph(attributePaths = {"intern", "intern.user"})
    Page<Application> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"intern", "intern.user"})
    Page<Application> findAllByStatus(ApplicationStatus status, Pageable pageable);

    List<Application> findByIntern_Id(Long internId);

    boolean existsByIntern_IdAndStatusIn(Long internId, Collection<ApplicationStatus> statuses);
}
