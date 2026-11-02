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
        if (prescriptionRepository.count() > 0) {
            log.info("Lab database đã có dữ liệu, bỏ qua nạp seed.");
            return;
        }

        log.info("Khởi tạo duy nhất 1 hồ sơ khúc xạ mẫu chuẩn nhãn khoa cho khách hàng Nguyễn Hoàng Duy...");

        // Hồ sơ khúc xạ mẫu duy nhất phục vụ demo ráp kính tại POS
        prescriptionRepository.save(Prescription.builder()
                .customerId(5L)
                .customerName("Nguyễn Hoàng Duy")
                .optometristId(3L)
                .optometristName("BS. Lê Thị Ánh Tuyết")
                .odSph(new BigDecimal("-2.50"))
                .odCyl(new BigDecimal("-0.50"))
                .odAxis(180)
                .odAdd(BigDecimal.ZERO)
                .osSph(new BigDecimal("-2.25"))
                .osCyl(new BigDecimal("-0.25"))
                .osAxis(175)
                .osAdd(BigDecimal.ZERO)
                .pd(new BigDecimal("63.0"))
                .notes("Mắt điều tiết tốt, khuyên dùng tròng Essilor Crizal Sapphire HR 1.67 hoặc Chemi U2.")
                .build());

        log.info("Lab Service hoàn tất khởi tạo tinh gọn: Hàng đợi Lab, Lịch hẹn và Bảo hành đều ở trạng thái sạch (0 rác).");
    }
}
