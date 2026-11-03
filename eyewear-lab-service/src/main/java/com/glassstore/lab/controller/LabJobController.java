package com.glassstore.lab.controller;

import com.glassstore.common.dto.ApiResponse;
import com.glassstore.lab.entity.LabJob;
import com.glassstore.lab.repository.LabJobRepository;
import com.glassstore.lab.service.LabJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Lab job API. Status changes go through {@link LabJobService} so the state machine and QC
 * gate are always enforced (BR-31..BR-34).
 */
@RestController
@RequestMapping("/api/lab-jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LabJobController {

    private final LabJobRepository labJobRepository;
    private final LabJobService labJobService;
    private final com.glassstore.lab.repository.LabJobStatusHistoryRepository historyRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LabJob>>> getAllLabJobs(
            @RequestParam(name = "status", required = false) String status) {
        List<LabJob> list = (status != null && !status.isEmpty())
                ? labJobRepository.findByStatusOrderByCreatedAtDesc(status)
                : labJobRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách lệnh gia công mài kính thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabJob>> getLabJobById(@PathVariable("id") Long id) {
        LabJob job = labJobRepository.findById(id)
                .orElseThrow(() -> com.glassstore.common.exception.ApiException.of(
                        com.glassstore.common.exception.ErrorCode.LAB_JOB_NOT_FOUND,
                        "Không tìm thấy lệnh mài kính ID: " + id));
        return ResponseEntity.ok(ApiResponse.success("OK", job));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<java.util.List<com.glassstore.lab.entity.LabJobStatusHistory>>> history(
            @PathVariable("id") Long id) {
        if (!labJobRepository.existsById(id)) {
            throw com.glassstore.common.exception.ApiException.of(
                    com.glassstore.common.exception.ErrorCode.LAB_JOB_NOT_FOUND,
                    "Không tìm thấy lệnh mài kính ID: " + id);
        }
        return ResponseEntity.ok(ApiResponse.success("OK",
                historyRepository.findByLabJobIdOrderByChangedAtAsc(id)));
    }

    /**
     * Creates a fabrication job for a paid order. Called internally by order-pos-service and
     * by staff from the portal.
     */
    @PostMapping("/from-order")
    public ResponseEntity<ApiResponse<LabJob>> createFromOrder(@RequestBody Map<String, Object> body) {
        Long orderId = asLong(body.get("orderId"));
        Long prescriptionId = asLong(body.get("prescriptionId"));
        LabJob job = labJobService.createFromOrder(
                orderId,
                prescriptionId,
                asString(body.get("frameSku")),
                asString(body.get("frameName")),
                asString(body.get("lensSku")),
                asString(body.get("lensName")),
                asString(body.get("technicianName")));
        return ResponseEntity.ok(ApiResponse.success("Tạo lệnh gia công thành công", job));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LabJob>> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        LabJob updated = labJobService.updateStatus(
                id,
                body.get("status"),
                body.get("qcNotes"),
                body.get("technicianName"));
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật trạng thái gia công QC thành công", updated));
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}