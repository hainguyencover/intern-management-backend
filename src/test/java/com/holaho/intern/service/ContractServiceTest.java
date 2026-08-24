package com.holaho.intern.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.entity.InternshipContract;
import com.holaho.intern.intern.repository.InternshipContractRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.shared.dto.request.ContractRevisionRequest;
import com.holaho.intern.shared.dto.response.ContractResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.ContractStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.ForbiddenException;
import com.holaho.intern.shared.workflow.ApplicationStateMachine;
import com.holaho.intern.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private InternshipContractRepository contractRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ApplicationStateMachine stateMachine;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ContractService contractService;

    private User internUser;
    private InternProfile internProfile;
    private Application approvedApp;
    private MockMultipartFile validPdfFile;
    private MockMultipartFile invalidDocFile;

    @BeforeEach
    void setUp() {
        internUser = new User();
        internUser.setId(10L);
        internUser.setFullName("Nguyen Van Intern");
        internUser.setEmail("intern@holaho.com");

        internProfile = new InternProfile();
        internProfile.setId(100L);
        internProfile.setUser(internUser);

        approvedApp = new Application();
        approvedApp.setId(200L);
        approvedApp.setIntern(internProfile);
        approvedApp.setStatus(ApplicationStatus.APPROVED);

        validPdfFile = new MockMultipartFile(
                "file",
                "contract.pdf",
                "application/pdf",
                "%PDF-1.4 Dummy Content".getBytes()
        );

        invalidDocFile = new MockMultipartFile(
                "file",
                "contract.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Invalid doc content".getBytes()
        );
    }

    @Test
    @DisplayName("US-009: HR upload PDF thành công cho hồ sơ APPROVED")
    void shouldUploadContractSuccessfully() {
        // Given
        when(applicationRepository.findById(200L)).thenReturn(Optional.of(approvedApp));
        when(fileStorageService.store(any(), eq("contracts"))).thenReturn("contracts/uuid.pdf");

        InternshipContract mockSavedContract = InternshipContract.builder()
                .application(approvedApp)
                .fileUrl("contracts/uuid.pdf")
                .status(ContractStatus.SENT)
                .build();
        mockSavedContract.setId(500L);

        when(contractRepository.findByApplication_Id(200L)).thenReturn(Optional.empty());
        when(contractRepository.save(any(InternshipContract.class))).thenReturn(mockSavedContract);

        // When
        ContractResponse response = contractService.uploadContract(200L, validPdfFile, 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("SENT");
        verify(applicationRepository).save(approvedApp);
    }

    @Test
    @DisplayName("US-009: Từ chối upload contract nếu Application bị REJECTED")
    void shouldRejectUploadWhenApplicationRejected() {
        // Given
        approvedApp.setStatus(ApplicationStatus.REJECTED);
        when(applicationRepository.findById(200L)).thenReturn(Optional.of(approvedApp));

        // When / Then
        assertThatThrownBy(() -> contractService.uploadContract(200L, validPdfFile, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể gửi hợp đồng cho đơn ứng tuyển đã bị từ chối");
    }

    @Test
    @DisplayName("US-048: Cho phép Replace file PDF hợp đồng khi trạng thái chưa SIGNED")
    void shouldReplaceContractSuccessfullyWhenNotSigned() {
        // Given
        InternshipContract existingContract = InternshipContract.builder()
                .application(approvedApp)
                .fileUrl("contracts/old.pdf")
                .status(ContractStatus.SENT)
                .build();
        existingContract.setId(500L);

        when(applicationRepository.findById(200L)).thenReturn(Optional.of(approvedApp));
        when(contractRepository.findByApplication_Id(200L)).thenReturn(Optional.of(existingContract));
        when(fileStorageService.store(any(), eq("contracts"))).thenReturn("contracts/new.pdf");
        when(contractRepository.save(any(InternshipContract.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ContractResponse response = contractService.uploadContract(200L, validPdfFile, 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(existingContract.getFileUrl()).isEqualTo("contracts/new.pdf");
    }

    @Test
    @DisplayName("US-048: Từ chối Replace file hợp đồng khi trạng thái đã SIGNED")
    void shouldRejectReplacementWhenAlreadySigned() {
        // Given
        InternshipContract existingContract = InternshipContract.builder()
                .application(approvedApp)
                .fileUrl("contracts/signed.pdf")
                .status(ContractStatus.SIGNED)
                .build();
        existingContract.setId(500L);

        when(applicationRepository.findById(200L)).thenReturn(Optional.of(approvedApp));
        when(contractRepository.findByApplication_Id(200L)).thenReturn(Optional.of(existingContract));

        // When / Then
        assertThatThrownBy(() -> contractService.uploadContract(200L, validPdfFile, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Hợp đồng này đã được các bên ký hoàn tất (SIGNED)");
    }

    @Test
    @DisplayName("US-060: Intern gửi Yêu cầu chỉnh sửa Hợp đồng thành công")
    void shouldRequestRevisionSuccessfully() {
        // Given
        InternshipContract existingContract = InternshipContract.builder()
                .application(approvedApp)
                .fileUrl("contracts/v1.pdf")
                .status(ContractStatus.SENT)
                .build();
        existingContract.setId(500L);

        when(contractRepository.findById(500L)).thenReturn(Optional.of(existingContract));
        when(contractRepository.save(any(InternshipContract.class))).thenAnswer(i -> i.getArgument(0));

        ContractRevisionRequest req = new ContractRevisionRequest("Sai thông tin ngày bắt đầu");

        // When
        ContractResponse response = contractService.requestRevision(500L, 10L, req);

        // Then
        assertThat(response.getStatus()).isEqualTo("NEEDS_REVISION");
        assertThat(existingContract.getRevisionReason()).isEqualTo("Sai thông tin ngày bắt đầu");
        assertThat(existingContract.getRevisionRequestedAt()).isNotNull();
    }

    @Test
    @DisplayName("US-010 Security: Từ chối nếu Intern A cố tình xác nhận hợp đồng của Intern B")
    void shouldThrowForbiddenWhenInternConfirmsOtherContract() {
        // Given
        InternshipContract existingContract = InternshipContract.builder()
                .application(approvedApp)
                .status(ContractStatus.SENT)
                .build();
        existingContract.setId(500L);

        when(contractRepository.findById(500L)).thenReturn(Optional.of(existingContract));

        // When / Then (Intern User ID 999 attempting to confirm Intern User ID 10's contract)
        assertThatThrownBy(() -> contractService.confirmByIntern(500L, 999L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Bạn không có quyền xác nhận hợp đồng này");
    }
}
