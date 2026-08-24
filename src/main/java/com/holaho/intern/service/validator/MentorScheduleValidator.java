package com.holaho.intern.service.validator;

import com.holaho.intern.entity.Program;
import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.shared.enums.GroupStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MentorScheduleValidator {

    private final ProgramGroupRepository groupRepository;

    public void validateMentorSchedule(Long mentorId, String workDaysStr, LocalTime start,
                                       LocalTime end, Program currentProgram, Long excludeGroupId) {
        if (mentorId == null || !StringUtils.hasText(workDaysStr) || start == null || end == null) {
            return;
        }

        List<ProgramGroup> activeGroups = groupRepository.findByMentorIdAndStatus(mentorId, GroupStatus.ACTIVE);

        for (ProgramGroup g : activeGroups) {
            if (excludeGroupId != null && g.getId().equals(excludeGroupId)) {
                continue;
            }

            // Must overlap in DATE (Program duration) first
            if (currentProgram != null && g.getProgram() != null && !hasDateOverlap(
                    currentProgram.getStartDate(), currentProgram.getEndDate(),
                    g.getProgram().getStartDate(), g.getProgram().getEndDate())) {
                continue; // Different periods -> No conflict
            }

            if (g.getWorkDays() == null || g.getWorkStartTime() == null || g.getWorkEndTime() == null) {
                continue;
            }

            if (hasDayOverlap(workDaysStr, g.getWorkDays())
                    && hasTimeOverlap(start, end, g.getWorkStartTime(), g.getWorkEndTime())) {
                throw new BadRequestException(
                        "Mentor đã có lịch dạy tại nhóm: " + g.getName() + " (Chương trình: " + g.getProgram().getName()
                                + ")");
            }
        }
    }

    private boolean hasDayOverlap(String days1, String days2) {
        String[] d1 = days1.split(",");
        String[] d2 = days2.split(",");
        for (String s1 : d1) {
            for (String s2 : d2) {
                if (s1.trim().equalsIgnoreCase(s2.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private boolean hasDateOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        if (start1 == null && end1 == null)
            return true;
        if (start2 == null && end2 == null)
            return true;

        LocalDate s1 = start1 != null ? start1 : LocalDate.MIN;
        LocalDate e1 = end1 != null ? end1 : LocalDate.MAX;
        LocalDate s2 = start2 != null ? start2 : LocalDate.MIN;
        LocalDate e2 = end2 != null ? end2 : LocalDate.MAX;

        return s1.isBefore(e2) && s2.isBefore(e1);
    }
}
