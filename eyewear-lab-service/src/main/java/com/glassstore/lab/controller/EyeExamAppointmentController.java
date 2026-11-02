package com.glassstore.lab.controller;

import com.glassstore.lab.entity.EyeExamAppointment;
import com.glassstore.lab.repository.EyeExamAppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EyeExamAppointmentController {

    private final EyeExamAppointmentRepository appointmentRepository;

    @GetMapping
    public ResponseEntity<List<EyeExamAppointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentRepository.findAllByOrderByPriorityLevelAscCreatedAtAsc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EyeExamAppointment> getAppointmentById(@PathVariable("id") Long id) {
        return appointmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EyeExamAppointment> createAppointment(@RequestBody EyeExamAppointment appointment) {
        if (appointment.getTicketCode() == null || appointment.getTicketCode().isEmpty()) {
            appointment.setTicketCode("EP-" + (1000 + (int)(Math.random() * 9000)));
        }
        appointment.setCreatedAt(LocalDateTime.now());
        if (appointment.getStatus() == null) {
            appointment.setStatus("WAITING");
            appointment.setStatusLabel("Đang chờ");
        }
        EyeExamAppointment saved = appointmentRepository.save(appointment);
        log.info("Tiếp nhận lượt khám mắt thành công: {}", saved.getTicketCode());
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EyeExamAppointment> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        return appointmentRepository.findById(id).map(a -> {
            if (body.containsKey("status")) a.setStatus(body.get("status"));
            if (body.containsKey("statusLabel")) a.setStatusLabel(body.get("statusLabel"));
            if (body.containsKey("roomName")) a.setRoomName(body.get("roomName"));
            if (body.containsKey("examStep")) a.setExamStep(body.get("examStep"));
            return ResponseEntity.ok(appointmentRepository.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }
}
