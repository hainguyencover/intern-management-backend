package com.example.backend.mapper;

import com.example.backend.dto.response.EvaluationResponse;
import com.example.backend.entity.Evaluation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EvaluationMapper {

    @Mapping(target = "internId", source = "intern.id")
    @Mapping(target = "internName", source = "intern.user.fullName")
    @Mapping(target = "mentorId", source = "mentor.id")
    @Mapping(target = "mentorName", source = "mentor.user.fullName")
    EvaluationResponse toResponse(Evaluation evaluation);
}
