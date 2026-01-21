package com.example.backend.repository;

import com.example.backend.dto.response.MentorWorkloadResponse;
import com.example.backend.entity.Mentor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MentorRepository extends JpaRepository<Mentor, Long> {
    Optional<Mentor> findByUser_Id(Long userId);
    Optional<Mentor> findByUser_Email(String email);
    boolean existsByUser_Id(Long userId);

    Page<Mentor> findAll(Pageable pageable);
    @Query("""
        SELECT new com.example.backend.dto.response.MentorWorkloadResponse(
            m.id,
            u.email,
            u.fullName,
            u.phone,
            m.title,
            d.name,
            COUNT(DISTINCT i.id)
        )
        FROM Mentor m
        JOIN m.user u
        LEFT JOIN m.department d
        LEFT JOIN ProgramGroup pg ON pg.mentorId = m.id
        LEFT JOIN GroupMember gm ON gm.group = pg AND gm.leftAt IS NULL
        LEFT JOIN gm.intern i
        WHERE (:search IS NULL OR :search = '' OR
               LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        GROUP BY m.id, u.email, u.fullName, u.phone, m.title, d.name
        """)
    Page<MentorWorkloadResponse> findMentorWorkload(
            @Param("search") String search,
            Pageable pageable
    );
}
