package com.glassstore.lab.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.lab.entity.LabJob;
import com.glassstore.lab.entity.Prescription;
import com.glassstore.lab.repository.LabJobRepository;
import com.glassstore.lab.repository.LabJobStatusHistoryRepository;
import com.glassstore.lab.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LabJobServiceTest {

    private LabJobRepository labJobRepository;
    private PrescriptionRepository prescriptionRepository;
    private LabJobStatusHistoryRepository historyRepository;
    private LabJobService service;

    @BeforeEach
    void setUp() {
        labJobRepository = mock(LabJobRepository.class);
        prescriptionRepository = mock(PrescriptionRepository.class);
        historyRepository = mock(LabJobStatusHistoryRepository.class);
        service = new LabJobService(labJobRepository, prescriptionRepository, historyRepository);
        when(labJobRepository.findByOrderId(any())).thenReturn(Optional.empty());
        AtomicLong ids = new AtomicLong(1);
        when(labJobRepository.save(any(LabJob.class))).thenAnswer(inv -> {
            LabJob job = inv.getArgument(0);
            if (job.getId() == null) {
                job.setId(ids.getAndIncrement());
            }
            return job;
        });
    }

    @Test
    @DisplayName("Creating a job is idempotent per order (BR-29)")
    void idempotentPerOrder() {
        LabJob existing = LabJob.builder().id(4L).orderId(10L).build();
        when(labJobRepository.findByOrderId(10L)).thenReturn(Optional.of(existing));

        LabJob result = service.createFromOrder(10L, 1L, "F", "Frame", "L", "Lens", "Tech");
        assertThat(result.getId()).isEqualTo(4L);
        verify(prescriptionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Incomplete prescription cannot enter fabrication")
    void incompleteRx() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(Prescription.builder()
                .id(1L).customerId(1L).optometristId(1L)
                .odSph(null).osSph(new BigDecimal("-1.00")).pd(new BigDecimal("62.0"))
                .build()));

        assertThatThrownBy(() -> service.createFromOrder(10L, 1L, "F", "Frame", "L", "Lens", "Tech"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRESCRIPTION_INCOMPLETE);
    }

    @Test
    @DisplayName("QC failure without a reason is rejected (BR-32)")
    void qcReasonRequired() {
        LabJob job = LabJob.builder().id(1L).status("QC_CHECK").build();
        when(labJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.updateStatus(1L, "QC_FAILED", "  ", "Tech"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.LAB_JOB_QC_REASON_REQUIRED);
    }

    @Test
    @DisplayName("Skipping QC to handover is rejected")
    void cannotSkipQc() {
        LabJob job = LabJob.builder().id(1L).status("IN_PROGRESS").build();
        when(labJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.updateStatus(1L, "READY_FOR_DELIVERY", null, "Tech"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.LAB_JOB_INVALID_TRANSITION);
    }
}
