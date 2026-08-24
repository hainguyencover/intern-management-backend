package com.holaho.intern.mentor.service;

import com.holaho.intern.mentor.dto.MentorMatchFilterRequest;
import com.holaho.intern.mentor.dto.MentorMatchResultResponse;

import java.util.List;

public interface MentorMatchingService {

    List<MentorMatchResultResponse> matchMentorsForHr(Long tenantId, MentorMatchFilterRequest filter);
}
