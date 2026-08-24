package com.holaho.intern.mentor.controller;

import com.holaho.intern.mentor.dto.MentorWorkloadResponse;
import com.holaho.intern.mentor.dto.MentorWorkloadSummaryResponse;
import com.holaho.intern.mentor.service.MentorWorkloadService;
import com.holaho.intern.shared.enums.MentorWorkloadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MentorWorkloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MentorWorkloadService workloadService;

    @Test
    @WithMockUser(roles = "HR")
    void testGetWorkloads_AsHR_ShouldReturn200() throws Exception {
        MentorWorkloadResponse response = MentorWorkloadResponse.builder()
                .mentorId(1L)
                .mentorName("Nguyễn Văn A")
                .currentInternCount(8)
                .maxInternCapacity(10)
                .utilizationPercent(80)
                .workloadStatus(MentorWorkloadStatus.NEAR_CAPACITY)
                .build();

        when(workloadService.getWorkloads(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/hr/mentors/workloads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].mentorName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.content[0].workloadStatus").value("NEAR_CAPACITY"));
    }

    @Test
    @WithMockUser(roles = "MENTOR")
    void testGetWorkloads_AsMentor_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/mentors/workloads"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void testGetWorkloadSummary_AsHR_ShouldReturn200() throws Exception {
        MentorWorkloadSummaryResponse summary = MentorWorkloadSummaryResponse.builder()
                .totalMentors(10)
                .totalActiveInterns(45)
                .overloadCount(2)
                .build();

        when(workloadService.getWorkloadSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/hr/mentors/workloads/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalMentors").value(10))
                .andExpect(jsonPath("$.data.overloadCount").value(2));
    }
}
