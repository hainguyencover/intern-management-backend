package com.holaho.intern.controller;

import com.holaho.intern.intern.controller.InternController;
import com.holaho.intern.intern.service.InternProfileService;
import com.holaho.intern.service.AuditLogService;
import com.holaho.intern.shared.dto.request.InternProfileRequest;
import com.holaho.intern.shared.dto.response.AuditLogResponse;
import com.holaho.intern.shared.dto.response.InternProfileResponse;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller test
class InternControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InternProfileService internProfileService;

    @MockitoBean
    private AuditLogService auditLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void getInternById_Success_US047() throws Exception {
        // Arrange
        InternProfileResponse response = new InternProfileResponse();
        response.setId(1L);
        response.setFullName("Nguyễn Văn A");
        response.setEmail("anv@holaho.vn");
        response.setUniversity("Đại học Bách Khoa Hà Nội");
        response.setMajor("Khoa học Máy tính");
        response.setStatus("DRAFT");

        when(internProfileService.getInternProfileById(anyLong())).thenReturn(response);

        // Act & Assert (US-047 Detail View)
        mockMvc.perform(get("/api/v1/interns/profiles/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.university").value("Đại học Bách Khoa Hà Nội"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @WithMockUser
    void getInternHistory_Success_US048() throws Exception {
        // Arrange
        AuditLogResponse log1 = new AuditLogResponse();
        log1.setId(101L);
        log1.setAction("CREATE");
        log1.setActorEmail("admin@holaho.vn");
        log1.setMessage("Khởi tạo hồ sơ thực tập sinh trạng thái DRAFT");

        when(auditLogService.getEntityHistory(eq("INTERN_PROFILE"), eq(1L))).thenReturn(List.of(log1));

        // Act & Assert (US-048 Profile Audit History)
        mockMvc.perform(get("/api/v1/interns/profiles/1/history"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].action").value("CREATE"))
                .andExpect(jsonPath("$.data[0].actorEmail").value("admin@holaho.vn"));
    }

    @Test
    @WithMockUser
    void createIntern_NewEmail_AutoProvisionsUser_Returns201() throws Exception {
        // Arrange
        InternProfileRequest request = new InternProfileRequest();
        request.setEmail("duchai123@gmail.com");
        request.setFullName("Đức Hải");
        request.setUniversity("HUST");
        request.setMajor("CS");

        InternProfileResponse response = new InternProfileResponse();
        response.setId(10L);
        response.setFullName("Đức Hải");
        response.setEmail("duchai123@gmail.com");
        response.setStatus("DRAFT");

        when(internProfileService.createIntern(any(InternProfileRequest.class))).thenReturn(response);

        // Act & Assert (Auto-provision User & Create Profile)
        mockMvc.perform(post("/api/v1/interns/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("duchai123@gmail.com"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @WithMockUser
    void updateIntern_LockedStatusCompleted_Returns409() throws Exception {
        // Arrange
        InternProfileRequest request = new InternProfileRequest();
        request.setUniversity("UET");
        request.setMajor("IT");

        when(internProfileService.updateIntern(eq(1L), any(InternProfileRequest.class)))
                .thenThrow(new ConflictException("Hồ sơ đã hoàn thành và không thể chỉnh sửa."));

        // Act & Assert (US-002 Status Lock Gate)
        mockMvc.perform(put("/api/v1/interns/profiles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Hồ sơ đã hoàn thành và không thể chỉnh sửa."));
    }

    @Test
    @WithMockUser
    void updateIntern_LockedStatusRejected_Returns409_BR05() throws Exception {
        // Arrange
        InternProfileRequest request = new InternProfileRequest();
        request.setUniversity("UET");

        when(internProfileService.updateIntern(eq(1L), any(InternProfileRequest.class)))
                .thenThrow(new ConflictException("Hồ sơ đã bị từ chối và không thể chỉnh sửa. Theo BR-05, ứng viên cần khởi tạo hồ sơ mới."));

        // Act & Assert (BR-05 Status Lock Gate)
        mockMvc.perform(put("/api/v1/interns/profiles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Hồ sơ đã bị từ chối và không thể chỉnh sửa. Theo BR-05, ứng viên cần khởi tạo hồ sơ mới."));
    }
}
