package com.holaho.intern.shared.elasticsearch.repository;

import com.holaho.intern.shared.elasticsearch.model.InternIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternSearchRepository extends ElasticsearchRepository<InternIndex, String> {
    List<InternIndex> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrStudentCodeContainingIgnoreCase(
            String fullName, String email, String studentCode);
}

