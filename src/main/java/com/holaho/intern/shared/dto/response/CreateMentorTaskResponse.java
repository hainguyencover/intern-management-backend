package com.holaho.intern.shared.dto.response;

import java.util.List;

public record CreateMentorTaskResponse(
        int created,
        List<Long> taskIds
) {}

