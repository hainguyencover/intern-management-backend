package com.holaho.intern.service;

import com.holaho.intern.entity.Department;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.entity.Program;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.repository.ProgramRepository;


import com.holaho.intern.shared.dto.request.CreateProgramRequest;
import com.holaho.intern.shared.dto.response.ProgramResponse;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramService {

    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Page<ProgramResponse> search(Long departmentId, ProgramStatus status,
            String keyword, Pageable pageable) {
        return programRepository.search(departmentId, status, keyword, pageable)
                .map(p -> {
                    ProgramResponse res = new ProgramResponse(p);
                    res.setTotalGroups((long) groupRepository.findByProgramId(p.getId()).size());
                    return res;
                });
    }

    @Transactional(readOnly = true)
    public ProgramResponse getById(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found with id: " + id));
        ProgramResponse response = new ProgramResponse(program);
        // Ãƒâ€žÃ‚ÂÃƒÂ¡Ã‚ÂºÃ‚Â¿m groups vÃƒÆ’Ã‚Â  interns
        response.setTotalGroups((long) groupRepository.findByProgramId(id).size());
        response.setTotalInterns(memberRepository.countByGroup_ProgramId(id));
        return response;
    }

    @Transactional
    public ProgramResponse createProgram(CreateProgramRequest request) {
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found: " + request.getDepartmentId()));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Start date must be before end date");
        }

        Program program = new Program();
        program.setDepartment(department);
        program.setName(request.getName());
        program.setDescription(request.getDescription());
        program.setStartDate(request.getStartDate());
        program.setEndDate(request.getEndDate());
        program.setStatus(ProgramStatus.ACTIVE);

        program = programRepository.save(program);
        log.info("Created program: {}", program.getName());

        return mapToResponse(program);
    }

    @Transactional
    public ProgramResponse updateProgram(Long id, CreateProgramRequest request) {
        Program program = programRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> new RuntimeException("Program not found: " + id));

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + request.getDepartmentId()));
            program.setDepartment(department);
        }

        if (request.getName() != null) {
            program.setName(request.getName());
        }
        if (request.getDescription() != null) {
            program.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            program.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            program.setEndDate(request.getEndDate());
        }

        if (program.getStartDate().isAfter(program.getEndDate())) {
            throw new RuntimeException("Start date must be before end date");
        }

        program = programRepository.save(program);
        log.info("Updated program: {}", id);

        return mapToResponse(program);
    }

    @Transactional(readOnly = true)
    public Page<ProgramResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return programRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public ProgramResponse updateProgramStatus(Long id, ProgramStatus status) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program not found: " + id));

        program.setStatus(status);
        program = programRepository.save(program);

        log.info("Updated program {} status to {}", id, status);
        return mapToResponse(program);
    }

    @Transactional(readOnly = true)
    public ProgramResponse getProgramById(Long id) {
        Program program = programRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> new RuntimeException("Program not found: " + id));
        return mapToResponse(program);
    }

    @Transactional(readOnly = true)
    public Page<ProgramResponse> getAllPrograms(Pageable pageable) {
        return programRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProgramResponse> getProgramsByStatus(ProgramStatus status, Pageable pageable) {
        return programRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> getProgramsByDepartmentId(Long departmentId) {
        return programRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> getCurrentlyActivePrograms() {
        return programRepository.findCurrentlyActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteProgram(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program not found: " + id));

        programRepository.delete(program);
        log.info("Deleted program: {}", id);
    }

    private ProgramResponse mapToResponse(Program program) {
        ProgramResponse response = new ProgramResponse();
        response.setId(program.getId());
        response.setName(program.getName());
        response.setDepartmentId(program.getDepartment().getId());
        response.setDepartmentName(program.getDepartment().getName());
        response.setDescription(program.getDescription());
        response.setStartDate(program.getStartDate());
        response.setEndDate(program.getEndDate());
        response.setStatus(ProgramStatus.valueOf(program.getStatus().name()));
        response.setCreatedAt(program.getCreatedAt());
        response.setUpdatedAt(program.getUpdatedAt());
        return response;
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void autoCloseExpiredPrograms() {
        List<Program> expiredPrograms = programRepository.findByStatusAndEndDateBefore(ProgramStatus.ACTIVE,
                java.time.LocalDate.now());

        int count = 0;
        for (Program p : expiredPrograms) {
            p.setStatus(ProgramStatus.CLOSED);
            programRepository.save(p);
            count++;
        }

        if (count > 0) {
            log.info("Auto-closed {} expired programs", count);
        }
    }
}

