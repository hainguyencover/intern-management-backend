package com.example.backend.service;

import com.example.backend.dto.response.AssignMentorResponse;

public interface MentorAssignmentService {
    AssignMentorResponse assignMentorToIntern(Long internId, Long mentorId);
    AssignMentorResponse removeMentorFromIntern(Long internId);
}
