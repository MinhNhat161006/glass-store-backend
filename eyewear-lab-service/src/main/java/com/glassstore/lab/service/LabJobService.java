package com.glassstore.lab.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.lab.domain.LabJobStatus;
import com.glassstore.lab.entity.LabJob;
import com.glassstore.lab.entity.Prescription;
import com.glassstore.lab.repository.LabJobRepository;
import com.glassstore.lab.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Creates and advances lens fabrication jobs (SRS §4.11).
 *
 * <p>Prescription completeness is validated here, not trusted from the caller, so an
 * incomplete prescription can never reach the grinding machine (BR-30).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabJobService {

    private final LabJobRepository labJobRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final com.glassstore.lab.repository.LabJobStatusHistoryRepository historyRepository;

    /** Creates a job from a paid order. Idempotent per order (one job per order). */
    @Transactional
    public LabJob createFromOrder(Long orderId, Long prescriptionId, String frameSku, String frameName,
                                  String lensSku, String lensName, String technicianName) {

        if (orderId == null) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Thiếu mã đơn hàng để tạo lệnh gia công");
        }
        if (prescriptionId == null) {
            throw ApiException.of(ErrorCode.PRESCRIPTION_INCOMPLETE,
                    "Thiếu toa kính đã xác nhận để tạo lệnh gia công");
        }
        // BR-29: one fabrication job per order keeps the audit trail unambiguous.
        return labJobRepository.findByOrderId(orderId).orElseGet(() -> {
            Prescription rx = prescriptionRepository.findById(prescriptionId)
                    .orElseThrow(() -> ApiException.of(ErrorCode.PRESCRIPTION_NOT_FOUND,
                            "Không tìm thấy toa kính ID: " + prescriptionId));

            requireCompletePrescription(rx);

            LabJob job = LabJob.builder()
                    .orderId(orderId)
                    .prescription(rx)
                    .frameSku(frameSku)
                    .frameName(frameName)
                    .lensSku(lensSku)
                    .lensName(lensName)
                    .technicianName(technicianName)
                    .status(LabJobStatus.WAITING_PROCESS.name())
                    .build();

            LabJob saved = labJobRepository.save(job);
            historyRepository.save(com.glassstore.lab.entity.LabJobStatusHistory.builder()
                    .labJobId(saved.getId())
                    .fromStatus(null)
                    .toStatus(saved.getStatus())
                    .actor(technicianName)
                    .reason("Tạo lệnh gia công từ đơn hàng")
                    .build());
            log.info("Created lab job {} for order {} using prescription {}",
                    saved.getId(), orderId, rx.getId());
            return saved;
        });
    }

    /** Validated status change (BR-31..BR-34). */
    @Transactional
    public LabJob updateStatus(Long id, String rawStatus, String qcNotes, String technicianName) {
        LabJob job = labJobRepository.findById(id)
                .orElseThrow(() -> ApiException.of(ErrorCode.LAB_JOB_NOT_FOUND,
                        "Không tìm thấy lệnh mài kính ID: " + id));

        LabJobStatus current = LabJobStatus.parse(job.getStatus());
        LabJobStatus target = LabJobStatus.parse(rawStatus);

        if (current == target) {
            throw ApiException.of(ErrorCode.LAB_JOB_INVALID_TRANSITION,
                    "Lệnh gia công đã ở trạng thái " + current);
        }
        if (!current.canTransitionTo(target)) {
            throw ApiException.of(ErrorCode.LAB_JOB_INVALID_TRANSITION,
                    "Không thể chuyển lệnh gia công từ " + current + " sang " + target);
        }
        // BR-32: a QC failure or cancellation must state why.
        if (target.requiresReasonToEnter() && (qcNotes == null || qcNotes.isBlank())) {
            throw ApiException.of(ErrorCode.LAB_JOB_QC_REASON_REQUIRED,
                    "Phải ghi rõ lý do khi đánh giá QC không đạt hoặc hủy lệnh");
        }

        job.setStatus(target.name());
        if (qcNotes != null && !qcNotes.isBlank()) {
            job.setQcNotes(qcNotes);
        }
        if (technicianName != null && !technicianName.isBlank()) {
            job.setTechnicianName(technicianName);
        }
        job.setUpdatedAt(LocalDateTime.now());

        LabJob saved = labJobRepository.save(job);
        historyRepository.save(com.glassstore.lab.entity.LabJobStatusHistory.builder()
                .labJobId(saved.getId())
                .fromStatus(current.name())
                .toStatus(target.name())
                .actor(technicianName)
                .reason(qcNotes)
                .build());
        log.info("Lab job {} transitioned {} -> {}", id, current, target);
        return saved;
    }

    /**
     * Guards the handover step: a job cannot be marked DELIVERED unless QC passed earlier,
     * which the state machine already enforces, but the QC notes must also exist.
     */
    private void requireCompletePrescription(Prescription rx) {
        if (rx.getOdSph() == null || rx.getOsSph() == null || rx.getPd() == null) {
            throw ApiException.of(ErrorCode.PRESCRIPTION_INCOMPLETE,
                    "Toa kính thiếu SPH hoặc PD bắt buộc, không thể đưa vào gia công");
        }
        validateAxis(rx.getOdAxis(), "OD");
        validateAxis(rx.getOsAxis(), "OS");
        // BR-27: values must stay within professional range; never auto-corrected.
        checkRange(rx.getOdSph(), -30, 30, "OD SPH");
        checkRange(rx.getOsSph(), -30, 30, "OS SPH");
        checkRange(rx.getOdCyl(), -10, 10, "OD CYL");
        checkRange(rx.getOsCyl(), -10, 10, "OS CYL");
        checkRange(rx.getPd(), 40, 90, "PD");
    }

    private void validateAxis(Integer axis, String eye) {
        if (axis != null && (axis < 0 || axis > 180)) {
            throw ApiException.of(ErrorCode.PRESCRIPTION_INCOMPLETE,
                    "Trục loạn " + eye + " phải nằm trong khoảng 0-180 độ");
        }
    }

    private void checkRange(BigDecimal value, double min, double max, String label) {
        if (value == null) {
            return;
        }
        double v = value.doubleValue();
        if (v < min || v > max) {
            throw ApiException.of(ErrorCode.PRESCRIPTION_INCOMPLETE,
                    "Giá trị " + label + " nằm ngoài khoảng hợp lệ (" + min + " .. " + max + ")");
        }
    }
}