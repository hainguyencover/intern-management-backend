package com.holaho.intern.service;

import com.holaho.intern.entity.Department;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.entity.Program;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.repository.ProgramRepository;


import com.holaho.intern.shared.dto.request.CreateProgramRequest;
import com.holaho.intern.shared.dto.request.UpdateProgramTimelineRequest;
import com.holaho.intern.shared.dto.response.ProgramResponse;
import com.holaho.intern.shared.dto.response.ProgramTimelineResponse;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
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
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return programRepository.search(departmentId, status, cleanKeyword, pageable)
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
        // Đếm groups và interns
        response.setTotalGroups((long) groupRepository.findByProgramId(id).size());
        response.setTotalInterns(memberRepository.countByGroup_ProgramId(id));
        return response;
    }

    @Transactional
    public ProgramResponse createProgram(CreateProgramRequest request) {
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new NotFoundException("Department", request.getDepartmentId()));

        if (request.getStartDate() != null && request.getEndDate() != null && !request.getStartDate().isBefore(request.getEndDate())) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        String code = request.getCode();
        if (code == null || code.isBlank()) {
            code = "PROG-" + System.currentTimeMillis() % 100000;
        } else if (programRepository.existsByCode(code)) {
            throw new BadRequestException("Mã chương trình '" + code + "' đã tồn tại");
        }

        Program program = new Program();
        program.setDepartment(department);
        program.setCode(code);
        program.setName(request.getName());
        program.setDescription(request.getDescription());
        program.setStartDate(request.getStartDate());
        program.setEndDate(request.getEndDate());
        program.setMaxInterns(request.getMaxInterns());
        program.setStatus(ProgramStatus.ACTIVE);

        program = programRepository.save(program);
        log.info("Created program: {} ({})", program.getName(), program.getCode());

        return mapToResponse(program);
    }

    @Transactional
    public ProgramResponse updateProgram(Long id, CreateProgramRequest request) {
        Program program = programRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> new NotFoundException("Program", id));

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new NotFoundException("Department", request.getDepartmentId()));
            program.setDepartment(department);
        }

        if (request.getCode() != null && !request.getCode().isBlank()) {
            if (programRepository.existsByCodeAndIdNot(request.getCode(), id)) {
                throw new BadRequestException("Mã chương trình '" + request.getCode() + "' đã tồn tại");
            }
            program.setCode(request.getCode());
        }

        if (request.getName() != null) {
            program.setName(request.getName());
        }
        if (request.getDescription() != null) {
            program.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null || request.getEndDate() != null) {
            validateProgramCanChangeTimeline(program);
            if (request.getStartDate() != null) {
                program.setStartDate(request.getStartDate());
            }
            if (request.getEndDate() != null) {
                program.setEndDate(request.getEndDate());
            }
        }
        if (request.getMaxInterns() != null) {
            program.setMaxInterns(request.getMaxInterns());
        }

        if (program.getStartDate() != null && program.getEndDate() != null && !program.getStartDate().isBefore(program.getEndDate())) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        program = programRepository.save(program);
        log.info("Updated program: {}", id);

        return mapToResponse(program);
    }

    @Transactional
    public ProgramResponse updateProgramTimeline(Long id, UpdateProgramTimelineRequest request) {
        Program program = programRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> new NotFoundException("Program", id));

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("Ngày bắt đầu và ngày kết thúc không được để trống");
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        validateProgramCanChangeTimeline(program);

        program.setStartDate(request.getStartDate());
        program.setEndDate(request.getEndDate());

        program = programRepository.save(program);
        log.info("Updated timeline for program {}: {} -> {}", id, request.getStartDate(), request.getEndDate());

        return mapToResponse(program);
    }

    private void validateProgramCanChangeTimeline(Program program) {
        long activeInternCount = memberRepository.countByGroup_ProgramId(program.getId());
        if (activeInternCount > 0 && program.getStatus() == ProgramStatus.ACTIVE) {
            throw new ConflictException("Không thể thay đổi thời gian chương trình khi đã có thực tập sinh đang tham gia");
        }
    }

    @Transactional(readOnly = true)
    public Page<ProgramResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return programRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public ProgramResponse updateProgramStatus(Long id, ProgramStatus newStatus) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program", id));

        ProgramStatus current = program.getStatus();

        if (current == newStatus) {
            return mapToResponse(program);
        }

        if (current == ProgramStatus.COMPLETED || current == ProgramStatus.CANCELLED || current == ProgramStatus.ARCHIVED) {
            throw new BadRequestException("Không thể chuyển trạng thái cho chương trình đã " + current.name());
        }

        if (current == ProgramStatus.DRAFT) {
            if (newStatus != ProgramStatus.UPCOMING && newStatus != ProgramStatus.ACTIVE && newStatus != ProgramStatus.CANCELLED) {
                throw new BadRequestException("Trạng thái DRAFT chỉ có thể chuyển sang UPCOMING, ACTIVE hoặc CANCELLED");
            }
        } else if (current == ProgramStatus.UPCOMING) {
            if (newStatus != ProgramStatus.ACTIVE && newStatus != ProgramStatus.CANCELLED) {
                throw new BadRequestException("Trạng thái UPCOMING chỉ có thể chuyển sang ACTIVE hoặc CANCELLED");
            }
        } else if (current == ProgramStatus.ACTIVE) {
            if (newStatus != ProgramStatus.COMPLETED && newStatus != ProgramStatus.CANCELLED && newStatus != ProgramStatus.CLOSED) {
                throw new BadRequestException("Trạng thái ACTIVE chỉ có thể chuyển sang COMPLETED hoặc CANCELLED");
            }
        }

        program.setStatus(newStatus);
        program = programRepository.save(program);

        log.info("Updated program {} status from {} to {}", id, current, newStatus);
        return mapToResponse(program);
    }

    @Transactional(readOnly = true)
    public ProgramResponse getProgramById(Long id) {
        Program program = programRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> new NotFoundException("Program", id));
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
                .orElseThrow(() -> new NotFoundException("Program", id));

        programRepository.delete(program);
        log.info("Deleted program: {}", id);
    }

    private ProgramResponse mapToResponse(Program program) {
        ProgramResponse response = new ProgramResponse();
        response.setId(program.getId());
        response.setCode(program.getCode());
        response.setName(program.getName());
        response.setDepartmentId(program.getDepartment() != null ? program.getDepartment().getId() : null);
        response.setDepartmentName(program.getDepartment() != null ? program.getDepartment().getName() : null);
        response.setDescription(program.getDescription());
        response.setStartDate(program.getStartDate());
        response.setEndDate(program.getEndDate());
        response.setStatus(ProgramStatus.valueOf(program.getStatus().name()));
        response.setCreatedAt(program.getCreatedAt());
        response.setUpdatedAt(program.getUpdatedAt());
        return response;
    }

    @Transactional(readOnly = true)
    public com.holaho.intern.shared.dto.response.ProgramCapacityResponse getProgramCapacity(Long programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program", programId));

        long currentCount = memberRepository.countByGroup_ProgramId(programId);
        Integer max = program.getMaxInterns();
        Integer available = max != null ? Math.max(0, max - (int) currentCount) : null;
        boolean isFull = max != null && currentCount >= max;
        double pct = max != null && max > 0 ? ((double) currentCount / max) * 100.0 : 0.0;
        boolean nearCapacity = pct >= 80.0 && !isFull;

        return com.holaho.intern.shared.dto.response.ProgramCapacityResponse.builder()
                .programId(program.getId())
                .programName(program.getName())
                .programCode(program.getCode())
                .currentInterns(currentCount)
                .maxInterns(max)
                .availableSlots(available)
                .full(isFull)
                .nearCapacity(nearCapacity)
                .capacityPercentage(pct)
                .build();
    }

    @Transactional(readOnly = true)
    public ProgramTimelineResponse getProgramTimelineDetails(Long programId) {
        Program program = programRepository.findByIdWithDepartment(programId)
                .orElseThrow(() -> new NotFoundException("Program", programId));

        long durationDays = 0;
        if (program.getStartDate() != null && program.getEndDate() != null) {
            durationDays = java.time.temporal.ChronoUnit.DAYS.between(program.getStartDate(), program.getEndDate());
        }

        long internCount = memberRepository.countByGroup_ProgramId(programId);

        java.time.LocalDate today = java.time.LocalDate.now();
        boolean warningNearingEnd = program.getStatus() == ProgramStatus.ACTIVE
                && program.getEndDate() != null
                && !today.isBefore(program.getEndDate().minusDays(7))
                && !today.isAfter(program.getEndDate());

        return ProgramTimelineResponse.builder()
                .programId(program.getId())
                .programName(program.getName())
                .programCode(program.getCode())
                .departmentId(program.getDepartment() != null ? program.getDepartment().getId() : null)
                .departmentName(program.getDepartment() != null ? program.getDepartment().getName() : null)
                .startDate(program.getStartDate())
                .endDate(program.getEndDate())
                .durationDays(durationDays)
                .status(program.getStatus())
                .internCount(internCount)
                .warningNearingEnd(warningNearingEnd)
                .build();
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void autoManageProgramLifecycle() {
        java.time.LocalDate today = java.time.LocalDate.now();

        // 1. UPCOMING -> ACTIVE when startDate <= today
        List<Program> upcomingPrograms = programRepository.findByStatus(ProgramStatus.UPCOMING, Pageable.unpaged()).getContent();
        int activatedCount = 0;
        for (Program p : upcomingPrograms) {
            if (p.getStartDate() != null && !today.isBefore(p.getStartDate()) && (p.getEndDate() == null || !today.isAfter(p.getEndDate()))) {
                p.setStatus(ProgramStatus.ACTIVE);
                programRepository.save(p);
                activatedCount++;
            }
        }

        // 2. ACTIVE -> CLOSED when today > endDate
        List<Program> expiredPrograms = programRepository.findByStatusAndEndDateBefore(ProgramStatus.ACTIVE, today);
        int closedCount = 0;
        for (Program p : expiredPrograms) {
            p.setStatus(ProgramStatus.CLOSED);
            programRepository.save(p);
            closedCount++;
        }

        if (activatedCount > 0 || closedCount > 0) {
            log.info("Auto-managed program lifecycle: {} activated, {} closed", activatedCount, closedCount);
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 8 * * ?") // Runs daily at 8 AM
    @Transactional(readOnly = true)
    public void notifyNearingEndPrograms() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate warningThreshold = today.plusDays(7);

        List<Program> activePrograms = programRepository.findByStatus(ProgramStatus.ACTIVE, Pageable.unpaged()).getContent();
        for (Program p : activePrograms) {
            if (p.getEndDate() != null && !p.getEndDate().isBefore(today) && !p.getEndDate().isAfter(warningThreshold)) {
                long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, p.getEndDate());
                log.warn("WARNING: Program '{}' ({}) is ending in {} days on {}", p.getName(), p.getCode(), daysRemaining, p.getEndDate());
            }
        }
    }
}
