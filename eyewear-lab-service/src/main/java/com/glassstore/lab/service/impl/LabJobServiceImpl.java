package com.glassstore.lab.service.impl;

import com.glassstore.lab.dto.CreateLabJobRequest;
import com.glassstore.lab.dto.LabJobResponse;
import com.glassstore.lab.dto.UpdateJobStatusRequest;
import com.glassstore.lab.entity.LabJob;
import com.glassstore.lab.entity.LabJobStatus;
import com.glassstore.lab.entity.Prescription;
import com.glassstore.lab.repository.LabJobRepository;
import com.glassstore.lab.repository.PrescriptionRepository;
import com.glassstore.lab.service.LabJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabJobServiceImpl implements LabJobService {

    private final LabJobRepository labJobRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LabJobResponse> getLabJobs(LabJobStatus status) {
        log.info("Lấy danh sách lệnh mài kính tại xưởng với bộ lọc trạng thái: {}", status);
        List<LabJob> jobs = (status != null)
                ? labJobRepository.findByStatusOrderByCreatedAtDesc(status)
                : labJobRepository.findAllByOrderByCreatedAtDesc();

        if (jobs == null || jobs.isEmpty()) {
            log.info("Danh sách lệnh gia công mài kính rỗng");
            return Collections.emptyList();
        }

        Set<Long> prescriptionIds = jobs.stream()
                .map(LabJob::getPrescriptionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Prescription> prescriptionMap = prescriptionIds.isEmpty()
                ? Collections.emptyMap()
                : prescriptionRepository.findAllById(prescriptionIds).stream()
                        .collect(Collectors.toMap(Prescription::getId, p -> p, (p1, p2) -> p1));

        return jobs.stream()
                .map(job -> LabJobResponse.from(job, prescriptionMap.get(job.getPrescriptionId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LabJobResponse getLabJobById(Long id) {
        log.info("Lấy chi tiết lệnh mài kính ID: {}", id);
        LabJob job = labJobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lệnh mài kính ID: " + id));

        Prescription rx = null;
        if (job.getPrescriptionId() != null) {
            rx = prescriptionRepository.findById(job.getPrescriptionId()).orElse(null);
        }
        return LabJobResponse.from(job, rx);
    }

    @Override
    @Transactional
    public LabJobResponse updateJobStatus(Long id, UpdateJobStatusRequest request) {
        log.info("Cập nhật trạng thái lệnh gia công ID: {}, yêu cầu: {}", id, request);
        LabJob job = labJobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lệnh mài kính ID: " + id));

        LabJobStatus newStatus = request.getStatus();
        if (newStatus == null) {
            throw new IllegalArgumentException("Trạng thái cập nhật không được để trống");
        }

        // 1. Chuyển sang IN_PROGRESS: gán startedAt = LocalDateTime.now(), technicianId
        if (newStatus == LabJobStatus.IN_PROGRESS) {
            job.setStartedAt(LocalDateTime.now());
            if (request.getTechnicianId() != null) {
                job.setTechnicianId(request.getTechnicianId());
            }
            if (request.getTechnicianName() != null && !request.getTechnicianName().isBlank()) {
                job.setTechnicianName(request.getTechnicianName());
            }
            String techNotes = request.getEffectiveTechnicianNotes();
            if (techNotes != null && !techNotes.isBlank()) {
                job.setTechnicianNotes(techNotes);
            }
        }

        // 2. Chuyển sang QC_PASSED (hoàn thành): gán completedAt = LocalDateTime.now()
        if (newStatus == LabJobStatus.QC_PASSED) {
            job.setCompletedAt(LocalDateTime.now());
            String qcNotes = request.getEffectiveQcNotes();
            if (qcNotes != null && !qcNotes.isBlank()) {
                job.setQcNotes(qcNotes);
            }
        }

        // 3. Nếu cập nhật status là QC_FAILED: bắt buộc qcNotes không được null hoặc rỗng
        if (newStatus == LabJobStatus.QC_FAILED) {
            String qcNotes = request.getEffectiveQcNotes();
            if (qcNotes == null || qcNotes.trim().isEmpty()) {
                log.error("Cập nhật lệnh mài ID: {} sang QC_FAILED thất bại: thiếu ghi chú QC", id);
                throw new IllegalArgumentException("Ghi chú kiểm định QC (qcNotes) không được để trống khi kết quả là QC_FAILED");
            }
            job.setQcNotes(qcNotes.trim());
        }

        // Chuyển sang QC_PENDING: lưu ghi chú nếu có
        if (newStatus == LabJobStatus.QC_PENDING) {
            String notes = request.getEffectiveQcNotes();
            if (notes != null && !notes.isBlank()) {
                job.setQcNotes(notes);
            }
        }

        // Cập nhật thông tin kỹ thuật viên nếu được cung cấp
        if (request.getTechnicianId() != null) {
            job.setTechnicianId(request.getTechnicianId());
        }
        if (request.getTechnicianName() != null && !request.getTechnicianName().isBlank()) {
            job.setTechnicianName(request.getTechnicianName());
        }

        job.setStatus(newStatus);
        job.setUpdatedAt(LocalDateTime.now());

        LabJob updatedJob = labJobRepository.save(job);
        log.info("Lệnh mài ID: {} đã chuyển sang trạng thái: {}", id, newStatus);

        Prescription rx = null;
        if (updatedJob.getPrescriptionId() != null) {
            rx = prescriptionRepository.findById(updatedJob.getPrescriptionId()).orElse(null);
        }
        return LabJobResponse.from(updatedJob, rx);
    }

    @Override
    @Transactional
    public LabJobResponse createLabJob(CreateLabJobRequest request) {
        log.info("Tạo mới lệnh mài kính cho đơn hàng orderId={}, prescriptionId={}, customerName={}",
                request.getOrderId(), request.getPrescriptionId(), request.getCustomerName());

        LabJob job = LabJob.builder()
                .orderId(request.getOrderId())
                .customerName(request.getCustomerName())
                .prescriptionId(request.getPrescriptionId())
                .lensSku(request.getLensSku())
                .lensName(request.getLensName())
                .frameSku(request.getFrameSku())
                .frameName(request.getFrameName())
                .status(LabJobStatus.PENDING)
                .technicianId(request.getTechnicianId())
                .technicianName(request.getTechnicianName())
                .technicianNotes(request.getTechnicianNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        LabJob savedJob = labJobRepository.save(job);
        log.info("Đã tạo lệnh mài kính mới thành công với ID: {}, status: PENDING", savedJob.getId());

        Prescription rx = null;
        if (savedJob.getPrescriptionId() != null) {
            rx = prescriptionRepository.findById(savedJob.getPrescriptionId()).orElse(null);
        }
        return LabJobResponse.from(savedJob, rx);
    }
}

