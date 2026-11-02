package com.glassstore.lab.controller;

import com.glassstore.common.dto.ApiResponse;
import com.glassstore.lab.entity.LabJob;
import com.glassstore.lab.repository.LabJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab-jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LabJobController {

    private final LabJobRepository labJobRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LabJob>>> getAllLabJobs(@RequestParam(name = "status", required = false) String status) {
        List<LabJob> list = (status != null && !status.isEmpty())
                ? labJobRepository.findByStatusOrderByCreatedAtDesc(status)
                : labJobRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lệnh gia công mài kính thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabJob>> getLabJobById(@PathVariable("id") Long id) {
        LabJob job = labJobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lệnh mài kính ID: " + id));
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết lệnh mài kính thành công", job));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LabJob>> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        LabJob job = labJobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lệnh mài kính ID: " + id));

        if (body.containsKey("status")) {
            job.setStatus(body.get("status"));
        }
        if (body.containsKey("qcNotes")) {
            job.setQcNotes(body.get("qcNotes"));
        }
        job.setUpdatedAt(LocalDateTime.now());
        LabJob updated = labJobRepository.save(job);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái gia công QC thành công", updated));
    }
}

