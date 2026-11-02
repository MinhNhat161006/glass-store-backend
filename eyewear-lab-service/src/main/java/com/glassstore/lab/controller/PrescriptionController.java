package com.glassstore.lab.controller;

import com.glassstore.common.dto.ApiResponse;
import com.glassstore.lab.entity.Prescription;
import com.glassstore.lab.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Prescription>>> getAllPrescriptions() {
        List<Prescription> list = prescriptionRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách toa kính thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Prescription>> getPrescriptionById(@PathVariable("id") Long id) {
        Prescription p = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy toa kính với ID: " + id));
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết toa kính thành công", p));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<Prescription>>> getByCustomer(@PathVariable("customerId") Long customerId) {
        List<Prescription> list = prescriptionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử khúc xạ của khách hàng thành công", list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Prescription>> createPrescription(@RequestBody Prescription prescription) {
        prescription.setCreatedAt(LocalDateTime.now());
        Prescription saved = prescriptionRepository.save(prescription);
        return ResponseEntity.ok(ApiResponse.success("Tạo mới toa kính thành công", saved));
    }
}
