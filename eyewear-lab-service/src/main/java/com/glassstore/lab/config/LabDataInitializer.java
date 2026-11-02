package com.glassstore.lab.config;

import com.glassstore.lab.entity.LabJob;
import com.glassstore.lab.entity.Prescription;
import com.glassstore.lab.repository.LabJobRepository;
import com.glassstore.lab.repository.PrescriptionRepository;
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

    @Override
    public void run(String... args) {
        if (prescriptionRepository.count() > 0) {
            log.info("Lab database đã có dữ liệu hồ sơ lâm sàng, bỏ qua nạp seed.");
            return;
        }

        log.info("Bắt đầu khởi tạo dữ liệu hồ sơ khúc xạ OD/OS và lệnh gia công mài lắp kính...");

        // 1. Nguyễn Hoàng Duy (32 tuổi) - Cận thị văn phòng
        Prescription rx1 = prescriptionRepository.save(Prescription.builder()
                .customerId(10L)
                .customerName("Nguyễn Hoàng Duy")
                .optometristId(6L)
                .optometristName("BS. CKI Vũ Anh Tuấn")
                .odSph(new BigDecimal("-3.25"))
                .odCyl(new BigDecimal("-0.75"))
                .odAxis(180)
                .odAdd(BigDecimal.ZERO)
                .osSph(new BigDecimal("-2.75"))
                .osCyl(new BigDecimal("-0.50"))
                .osAxis(175)
                .osAdd(BigDecimal.ZERO)
                .pd(new BigDecimal("63.5"))
                .notes("Làm việc màn hình 8h/ngày, khuyên dùng tròng Essilor Crizal Sapphire HR 1.67 Blue UV giảm mỏi điều tiết")
                .build());

        // 2. Lê Thúy Kiều (28 tuổi) - Cận loạn vừa
        Prescription rx2 = prescriptionRepository.save(Prescription.builder()
                .customerId(11L)
                .customerName("Lê Thúy Kiều")
                .optometristId(7L)
                .optometristName("BS. Lâm Hoàng Phúc")
                .odSph(new BigDecimal("-2.00"))
                .odCyl(new BigDecimal("-0.50"))
                .odAxis(180)
                .odAdd(BigDecimal.ZERO)
                .osSph(new BigDecimal("-1.75"))
                .osCyl(new BigDecimal("-0.25"))
                .osAxis(175)
                .osAdd(BigDecimal.ZERO)
                .pd(new BigDecimal("61.0"))
                .notes("Độ khúc xạ ổn định so với năm ngoái, lắp gọng Aviator mạ vàng tôn dáng mặt")
                .build());

        // 3. Trịnh Minh Quang (41 tuổi) - Cận nặng & Loạn cao
        Prescription rx3 = prescriptionRepository.save(Prescription.builder()
                .customerId(12L)
                .customerName("Trịnh Minh Quang")
                .optometristId(6L)
                .optometristName("BS. CKI Vũ Anh Tuấn")
                .odSph(new BigDecimal("-7.50"))
                .odCyl(new BigDecimal("-2.50"))
                .odAxis(180)
                .odAdd(BigDecimal.ZERO)
                .osSph(new BigDecimal("-7.00"))
                .osCyl(new BigDecimal("-2.00"))
                .osAxis(170)
                .osAdd(BigDecimal.ZERO)
                .pd(new BigDecimal("69.0"))
                .notes("Cảnh báo: Khoảng cách đồng tử PD rộng 69mm và độ cận cao. Cần mài tâm tròng Chemi U6 1.74 chính xác")
                .build());

        // 4. Phạm Thị Bích Ngọc (35 tuổi) - Lão thị / Kính Đa Tròng
        Prescription rx4 = prescriptionRepository.save(Prescription.builder()
                .customerId(13L)
                .customerName("Phạm Thị Bích Ngọc")
                .optometristId(6L)
                .optometristName("BS. CKI Vũ Anh Tuấn")
                .odSph(new BigDecimal("1.50"))
                .odCyl(BigDecimal.ZERO)
                .odAxis(0)
                .odAdd(new BigDecimal("2.00"))
                .osSph(new BigDecimal("1.75"))
                .osCyl(BigDecimal.ZERO)
                .osAxis(0)
                .osAdd(new BigDecimal("2.00"))
                .pd(new BigDecimal("64.0"))
                .notes("Lão thị nhìn gần mờ, tư vấn đa tròng kỹ thuật số Essilor Varilux Comfort Max mượt mà")
                .build());

        // Lab Jobs
        labJobRepository.save(LabJob.builder()
                .orderId(1001L)
                .prescription(rx1)
                .frameSku("BL-BJ3120-C10")
                .frameName("Gọng Bolon Titanium Rose Gold BJ3120")
                .lensSku("LZ-CRIZAL-HR167")
                .lensName("Cặp Tròng Essilor Crizal Sapphire HR 1.67")
                .technicianId(8L)
                .technicianName("Trần Văn Bách")
                .status("IN_PROGRESS")
                .qcNotes("KTV Trần Văn Bách đang vận hành máy mài Essilor Kappa vát cạnh gọng Bolon")
                .build());

        labJobRepository.save(LabJob.builder()
                .orderId(1002L)
                .prescription(rx2)
                .frameSku("RB-3025-001")
                .frameName("Ray-Ban Aviator Gọng Vàng Kim")
                .lensSku("LZ-CHEMI-U2-160")
                .lensName("Cặp Tròng Chemi U2 BlueCut 1.60")
                .technicianId(8L)
                .technicianName("Trần Văn Bách")
                .status("QC_PASSED")
                .qcNotes("Kiểm tra tâm quang học đạt chuẩn ANSI Z80.1, hoàn tất bao gói và chuyển quầy giao khách")
                .build());

        labJobRepository.save(LabJob.builder()
                .orderId(1003L)
                .prescription(rx3)
                .frameSku("CH-ZT1980-BK")
                .frameName("Gọng Charmant Titanium Z Khoan Không Viền")
                .lensSku("LZ-CHEMI-U6-174")
                .lensName("Cặp Tròng Chemi Crystal U6 1.74 Siêu Mỏng")
                .technicianId(8L)
                .technicianName("Trần Văn Bách")
                .status("WAITING_PROCESS")
                .qcNotes("Đang chờ BS. Vũ Anh Tuấn ký duyệt lệnh mài tròng chiết suất 1.74 do PD = 69mm")
                .build());

        log.info("Đã khởi tạo thành công {} đơn khúc xạ và {} lệnh lab vào db_lab!",
                prescriptionRepository.count(), labJobRepository.count());
    }
}
