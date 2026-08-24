package com.holaho.intern.evaluation.repository;

import com.holaho.intern.evaluation.entity.FinalReportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinalReportItemRepository extends JpaRepository<FinalReportItem, Long> {
    List<FinalReportItem> findByReportIdOrderByDisplayOrderAsc(Long reportId);
}
