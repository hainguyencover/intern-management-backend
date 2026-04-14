package com.holaho.intern.shared.dto;

public record ProgramCompletionStatDto(
        Long programId,
        String programName,
        Long totalInterns,
        Long completedInterns,
        Double completionRate
) {
    public ProgramCompletionStatDto(Long programId, String programName, Long totalInterns, Long completedInterns) {
        this(programId, programName, totalInterns, completedInterns, 0D);
    }
}

