package com.glassstore.lab.service;

import com.glassstore.lab.dto.CreateLabJobRequest;
import com.glassstore.lab.dto.LabJobResponse;
import com.glassstore.lab.dto.UpdateJobStatusRequest;
import com.glassstore.lab.entity.LabJob;
import com.glassstore.lab.entity.LabJobStatus;
import com.glassstore.lab.entity.Prescription;
import com.glassstore.lab.repository.LabJobRepository;
import com.glassstore.lab.repository.PrescriptionRepository;
import com.glassstore.lab.service.impl.LabJobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabJobServiceTest {

    @Mock
    private LabJobRepository labJobRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private LabJobServiceImpl labJobService;

    private Prescription prescription;
    private LabJob pendingJob;

    @BeforeEach
    void setUp() {
        prescription = Prescription.builder()
                .id(100L)
                .customerId(10L)
                .customerName("Nguyễn Hoàng Duy")
                .optometristId(6L)
                .optometristName("BS. Lê Thị Ánh Tuyết")
                .odSph(new BigDecimal("-2.25"))
                .odCyl(new BigDecimal("-0.75"))
                .odAxis(175)
                .odAdd(BigDecimal.ZERO)
                .osSph(new BigDecimal("-2.00"))
                .osCyl(new BigDecimal("-0.50"))
                .osAxis(180)
                .osAdd(BigDecimal.ZERO)
                .pd(new BigDecimal("63.5"))
                .notes("Cận loạn")
                .build();

        pendingJob = LabJob.builder()
                .id(1L)
                .orderId(501L)
                .prescriptionId(100L)
                .customerName("Nguyễn Hoàng Duy")
                .lensSku("LENS-ESSILOR-160")
                .frameSku("FRAME-RAYBAN-001")
                .status(LabJobStatus.PENDING)
                .createdAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    @Test
    @DisplayName("getLabJobs: Trả về danh sách rỗng khi không có bản ghi nào, không ném lỗi")
    void getLabJobs_WhenEmpty_ShouldReturnEmptyList() {
        when(labJobRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

        List<LabJobResponse> result = labJobService.getLabJobs(null);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(prescriptionRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("getLabJobs: Lấy danh sách kèm thông số độ khúc xạ từ PrescriptionRepository")
    void getLabJobs_WithData_ShouldMapRefractionParameters() {
        when(labJobRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(pendingJob));
        when(prescriptionRepository.findAllById(Set.of(100L))).thenReturn(List.of(prescription));

        List<LabJobResponse> result = labJobService.getLabJobs(null);

        assertThat(result).hasSize(1);
        LabJobResponse item = result.get(0);
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getStatus()).isEqualTo(LabJobStatus.PENDING);
        assertThat(item.getOdSphere()).isEqualTo(new BigDecimal("-2.25"));
        assertThat(item.getOdCyl()).isEqualTo(new BigDecimal("-0.75"));
        assertThat(item.getOdAxis()).isEqualTo(175);
        assertThat(item.getOsSphere()).isEqualTo(new BigDecimal("-2.00"));
        assertThat(item.getOsCyl()).isEqualTo(new BigDecimal("-0.50"));
        assertThat(item.getOsAxis()).isEqualTo(180);
        assertThat(item.getPd()).isEqualTo(new BigDecimal("63.5"));
    }

    @Test
    @DisplayName("getLabJobs: Lọc theo status")
    void getLabJobs_WithStatusFilter_ShouldCallFindByStatus() {
        when(labJobRepository.findByStatusOrderByCreatedAtDesc(LabJobStatus.PENDING))
                .thenReturn(List.of(pendingJob));
        when(prescriptionRepository.findAllById(Set.of(100L))).thenReturn(List.of(prescription));

        List<LabJobResponse> result = labJobService.getLabJobs(LabJobStatus.PENDING);

        assertThat(result).hasSize(1);
        verify(labJobRepository).findByStatusOrderByCreatedAtDesc(LabJobStatus.PENDING);
        verify(labJobRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("createLabJob: Tạo mới lệnh mài với status ban đầu là PENDING")
    void createLabJob_ShouldCreateWithStatusPending() {
        CreateLabJobRequest request = CreateLabJobRequest.builder()
                .orderId(502L)
                .customerName("Trần Văn A")
                .prescriptionId(100L)
                .lensSku("LENS-HOYA-167")
                .frameSku("FRAME-GUCCI-99")
                .build();

        when(labJobRepository.save(any(LabJob.class))).thenAnswer(invocation -> {
            LabJob entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });
        when(prescriptionRepository.findById(100L)).thenReturn(Optional.of(prescription));

        LabJobResponse response = labJobService.createLabJob(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(LabJobStatus.PENDING);
        assertThat(response.getOrderId()).isEqualTo(502L);
        assertThat(response.getLensSku()).isEqualTo("LENS-HOYA-167");
        assertThat(response.getOdSphere()).isEqualTo(new BigDecimal("-2.25"));
    }

    @Test
    @DisplayName("updateJobStatus: Chuyển sang IN_PROGRESS thì gán startedAt và technicianId")
    void updateJobStatus_ToInProgress_ShouldSetStartedAtAndTechnicianId() {
        when(labJobRepository.findById(1L)).thenReturn(Optional.of(pendingJob));
        when(labJobRepository.save(any(LabJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(prescriptionRepository.findById(100L)).thenReturn(Optional.of(prescription));

        UpdateJobStatusRequest request = UpdateJobStatusRequest.builder()
                .status(LabJobStatus.IN_PROGRESS)
                .technicianId(25L)
                .technicianName("Nguyễn Kỹ Thuật")
                .notes("Bắt đầu mài tròng")
                .build();

        LabJobResponse response = labJobService.updateJobStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(LabJobStatus.IN_PROGRESS);
        assertThat(response.getStartedAt()).isNotNull();
        assertThat(response.getTechnicianId()).isEqualTo(25L);
        assertThat(response.getTechnicianNotes()).isEqualTo("Bắt đầu mài tròng");
    }

    @Test
    @DisplayName("updateJobStatus: Chuyển sang QC_PASSED thì gán completedAt")
    void updateJobStatus_ToQcPassed_ShouldSetCompletedAt() {
        pendingJob.setStatus(LabJobStatus.QC_PENDING);
        when(labJobRepository.findById(1L)).thenReturn(Optional.of(pendingJob));
        when(labJobRepository.save(any(LabJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(prescriptionRepository.findById(100L)).thenReturn(Optional.of(prescription));

        UpdateJobStatusRequest request = UpdateJobStatusRequest.builder()
                .status(LabJobStatus.QC_PASSED)
                .qcNotes("Mài lắp chuẩn tâm, không trầy xước")
                .build();

        LabJobResponse response = labJobService.updateJobStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(LabJobStatus.QC_PASSED);
        assertThat(response.getCompletedAt()).isNotNull();
        assertThat(response.getQcNotes()).isEqualTo("Mài lắp chuẩn tâm, không trầy xước");
    }

    @Test
    @DisplayName("updateJobStatus: Chuyển sang QC_FAILED mà không có qcNotes thì ném IllegalArgumentException")
    void updateJobStatus_ToQcFailed_WithoutNotes_ShouldThrowIllegalArgumentException() {
        when(labJobRepository.findById(1L)).thenReturn(Optional.of(pendingJob));

        UpdateJobStatusRequest request = UpdateJobStatusRequest.builder()
                .status(LabJobStatus.QC_FAILED)
                .qcNotes("") // rỗng
                .build();

        assertThatThrownBy(() -> labJobService.updateJobStatus(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("qcNotes");

        verify(labJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateJobStatus: Chuyển sang QC_FAILED có qcNotes hợp lệ thì thành công")
    void updateJobStatus_ToQcFailed_WithValidNotes_ShouldSucceed() {
        when(labJobRepository.findById(1L)).thenReturn(Optional.of(pendingJob));
        when(labJobRepository.save(any(LabJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(prescriptionRepository.findById(100L)).thenReturn(Optional.of(prescription));

        UpdateJobStatusRequest request = UpdateJobStatusRequest.builder()
                .status(LabJobStatus.QC_FAILED)
                .qcNotes("Bị mẻ góc mép tròng khi mài vát")
                .build();

        LabJobResponse response = labJobService.updateJobStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(LabJobStatus.QC_FAILED);
        assertThat(response.getQcNotes()).isEqualTo("Bị mẻ góc mép tròng khi mài vát");
    }
}

