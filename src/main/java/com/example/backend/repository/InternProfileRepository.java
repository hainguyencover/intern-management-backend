package com.example.backend.repository;

import com.example.backend.dto.InternCountStatDto;
import com.example.backend.entity.InternProfile;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternProfileRepository
        extends JpaRepository<InternProfile, Long>, JpaSpecificationExecutor<InternProfile> {

    @Override
    @EntityGraph(attributePaths = { "user", "mentor", "mentor.user" })
    Page<InternProfile> findAll(Specification<InternProfile> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "user" })
    Optional<InternProfile> findById(Long id);

    Optional<InternProfile> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    Optional<InternProfile> findByUser_Email(String email);

    Optional<InternProfile> findByStudentCode(String studentCode);

    @Query("SELECT DISTINCT i.university FROM InternProfile i ORDER BY i.university")
    List<String> findAllUniversities();

    @Query("SELECT ip FROM InternProfile ip " +
            "JOIN ip.user u " +
            "WHERE (:university IS NULL OR LOWER(ip.university) LIKE LOWER(CONCAT('%', :university, '%'))) "
            +
            "AND (:major IS NULL OR LOWER(ip.major) LIKE LOWER(CONCAT('%', :major, '%'))) " +
            "AND (:gpaMin IS NULL OR ip.gpa >= :gpaMin) " +
            "AND (:gpaMax IS NULL OR ip.gpa <= :gpaMax) " +
            "AND (:keyword IS NULL OR " +
            "     LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(ip.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<InternProfile> searchInterns(
            @Param("university") String university,
            @Param("major") String major,
            @Param("gpaMin") Double gpaMin,
            @Param("gpaMax") Double gpaMax,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT COUNT(DISTINCT ip) FROM InternProfile ip " +
            "LEFT JOIN GroupMember gm ON gm.intern = ip AND gm.leftAt IS NULL " +
            "LEFT JOIN gm.group pg " +
            "WHERE ip.mentor.id = :mentorId OR pg.mentorId = :mentorId")
    long countByMentorId(@Param("mentorId") Long mentorId);

    @Query("SELECT DISTINCT i.major FROM InternProfile i ORDER BY i.major")
    List<String> findAllMajors();

    @Query("SELECT COUNT(i) FROM InternProfile i WHERE i.university = :university")
    long countByUniversity(@Param("university") String university);

    @Query("SELECT COUNT(i) FROM InternProfile i WHERE i.major = :major")
    long countByMajor(@Param("major") String major);

    @Query("""
                select new com.example.backend.dto.InternCountStatDto(
                    coalesce(ip.university, 'Chưa cập nhật'),
                    count(ip.id)
                )
                from InternProfile ip
                group by ip.university
                order by count(ip.id) desc
            """)
    List<InternCountStatDto> countInternsGroupedByUniversity();

    @Query("""
                select new com.example.backend.dto.InternCountStatDto(
                    coalesce(ip.major, 'Chưa cập nhật'),
                    count(ip.id)
                )
                from InternProfile ip
                group by ip.major
                order by count(ip.id) desc
            """)
    List<InternCountStatDto> countInternsGroupedByMajor();

    // ✅ Thống kê theo trường + ngành
    @Query("""
                select new com.example.backend.dto.InternCountStatDto(
                    concat(coalesce(ip.university, 'Chưa cập nhật'), ' - ', coalesce(ip.major, 'Chưa cập nhật')),
                    count(ip.id)
                )
                from InternProfile ip
                group by ip.university, ip.major
                order by count(ip.id) desc
            """)
    List<InternCountStatDto> countByUniversityAndMajor();

    boolean existsByStudentCode(String studentCode);

    @Query("SELECT i FROM InternProfile i JOIN FETCH i.user WHERE i.id = :id")
    Optional<InternProfile> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT i FROM InternProfile i JOIN FETCH i.user u WHERE i.university = :university")
    List<InternProfile> findByUniversity(@Param("university") String university);

    @Query("SELECT i FROM InternProfile i JOIN FETCH i.user u WHERE i.major = :major")
    List<InternProfile> findByMajor(@Param("major") String major);

    @Query("SELECT i FROM InternProfile i JOIN FETCH i.user u " +
            "WHERE i.university = :university AND i.major = :major")
    List<InternProfile> findByUniversityAndMajor(
            @Param("university") String university,
            @Param("major") String major);

    @Query("SELECT i FROM InternProfile i JOIN FETCH i.user u " +
            "WHERE i.gpa >= :minGpa AND i.gpa <= :maxGpa")
    List<InternProfile> findByGpaRange(
            @Param("minGpa") Double minGpa,
            @Param("maxGpa") Double maxGpa);

    @Query("SELECT i FROM InternProfile i JOIN i.user u " +
            "WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<InternProfile> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT i FROM InternProfile i WHERE i.mentor.id = :mentorId")
    List<InternProfile> findByMentorId(@Param("mentorId") Long mentorId);

    List<InternProfile> findByMentor_Id(Long mentorId);

    @Query("SELECT i FROM InternProfile i JOIN FETCH i.user WHERE i.dob IS NOT NULL AND MONTH(i.dob) = :month ORDER BY DAY(i.dob)")
    List<InternProfile> findByBirthdayMonth(@Param("month") int month);
}
