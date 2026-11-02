package com.glassstore.lab.config;

import com.glassstore.lab.entity.EyeExamAppointment;
import com.glassstore.lab.entity.LabJob;
import com.glassstore.lab.entity.Prescription;
import com.glassstore.lab.entity.Warranty;
import com.glassstore.lab.repository.EyeExamAppointmentRepository;
import com.glassstore.lab.repository.LabJobRepository;
import com.glassstore.lab.repository.PrescriptionRepository;
import com.glassstore.lab.repository.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

        // ==========================================
        // Seed Warranties (Phiếu Bảo Hành & Đổi Trả)
        // ==========================================
        warrantyRepository.save(Warranty.builder()
                .warrantyCode("#WAR-2026-089")
                .originalOrderCode("#OPT-8842-SG")
                .customerId(10L)
                .customerName("Nguyễn Hoàng Duy")
                .customerPhone("0918 732 998")
                .customerTier("VIP SILVER")
                .productName("Gọng Kính Bolon Graceful BJ3120 Titanium")
                .productSku("BLN-BJ3120-RG")
                .productDetails("Màu: Rose Gold C30 | Size: 52-18-145 | Chất liệu: B-Titanium siêu nhẹ")
                .issueDescription("Ốc bản lề càng kính trái bị lỏng khiến càng bị rơ khi đeo; hai đệm ve mũi silicon bị ngả vàng sau 11 tháng sử dụng.")
                .claimType("Thay Linh Kiện Miễn Phí Tại Quầy")
                .policyRef("Lỗi hao mòn cơ học (BR-35)")
                .techNote("Khung titan còn nguyên vẹn, không có vết nứt gãy. Đã tháo ốc cũ, tra keo chống trôi ren Loctite Optic, thay ốc titan Bolon M1.2 mới. Cân chỉnh 4 điểm Bench Alignment.")
                .cost("0 VNĐ")
                .techName("KTV Trần Văn Bách")
                .status("PROCESSING_COUNTER")
                .statusLabel("Đang xử lý tại quầy")
                .statusColor("bg-secondary-fixed text-on-secondary-fixed")
                .timeLabel("10:45 Hôm nay")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        warrantyRepository.save(Warranty.builder()
                .warrantyCode("#WAR-2026-088")
                .originalOrderCode("#OPT-8721")
                .customerId(11L)
                .customerName("Lê Thúy Kiều")
                .customerPhone("0988 124 551")
                .customerTier("VIP GOLD")
                .productName("Crizal Rock 1.60 • Bong váng phủ OD")
                .productSku("LNS-ESS-CRIZAL-160")
                .productDetails("Cặp tròng Essilor Crizal Rock 1.60 | Mua 4 tháng")
                .issueDescription("Mắt phải (OD) xuất hiện đốm bong lớp phủ chống trầy xước không do tác động ngoại lực.")
                .claimType("Đổi tròng")
                .policyRef("Bảo hành chính hãng Essilor")
                .techNote("KTV Lab thẩm định: Lớp váng AR bị rộp viền nhiệt do lỗi phôi. Đã duyệt bảo hành đổi phôi mới 100% từ nhà sản xuất Essilor.")
                .cost("Đổi tròng")
                .techName("KTV Trần Văn Bách")
                .status("LAB_APPROVED")
                .statusLabel("Lab duyệt đổi mới 100%")
                .statusColor("bg-primary-fixed text-on-primary-fixed")
                .timeLabel("09:12 Hôm nay")
                .createdAt(LocalDateTime.now().minusHours(3))
                .build());

        warrantyRepository.save(Warranty.builder()
                .warrantyCode("#WAR-2026-085")
                .originalOrderCode("#OPT-8810")
                .customerId(12L)
                .customerName("Trần Minh Quang")
                .customerPhone("0912 449 002")
                .customerTier("THÀNH VIÊN")
                .productName("Titanium Minimalist • Đổi size 54 sang 52")
                .productSku("FRM-MIN-54")
                .productDetails("Gọng Titanium Minimalist | Tình trạng: Nguyên tem hộp, không xước")
                .issueDescription("Khách hàng đeo thử thấy bản gọng size 54 hơi rộng cầu mũi, có nhu cầu đổi sang size 52 theo chính sách đổi trả trong vòng 7 ngày.")
                .claimType("Đổi 7 ngày (BR-38)")
                .policyRef("Đã mài lại tròng mới")
                .techNote("Sản phẩm nguyên vẹn, phụ kiện đầy đủ. Đã xuất kho gọng size 52 và mài lại tròng mới cho khách hàng.")
                .cost("Đổi 7 ngày (BR-38)")
                .techName("KTV Nguyễn Văn Định")
                .status("WAITING_CUSTOMER")
                .statusLabel("Chờ khách nhận gọng")
                .statusColor("bg-surface-container-high text-on-surface")
                .timeLabel("Hôm qua")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        warrantyRepository.save(Warranty.builder()
                .warrantyCode("#WAR-2026-082")
                .originalOrderCode("#OPT-8750")
                .customerId(14L)
                .customerName("Đỗ Quỳnh Anh")
                .customerPhone("0909 123 456")
                .customerTier("THÀNH VIÊN")
                .productName("Gọng Ray-Ban • Gãy càng do tai nạn xe")
                .productSku("FRM-RB-AVIATOR-GLD")
                .productDetails("Gọng Ray-Ban Classic")
                .issueDescription("Gọng kính bị gãy càng phải do va quẹt giao thông, móp méo cầu kính.")
                .claimType("Lỗi EX-WAR-03")
                .policyRef("Va đập ngoại lực mạnh")
                .techNote("Không thuộc diện bảo hành miễn phí theo điều khoản EX-WAR-03 (hư hỏng do tai nạn/ngoại lực). Đề xuất chính sách hỗ trợ khách hàng: Trợ giá 50% mua gọng mới.")
                .cost("Lỗi EX-WAR-03")
                .techName("KTV Trần Văn Bách")
                .status("REJECTED")
                .statusLabel("Từ chối BH miễn phí")
                .statusColor("bg-error-container text-on-error-container")
                .timeLabel("14/04/2026")
                .createdAt(LocalDateTime.now().minusDays(3))
                .build());

        warrantyRepository.save(Warranty.builder()
                .warrantyCode("#WAR-2026-079")
                .originalOrderCode("#OPT-8650")
                .customerId(13L)
                .customerName("Phan Đức Toàn")
                .customerPhone("0933 555 777")
                .customerTier("VIP GOLD")
                .productName("Đổi màu tròng râm cận (Trà sang Khói)")
                .productSku("LNS-ESS-TRANS-156")
                .productDetails("Tròng đổi màu Transitions Gen 8")
                .issueDescription("Khách hàng mong muốn đổi màu tròng từ màu Trà sang màu Khói do độ tương phản ánh sáng ngoài trời.")
                .claimType("Bảo hành thích nghi mắt")
                .policyRef("Đơn gốc #OPT-8650")
                .techNote("Hoàn tất mài lắp phôi mới màu Khói chuẩn trục, khách hàng đã trực tiếp thử kính và ký nhận bàn giao.")
                .cost("Đã ký nhận")
                .techName("KTV Trần Văn Bách")
                .status("COMPLETED")
                .statusLabel("Hoàn tất bàn giao")
                .statusColor("bg-surface-container text-outline")
                .timeLabel("12/04/2026")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build());

        // ==========================================
        // Seed Appointments (Hàng Đợi Đo Mắt FR-EYE-01)
        // ==========================================
        appointmentRepository.save(EyeExamAppointment.builder()
                .ticketCode("EP-9921")
                .ticketType("E-Pass")
                .customerId(10L)
                .customerName("Nguyễn Hoàng Duy")
                .customerPhone("0918 732 998")
                .customerTier("VIP Silver")
                .optometristId(6L)
                .optometristName("BS. Vũ Anh Tuấn (CCHN: 014829/BYT-CCHN)")
                .roomName("Phòng Đo Mắt 01")
                .roomDevice("Topcon KR-800 Auto Ref-Keratometer • Kính thử phoropter CV-5000")
                .status("IN_EXAM")
                .statusLabel("Đang khám")
                .priorityLevel(1)
                .waitMinutes(14)
                .examStep("Đang thử kính mẫu & Cân bằng thị giác hai mắt (Binocular Balance)")
                .examStepIndex(7)
                .scheduledTime("10:15 Hôm nay")
                .notes("Đo khúc xạ vi tính + soi đáy mắt kiểm tra thị lực sau 1 năm")
                .build());

        appointmentRepository.save(EyeExamAppointment.builder()
                .ticketCode("EP-9925")
                .ticketType("E-Pass")
                .customerId(11L)
                .customerName("Võ Thị Mai Lan")
                .customerPhone("0903 112 233")
                .customerTier("VIP Gold")
                .optometristId(7L)
                .optometristName("KTV Lâm Hoàng Phúc (KTV Khúc Xạ Chính [STF-102])")
                .roomName("Phòng Đo Mắt 02")
                .roomDevice("Nidek RT-5100 Intelligent Refractor • Bảng đo thị lực điện tử SC-1600")
                .status("WAITING")
                .statusLabel("Sẵn sàng tiếp nhận")
                .priorityLevel(1)
                .waitMinutes(6)
                .examStep("Chuẩn bị tiếp nhận vào phòng khám")
                .examStepIndex(1)
                .scheduledTime("09:15 Hôm nay")
                .notes("Đo tái khám & Đổi tròng đa tròng")
                .build());

        appointmentRepository.save(EyeExamAppointment.builder()
                .ticketCode("KSK-102")
                .ticketType("Kiosk")
                .customerId(12L)
                .customerName("Trần Minh Quang")
                .customerPhone("0912 449 002")
                .customerTier("Thành viên")
                .status("WAITING")
                .statusLabel("Chờ gọi")
                .priorityLevel(2)
                .waitMinutes(12)
                .examStep("Sảnh chờ tiếp nhận")
                .examStepIndex(0)
                .scheduledTime("10:30 Hôm nay")
                .notes("Cắt kính cận mới, kiểm tra loạn thị")
                .build());

        appointmentRepository.save(EyeExamAppointment.builder()
                .ticketCode("VL-204")
                .ticketType("Vãng lai")
                .customerName("Đỗ Tuấn Kiệt")
                .customerPhone("0938 552 191")
                .customerTier("Khách mới")
                .status("WAITING")
                .statusLabel("Chờ gọi")
                .priorityLevel(3)
                .waitMinutes(20)
                .examStep("Sảnh chờ tiếp nhận")
                .examStepIndex(0)
                .scheduledTime("10:45 Hôm nay")
                .notes("Khách đo mắt vãng lai tại sảnh Kiosk")
                .build());

        log.info("Đã khởi tạo thành công {} đơn khúc xạ, {} lệnh lab, {} phiếu bảo hành và {} lượt khám vào db_lab!",
                prescriptionRepository.count(), labJobRepository.count(), warrantyRepository.count(), appointmentRepository.count());
    }
}
