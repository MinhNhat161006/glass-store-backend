package com.glassstore.lab.controller;

import com.glassstore.lab.entity.Warranty;
import com.glassstore.lab.repository.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warranties")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WarrantyController {

    private final WarrantyRepository warrantyRepository;

    @GetMapping
    public ResponseEntity<List<Warranty>> getAllWarranties() {
        return ResponseEntity.ok(warrantyRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Warranty> getWarrantyById(@PathVariable("id") Long id) {
        return warrantyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Warranty> getWarrantyByCode(@PathVariable("code") String code) {
        return warrantyRepository.findByWarrantyCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Warranty> createWarranty(@RequestBody Warranty warranty) {
        if (warranty.getWarrantyCode() == null || warranty.getWarrantyCode().isEmpty()) {
            warranty.setWarrantyCode("#WAR-" + (System.currentTimeMillis() % 10000));
        }
        warranty.setCreatedAt(LocalDateTime.now());
        warranty.setUpdatedAt(LocalDateTime.now());
        Warranty saved = warrantyRepository.save(warranty);
        log.info("Tạo mới phiếu bảo hành thành công: {}", saved.getWarrantyCode());
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Warranty> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        return warrantyRepository.findById(id).map(w -> {
            if (body.containsKey("status")) w.setStatus(body.get("status"));
            if (body.containsKey("statusLabel")) w.setStatusLabel(body.get("statusLabel"));
            if (body.containsKey("statusColor")) w.setStatusColor(body.get("statusColor"));
            if (body.containsKey("techNote")) w.setTechNote(body.get("techNote"));
            w.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(warrantyRepository.save(w));
        }).orElse(ResponseEntity.notFound().build());
    }
}
