package com.holaho.intern.repository;

import com.holaho.intern.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {

    List<ApplicationStatusHistory> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);

    @Query("SELECT ash FROM ApplicationStatusHistory ash LEFT JOIN FETCH ash.changedBy WHERE ash.application.id = :applicationId ORDER BY ash.createdAt ASC")
    List<ApplicationStatusHistory> findByApplicationIdWithUser(@Param("applicationId") Long applicationId);
}
