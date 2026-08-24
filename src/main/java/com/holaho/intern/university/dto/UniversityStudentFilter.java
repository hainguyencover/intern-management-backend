package com.holaho.intern.university.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversityStudentFilter {
    private String keyword;
    private String status;
    private String major;
    private Double progressMin;
    private Double progressMax;
}
