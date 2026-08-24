package com.holaho.intern.mentor.service;

import com.holaho.intern.mentor.dto.*;
import com.holaho.intern.shared.dto.response.AssignMentorResponse;
import com.holaho.intern.shared.dto.response.MentorAssignmentResponse;
import com.holaho.intern.shared.dto.response.MentorCapacityResponse;

import java.util.List;

public interface MentorAssignmentService {
    MentorAssignmentResponse assignMentor(AssignMentorRequest request);
    MentorAssignmentResponse reassignMentor(Long assignmentId, ReassignMentorRequest request);
    MentorAssignmentResponse unassignMentor(Long assignmentId, UnassignMentorRequest request);
    MentorAssignmentResponse completeAssignment(Long assignmentId, String reason);
    MentorAssignmentResponse cancelAssignment(Long assignmentId, String reason);

    BulkAssignMentorResponse bulkAssignMentors(BulkAssignMentorRequest request);
    MentorSuggestionResponse suggestMentorsForIntern(Long internId);

    List<MentorAssignmentResponse> getInternAssignmentHistory(Long internId);
    List<MentorAssignmentResponse> getMentorActiveInterns(Long mentorId);
    MentorCapacityResponse getMentorCapacity(Long mentorId);

    // Legacy compatibility methods
    AssignMentorResponse assignMentorToIntern(Long internId, Long mentorId);
    AssignMentorResponse assignMentorToIntern(Long internId, Long mentorId, String reason);
    AssignMentorResponse removeMentorFromIntern(Long internId);
}
