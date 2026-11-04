package com.glassstore.lab.service.impl;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.lab.domain.LabJobStatus;
import com.glassstore.lab.dto.CreateLabJobRequest;
import com.glassstore.lab.dto.LabJobResponse;
import com.glassstore.lab.dto.UpdateJobStatusRequest;
import com.glassstore.lab.entity.LabJob;
import com.glassstore.lab.entity.LabJobStatusHistory;
import com.glassstore.lab.entity.Prescription;
import com.glassstore.lab.repository.LabJobRepository;
import com.glassstore.lab.repository.LabJobStatusHistoryRepository;
import com.glassstore.lab.repository.PrescriptionRepository;
import com.glassstore.lab.service.LabJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabJobServiceImpl implements LabJobService {

    private final LabJobRepository labJobRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LabJobStatusHistoryRepository historyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LabJobResponse> getLabJobs(com.glassstore.lab.entity.LabJobStatus status) {
        List<LabJob> jobs = status == null
                ? labJobRepository.findAllByOrderByCreatedAtDesc()
                : labJobRepository.findByStatusOrderByCreatedAtDesc(status);
        if (jobs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> prescriptionIds = jobs.stream()
                .map(LabJob::getPrescriptionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Prescription> prescriptions = prescriptionIds.isEmpty()
                ? Collections.emptyMap()
                : prescriptionRepository.findAllById(prescriptionIds).stream()
                .collect(Collectors.toMap(Prescription::getId, prescription -> prescription, (left, right) -> left));

        return jobs.stream()
                .map(job -> LabJobResponse.from(job, prescriptions.get(job.getPrescriptionId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LabJobResponse getLabJobById(Long id) {
        LabJob job = findJob(id);
        Prescription prescription = job.getPrescriptionId() == null
                ? null
                : prescriptionRepository.findById(job.getPrescriptionId()).orElse(null);
        return LabJobResponse.from(job, prescription);
    }

    @Override
    @Transactional
    public LabJobResponse createLabJob(CreateLabJobRequest request) {
        return createFromOrder(
                request.getOrderId(),
                request.getPrescriptionId(),
                request.getFrameSku(),
                request.getFrameName(),
                request.getLensSku(),
                request.getLensName(),
                request.getTechnicianName(),
                request.getTechnicianId(),
                request.getTechnicianNotes(),
                request.getCustomerName());
    }

    @Override
    @Transactional
    public LabJobResponse createFromOrder(
            Long orderId,
            Long prescriptionId,
            String frameSku,
            String frameName,
            String lensSku,
            String lensName,
            String technicianName) {
        return createFromOrder(
                orderId, prescriptionId, frameSku, frameName, lensSku, lensName,
                technicianName, null, null, null);
    }

    private LabJobResponse createFromOrder(
            Long orderId,
            Long prescriptionId,
            String frameSku,
            String frameName,
            String lensSku,
            String lensName,
            String technicianName,
            Long technicianId,
            String technicianNotes,
            String customerName) {
        if (orderId == null) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Thiếu mã đơn hàng để tạo lệnh gia công");
        }
        if (prescriptionId == null) {
            throw ApiException.of(ErrorCode.PRESCRIPTION_INCOMPLETE,
                    "Thiếu toa kính đã xác nhận để tạo lệnh gia công");
        }

        var existing = labJobRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            return getResponse(existing.get());
        }

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> ApiException.of(ErrorCode.PRESCRIPTION_NOT_FOUND,
                        "Không tìm thấy toa kính ID: " + prescriptionId));
        requireCompletePrescription(prescription);

        String resolvedCustomerName = customerName == null || customerName.isBlank()
                ? prescription.getCustomerName()
                : customerName;
        LabJob job = LabJob.builder()
                .orderId(orderId)
                .prescriptionId(prescriptionId)
                .customerName(resolvedCustomerName)
                .frameSku(frameSku)
                .frameName(frameName)
                .lensSku(lensSku)
                .lensName(lensName)
                .technicianId(technicianId)
                .technicianName(technicianName)
                .technicianNotes(technicianNotes)
                .status(com.glassstore.lab.entity.LabJobStatus.PENDING)
                .build();

        LabJob saved = labJobRepository.save(job);
        recordStatus(saved, null, saved.getStatus().name(), technicianName, "Tạo lệnh gia công từ đơn hàng");
        log.info("Created lab job {} for order {} using prescription {}",
                saved.getId(), orderId, prescriptionId);
        return LabJobResponse.from(saved, prescription);
    }

    @Override
    @Transactional
    public LabJobResponse updateJobStatus(Long id, UpdateJobStatusRequest request) {
        LabJob job = findJob(id);
        com.glassstore.lab.entity.LabJobStatus requestedStatus = request.getStatus();
        if (requestedStatus == null) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Trạng thái cập nhật không được để trống");
        }

        LabJobStatus current = toWorkflowStatus(job.getStatus());
        LabJobStatus target = toWorkflowStatus(requestedStatus);
        if (current == target || !current.canTransitionTo(target)) {
            throw ApiException.of(ErrorCode.LAB_JOB_INVALID_TRANSITION,
                    "Không thể chuyển lệnh gia công từ " + job.getStatus() + " sang " + requestedStatus);
        }

        String qcNotes = request.getEffectiveQcNotes();
        if ((target.requiresReasonToEnter()) && (qcNotes == null || qcNotes.isBlank())) {
            throw ApiException.of(ErrorCode.LAB_JOB_QC_REASON_REQUIRED,
                    "Phải ghi rõ lý do khi đánh giá QC không đạt hoặc hủy lệnh");
        }

        LocalDateTime now = LocalDateTime.now();
        if (requestedStatus == com.glassstore.lab.entity.LabJobStatus.IN_PROGRESS) {
            job.setStartedAt(now);
            String technicianNotes = request.getEffectiveTechnicianNotes();
            if (technicianNotes != null) {
                job.setTechnicianNotes(technicianNotes);
            }
        }
        if (requestedStatus == com.glassstore.lab.entity.LabJobStatus.QC_PASSED
                || requestedStatus == com.glassstore.lab.entity.LabJobStatus.DELIVERED) {
            job.setCompletedAt(now);
        }
        if (qcNotes != null) {
            job.setQcNotes(qcNotes);
        }
        if (request.getTechnicianId() != null) {
            job.setTechnicianId(request.getTechnicianId());
        }
        if (request.getTechnicianName() != null && !request.getTechnicianName().isBlank()) {
            job.setTechnicianName(request.getTechnicianName());
        }
        job.setStatus(requestedStatus);
        job.setUpdatedAt(now);

        LabJob saved = labJobRepository.save(job);
        recordStatus(saved, current.name(), target.name(), request.getTechnicianName(), qcNotes);
        return getResponse(saved);
    }

    private LabJob findJob(Long id) {
        return labJobRepository.findById(id)
                .orElseThrow(() -> ApiException.of(ErrorCode.LAB_JOB_NOT_FOUND,
                        "Không tìm thấy lệnh mài kính ID: " + id));
    }

    private LabJobResponse getResponse(LabJob job) {
        Prescription prescription = job.getPrescriptionId() == null
                ? null
                : prescriptionRepository.findById(job.getPrescriptionId()).orElse(null);
        return LabJobResponse.from(job, prescription);
    }

    private void recordStatus(
            LabJob job, String fromStatus, String toStatus, String actor, String reason) {
        historyRepository.save(LabJobStatusHistory.builder()
                .labJobId(job.getId())
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actor(actor)
                .reason(reason)
                .build());
    }

    private LabJobStatus toWorkflowStatus(com.glassstore.lab.entity.LabJobStatus status) {
        return switch (status) {
            case PENDING -> LabJobStatus.WAITING_PROCESS;
            case QC_PENDING -> LabJobStatus.QC_CHECK;
            default -> LabJobStatus.valueOf(status.name());
        };
    }

    private void requireCompletePrescription(Prescription prescription) {
        if (prescription.getOdSph() == null || prescription.getOsSph() == null || prescription.getPd() == null) {
            throw ApiException.of(ErrorCode.PRESCRIPTION_INCOMPLETE,
                    "Toa kính thiếu SPH hoặc PD bắt buộc, không thể đưa vào gia công");
        }
        validateAxis(prescription.getOdAxis(), "OD");
        validateAxis(prescription.getOsAxis(), "OS");
        checkRange(prescription.getOdSph(), -30, 30, "OD SPH");
        checkRange(prescription.getOsSph(), -30, 30, "OS SPH");
        checkRange(prescription.getOdCyl(), -10, 10, "OD CYL");
        checkRange(prescription.getOsCyl(), -10, 10, "OS CYL");
        checkRange(prescription.getPd(), 40, 90, "PD");
    }

    private void validateAxis(Integer axis, String eye) {
        if (axis != null && (axis < 0 || axis > 180)) {
            throw ApiException.of(ErrorCode.PRESCRIPTION_INCOMPLETE,
                    "Trục loạn " + eye + " phải nằm trong khoảng 0-180 độ");
        }
    }

    private void checkRange(BigDecimal value, double min, double max, String label) {
        if (value != null && (value.doubleValue() < min || value.doubleValue() > max)) {
            throw ApiException.of(ErrorCode.PRESCRIPTION_INCOMPLETE,
                    "Giá trị " + label + " nằm ngoài khoảng hợp lệ (" + min + " .. " + max + ")");
        }
    }
}
