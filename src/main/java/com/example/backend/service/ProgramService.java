package com.example.backend.service;

import com.example.backend.dto.request.ProgramCreateRequest;
import com.example.backend.dto.request.ProgramUpdateRequest;
import com.example.backend.dto.response.ProgramResponse;
import com.example.backend.dto.request.ProgramUpsertRequest;
import com.example.backend.entity.Department;
import com.example.backend.entity.Program;
import com.example.backend.enums.ProgramStatus;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.DepartmentRepository;
import com.example.backend.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;

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


    @Transactional
    public ProgramResponse createProgram(ProgramCreateRequest req) {
        Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));

        validateDate(req.getStartDate(), req.getEndDate());

        Program program = new Program();
        program.setDepartment(dept);
        program.setName(req.getName().trim());
        program.setDescription(req.getDescription());
        program.setStartDate(req.getStartDate());
        program.setEndDate(req.getEndDate());
        program.setStatus(ProgramStatus.DRAFT);

        Program saved = programRepository.save(program);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProgramResponse> listPrograms(Long departmentId, ProgramStatus status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);

        Page<Program> result;
        if (departmentId != null && status != null) {
            result = programRepository.findAllByDepartment_IdAndStatus(departmentId, status, pr);
        } else if (departmentId != null) {
            result = programRepository.findAllByDepartment_Id(departmentId, pr);
        } else if (status != null) {
            result = programRepository.findAllByStatus(status, pr);
        } else {
            result = programRepository.findAll(pr);
        }

        return result.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProgramResponse getProgram(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
        return toResponse(program);
    }

    @Transactional
    public ProgramResponse updateProgram(Long id, ProgramUpdateRequest req) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        if (req.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
            program.setDepartment(dept);
        }

        validateDate(req.getStartDate(), req.getEndDate());

        program.setName(req.getName().trim());
        program.setDescription(req.getDescription());
        program.setStartDate(req.getStartDate());
        program.setEndDate(req.getEndDate());

        return toResponse(programRepository.save(program));
    }

    @Transactional
    // Optional: publish DRAFT -> ACTIVE (vì enum hiện tại là DRAFT/ACTIVE/CLOSED)
    public ProgramResponse publishProgram(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        if (program.getStatus() != ProgramStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DRAFT program can be published");
        }
        program.setStatus(ProgramStatus.ACTIVE);

        return toResponse(programRepository.save(program));
    }

    private void validateDate(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be <= endDate");
        }
    }

    private ProgramResponse toResponse(Program p) {
        return new ProgramResponse(
                p.getId(),
                p.getDepartment() != null ? p.getDepartment().getId() : null,
                p.getDepartment() != null ? p.getDepartment().getName() : "Unknown Department",
                p.getName(),
                p.getDescription(),
                p.getStartDate(),
                p.getEndDate(),
                p.getStatus()
        );
    }
}
