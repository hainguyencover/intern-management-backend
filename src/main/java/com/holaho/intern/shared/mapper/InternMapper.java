package com.holaho.intern.shared.mapper;

import com.holaho.intern.shared.dto.response.InternProfileResponse;
import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.intern.entity.InternProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InternMapper {

    @Mapping(target = "userId", source = "profile.user.id")
    @Mapping(target = "email", source = "profile.user.email")
    @Mapping(target = "fullName", source = "profile.user.fullName")
    @Mapping(target = "mentorId", source = "profile.mentor.id")
    @Mapping(target = "mentorName", source = "profile.mentor.user.fullName")
    @Mapping(target = "programGroupId", source = "member.group.id")
    @Mapping(target = "programGroupName", source = "member.group.name")
    @Mapping(target = "status", expression = "java(calculateStatus(profile, member))")
    @Mapping(target = "startDate", expression = "java(effectiveStartDate(profile, member))")
    @Mapping(target = "endDate", expression = "java(effectiveEndDate(profile, member))")
    @Mapping(target = "id", source = "profile.id")
    @Mapping(target = "phone", source = "profile.phone")
    @Mapping(target = "studentCode", source = "profile.studentCode")
    @Mapping(target = "dob", source = "profile.dob")
    @Mapping(target = "university", source = "profile.university")
    @Mapping(target = "major", source = "profile.major")
    @Mapping(target = "address", source = "profile.address")
    @Mapping(target = "gpa", source = "profile.gpa")
    @Mapping(target = "cvUrl", source = "profile.cvUrl")
    @Mapping(target = "createdAt", source = "profile.createdAt")
    @Mapping(target = "updatedAt", source = "profile.updatedAt")
    @Mapping(target = "cvSkills", source = "profile.cvSkills")
    @Mapping(target = "cvScore", source = "profile.cvScore")
    @Mapping(target = "cvSummary", source = "profile.cvSummary")
    InternProfileResponse toResponse(InternProfile profile, GroupMember member);

    default String calculateStatus(InternProfile profile, GroupMember member) {
        if (member != null && member.getGroup() != null) {
            if (member.getGroup().getProgram() != null
                    && "CLOSED".equals(member.getGroup().getProgram().getStatus().name())) {
                return "FINISHED";
            }
            return "ACTIVE";
        }
        LocalDate now = LocalDate.now();
        if (profile.getEndDate() != null && profile.getEndDate().isBefore(now)) {
            return "FINISHED";
        } else if (profile.getStartDate() != null && profile.getStartDate().isAfter(now)) {
            return "WAITING";
        } else {
            return "ACTIVE";
        }
    }

    default LocalDate effectiveStartDate(InternProfile profile, GroupMember member) {
        if (member != null && member.getGroup() != null && member.getGroup().getProgram() != null) {
            return member.getGroup().getProgram().getStartDate();
        }
        return profile.getStartDate();
    }

    default LocalDate effectiveEndDate(InternProfile profile, GroupMember member) {
        if (member != null && member.getGroup() != null && member.getGroup().getProgram() != null) {
            return member.getGroup().getProgram().getEndDate();
        }
        return profile.getEndDate();
    }
}

