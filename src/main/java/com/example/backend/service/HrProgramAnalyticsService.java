package com.example.backend.service;

import com.example.backend.dto.ProgramCompletionStatDto;
import com.example.backend.repository.ProgramRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HrProgramAnalyticsService {

    private final ProgramRepository programRepository;

    public HrProgramAnalyticsService(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    public List<ProgramCompletionStatDto> completionRateByProgram(String period, Long departmentId) {
        String p = (period == null || period.isBlank()) ? "FINAL" : period.trim();

        List<ProgramCompletionStatDto> rows = programRepository.rawCompletionByProgram(p, departmentId);

        return rows.stream().map(r -> {
            long total = r.totalInterns() == null ? 0L : r.totalInterns();
            long completed = r.completedInterns() == null ? 0L : r.completedInterns();
            double rate = (total == 0) ? 0.0 : (completed * 100.0 / total);

            return new ProgramCompletionStatDto(
                    r.programId(),
                    r.programName(),
                    total,
                    completed,
                    Math.round(rate * 100.0) / 100.0 // làm tròn 2 chữ số
            );
        }).toList();
    }
}
