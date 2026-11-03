package com.glassstore.lab.config;

import com.glassstore.lab.entity.Prescription;
import com.glassstore.lab.repository.EyeExamAppointmentRepository;
import com.glassstore.lab.repository.LabJobRepository;
import com.glassstore.lab.repository.PrescriptionRepository;
import com.glassstore.lab.repository.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class LabDataInitializer implements CommandLineRunner {

    private final PrescriptionRepository prescriptionRepository;
    private final LabJobRepository labJobRepository;
    private final WarrantyRepository warrantyRepository;
    private final EyeExamAppointmentRepository appointmentRepository;

    @Override
    public void run(String... args) {
        log.info("Lab Service khởi động ở trạng thái sạch: CSDL sẵn sàng tiếp nhận đơn thật từ bác sĩ đo khám và quầy POS!");
    }
}
