package com.example.backend.service;

import com.example.backend.dto.response.ProgramResponse;
import com.example.backend.dto.request.ProgramUpsertRequest;
import com.example.backend.entity.Program;
import com.example.backend.enums.ProgramStatus;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;

    @Transactional
    public ProgramResponse create(ProgramUpsertRequest req) {
        validateDates(req.startDate(), req.endDate());

        Program p = new Program();
        p.setName(req.name());
        p.setDescription(req.description());
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        p.setStatus(req.status() != null ? req.status() : ProgramStatus.DRAFT);

        p = programRepository.save(p);
        return toResponse(p);
    }

    @Transactional
    public ProgramResponse update(Long id, ProgramUpsertRequest req) {
        validateDates(req.startDate(), req.endDate());

        Program p = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found: " + id));

        // rule MVP: FINISHED không cho sửa
        if (p.getStatus() == ProgramStatus.CLOSED) {
            throw new BadRequestException("Program is CLOSED, cannot be updated.");
        }

        p.setName(req.name());
        p.setDescription(req.description());
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        if (req.status() != null) p.setStatus(req.status());

        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public ProgramResponse get(Long id) {
        return programRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Program not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> list() {
        return programRepository.findAll().stream().map(this::toResponse).toList();
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BadRequestException("startDate/endDate is required");
        }
        if (start.isAfter(end)) {
            throw new BadRequestException("startDate must be <= endDate");
        }
    }

    private ProgramResponse toResponse(Program p) {
        return new ProgramResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getStartDate(),
                p.getEndDate(),
                p.getStatus()
        );
    }
}
