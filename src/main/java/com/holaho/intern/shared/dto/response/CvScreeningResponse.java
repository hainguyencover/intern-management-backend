package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvScreeningResponse {
    private List<String> skills;
    private Integer score;
    private String summary;
    private String recommendation;
}

