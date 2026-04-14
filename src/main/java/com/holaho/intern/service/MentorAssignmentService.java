package com.holaho.intern.service;

import com.holaho.intern.shared.dto.response.AssignMentorResponse;

public interface MentorAssignmentService {
    AssignMentorResponse assignMentorToIntern(Long internId, Long mentorId);
    AssignMentorResponse removeMentorFromIntern(Long internId);
}

