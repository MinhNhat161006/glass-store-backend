package com.glassstore.lab.controller;

import com.glassstore.common.dto.ApiResponse;
import com.glassstore.lab.dto.CreateLabJobRequest;
import com.glassstore.lab.dto.LabJobResponse;
import com.glassstore.lab.dto.UpdateJobStatusRequest;
import com.glassstore.lab.entity.LabJobStatus;
import com.glassstore.lab.service.LabJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lab-jobs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LabJobController {

    private final LabJobService labJobService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LabJobResponse>>> getLabJobs(
            @RequestParam(name = "status", required = false) LabJobStatus status) {
        log.info("API GET /api/lab-jobs - Lọc theo status: {}", status);
        List<LabJobResponse> list = labJobService.getLabJobs(status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lệnh gia công mài kính thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabJobResponse>> getLabJobById(@PathVariable("id") Long id) {
        log.info("API GET /api/lab-jobs/{}", id);
        LabJobResponse job = labJobService.getLabJobById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết lệnh mài kính thành công", job));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LabJobResponse>> updateJobStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateJobStatusRequest request) {
        log.info("API PATCH /api/lab-jobs/{}/status - Request: {}", id, request);
        LabJobResponse updated = labJobService.updateJobStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái gia công QC thành công", updated));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LabJobResponse>> createLabJob(
            @Valid @RequestBody CreateLabJobRequest request) {
        log.info("API POST /api/lab-jobs - Request: {}", request);
        LabJobResponse created = labJobService.createLabJob(request);
        ApiResponse<LabJobResponse> response = ApiResponse.<LabJobResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo lệnh gia công mài kính thành công")
                .data(created)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
