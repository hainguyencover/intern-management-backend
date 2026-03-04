package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.InternProfileResponse;
import com.example.backend.service.InternProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller test
class InternControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InternProfileService internProfileService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void getInternById_Success() throws Exception {
        // Arrange
        InternProfileResponse response = new InternProfileResponse();
        response.setId(1L);
        response.setFullName("John Doe");
        response.setEmail("john@example.com");

        when(internProfileService.getInternProfileById(anyLong())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/interns/profiles/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("John Doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }
}
