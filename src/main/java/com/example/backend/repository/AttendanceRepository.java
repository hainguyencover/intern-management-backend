package com.example.backend.repository;

import com.example.backend.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

        Optional<Attendance> findByIntern_IdAndDate(Long internId, LocalDate date);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "intern", "intern.user" })
        @Query("SELECT a FROM Attendance a WHERE a.intern.id = :internId " +
                        "AND a.date BETWEEN :fromDate AND :toDate ORDER BY a.date DESC")
        Page<Attendance> findByInternAndDateRange(
                        @Param("internId") Long internId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        Pageable pageable);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "intern", "intern.user" })
        @Query("SELECT a FROM Attendance a WHERE a.date BETWEEN :fromDate AND :toDate")
        Page<Attendance> findByDateRange(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        Pageable pageable);

        List<Attendance> findByIntern_Id(Long internId);

        @Query("SELECT a FROM Attendance a WHERE a.intern.id = :internId AND a.date BETWEEN :from AND :to")
        List<Attendance> findByInternIdAndDateRange(@Param("internId") Long internId, @Param("from") LocalDate from,
                        @Param("to") LocalDate to);

        @Query("SELECT a FROM Attendance a WHERE a.date BETWEEN :from AND :to")
        List<Attendance> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

        List<Attendance> findByInternId(Long internId);

        List<Attendance> findByInternIdOrderByDateDesc(Long internId);

        Page<Attendance> findByInternId(Long internId, Pageable pageable);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "intern", "intern.user" })
        Optional<Attendance> findByInternIdAndDate(Long internId, LocalDate date);

        boolean existsByInternIdAndDate(Long internId, LocalDate date);

        @Query("SELECT a FROM Attendance a " +
                        "WHERE a.intern.id = :internId " +
                        "AND a.date BETWEEN :startDate AND :endDate " +
                        "ORDER BY a.date DESC")
        List<Attendance> findByInternIdAndDateBetween(
                        @Param("internId") Long internId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT a FROM Attendance a " +
                        "WHERE a.date BETWEEN :startDate AND :endDate")
        List<Attendance> findByDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(a.totalMinutes) FROM Attendance a " +
                        "WHERE a.intern.id = :internId " +
                        "AND a.date BETWEEN :startDate AND :endDate")
        Long sumTotalMinutesByInternIdAndDateRange(
                        @Param("internId") Long internId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT COUNT(a) FROM Attendance a " +
                        "WHERE a.intern.id = :internId " +
                        "AND a.date BETWEEN :startDate AND :endDate")
        long countByInternIdAndDateBetween(
                        @Param("internId") Long internId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT a FROM Attendance a " +
                        "WHERE a.date = :date")
        List<Attendance> findByDate(@Param("date") LocalDate date);
}
