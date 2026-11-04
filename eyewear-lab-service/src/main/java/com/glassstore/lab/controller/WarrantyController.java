package com.glassstore.lab.controller;

import com.glassstore.lab.dto.WarrantyActivationRequest;
import com.glassstore.lab.dto.WarrantyLookupItemDTO;
import com.glassstore.lab.entity.Warranty;
import com.glassstore.lab.repository.WarrantyRepository;
import com.glassstore.lab.service.WarrantyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warranties")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WarrantyController {

    private final WarrantyRepository warrantyRepository;
    private final WarrantyService warrantyService;

    @GetMapping
    public ResponseEntity<List<Warranty>> getAllWarranties() {
        return ResponseEntity.ok(warrantyRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/search")
    public ResponseEntity<List<WarrantyLookupItemDTO>> searchWarranties(
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "barcode", required = false) String barcode) {
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasBarcode = barcode != null && !barcode.isBlank();
        if (hasPhone == hasBarcode) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(hasPhone
                ? warrantyService.lookupByPhone(phone)
                : warrantyService.lookupByBarcode(barcode));
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
    public ResponseEntity<Warranty> activateWarranty(@Valid @RequestBody WarrantyActivationRequest request) {
        return ResponseEntity.ok(warrantyService.activate(request));
    }
}
