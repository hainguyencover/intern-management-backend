package com.example.backend.repository;

import com.example.backend.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByIntern_IdAndDate(Long internId, LocalDate date);

    List<Attendance> findByIntern_IdAndDateBetween(Long internId, LocalDate from, LocalDate to);

    @Query("""
                SELECT a
                FROM Attendance a
                WHERE a.date BETWEEN :from AND :to
            """)
    List<Attendance> findAllInRange(LocalDate from, LocalDate to);

    @Query("""
                SELECT a
                FROM Attendance a
                WHERE a.intern.id = :internId
                  AND a.date BETWEEN :from AND :to
            """)
    List<Attendance> findByInternInRange(Long internId, LocalDate from, LocalDate to);
}
