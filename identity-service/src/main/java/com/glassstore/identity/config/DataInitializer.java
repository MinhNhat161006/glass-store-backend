package com.glassstore.identity.config;

import com.glassstore.identity.entity.Role;
import com.glassstore.identity.entity.User;
import com.glassstore.identity.repository.RoleRepository;
import com.glassstore.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0 && userRepository.count() > 0) {
            log.info("Database đã có dữ liệu nhân sự, bỏ qua bước khởi tạo seed.");
            return;
        }

        log.info("Bắt đầu nạp seed data nhân sự và khách hàng chuẩn SRS vào db_identity...");

        // 1. Roles
        Role adminRole = roleRepository.save(Role.builder().name("ROLE_ADMIN").description("Quản trị viên toàn hệ thống").build());
        Role staffRole = roleRepository.save(Role.builder().name("ROLE_SALES_STAFF").description("Nhân viên bán hàng & Thu ngân POS").build());
        Role optomRole = roleRepository.save(Role.builder().name("ROLE_OPTOMETRIST").description("Bác sĩ / KTV đo khám khúc xạ có CCHN").build());
        Role labRole = roleRepository.save(Role.builder().name("ROLE_LAB_TECH").description("Kỹ thuật viên xưởng gia công mài lắp kính").build());
        Role customerRole = roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("Khách hàng mua sắm đa kênh").build());

        String defaultPassHash = passwordEncoder.encode("Password2026@");

        // 2. Staff members
        userRepository.save(User.builder()
                .username("admin")
                .email("admin@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Trần Quản Trị")
                .phoneNumber("0901000001")
                .employeeCode("STF-001")
                .branchName("Trụ Sở Chính (Headquarter)")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(adminRole)))
                .build());

        userRepository.save(User.builder()
                .username("manager_nam")
                .email("nam.le@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Lê Hoàng Nam")
                .phoneNumber("0901000014")
                .employeeCode("STF-014")
                .branchName("Tràng Tiền Plaza (Hà Nội)")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(adminRole)))
                .build());

        userRepository.save(User.builder()
                .username("staff_minhanh")
                .email("minhanh.tran@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Trần Minh Anh")
                .phoneNumber("0903882190")
                .employeeCode("STF-088")
                .branchName("Flagship Q.1 (TP.HCM)")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(staffRole)))
                .build());

        userRepository.save(User.builder()
                .username("staff_ha")
                .email("ha.le@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Lê Văn Hà")
                .phoneNumber("0912345678")
                .employeeCode("STF-012")
                .branchName("Tràng Tiền Plaza (Hà Nội)")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(staffRole)))
                .build());

        userRepository.save(User.builder()
                .username("staff_vy")
                .email("vy.nguyen@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Nguyễn Thảo Vy")
                .phoneNumber("0903112233")
                .employeeCode("STF-092")
                .branchName("Thảo Điền Store (Q.2)")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(staffRole)))
                .build());

        userRepository.save(User.builder()
                .username("optom_tuan")
                .email("tuan.vu@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("BS. CKI Vũ Anh Tuấn")
                .phoneNumber("0901000003")
                .employeeCode("STF-021")
                .branchName("Flagship Q.1 (TP.HCM)")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(optomRole)))
                .build());

        userRepository.save(User.builder()
                .username("optom_phuc")
                .email("phuc.lam@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("BS. Lâm Hoàng Phúc")
                .phoneNumber("0908889999")
                .employeeCode("STF-102")
                .branchName("Flagship Q.1 (TP.HCM)")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(optomRole)))
                .build());

        userRepository.save(User.builder()
                .username("lab_bach")
                .email("bach.tran@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Trần Văn Bách")
                .phoneNumber("0901000004")
                .employeeCode("STF-045")
                .branchName("Lab Flagship Q.1")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(labRole)))
                .build());

        userRepository.save(User.builder()
                .username("lab_dinh")
                .email("dinh.nguyen@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Nguyễn Văn Định")
                .phoneNumber("0988776655")
                .employeeCode("STF-078")
                .branchName("Lab Tràng Tiền HN")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(labRole)))
                .build());

        // Internal Portal Quick Profiles
        userRepository.save(User.builder()
                .username("nv_pos_202688")
                .email("pos.banhang@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Nguyễn Bán Hàng (Thu Ngân)")
                .phoneNumber("0903222333")
                .employeeCode("NV-OPT-202688")
                .branchName("Chi nhánh 01 - Flagship Lê Thánh Tôn, Quận 1, TP.HCM")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(staffRole)))
                .build());

        userRepository.save(User.builder()
                .username("ktv_opt_104592")
                .email("bs.tuyet@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("BS. Lê Thị Ánh Tuyết (Khúc Xạ)")
                .phoneNumber("0903444555")
                .employeeCode("KTV-OPT-104592")
                .branchName("Chi nhánh 01 - Flagship Lê Thánh Tôn, Quận 1, TP.HCM")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(optomRole)))
                .build());

        userRepository.save(User.builder()
                .username("lab_opt_308115")
                .email("ktv.lab@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Trần Văn Kỹ Thuật (Lab Lens)")
                .phoneNumber("0903666777")
                .employeeCode("LAB-OPT-308115")
                .branchName("Trung tâm Gia công & Lab Lens Tân Bình, TP.HCM")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(labRole)))
                .build());

        userRepository.save(User.builder()
                .username("adm_opt_001099")
                .email("admin.portal@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Phạm Quản Trị Hệ Thống")
                .phoneNumber("0903999888")
                .employeeCode("ADM-OPT-001099")
                .branchName("Trụ sở Quản trị Toàn quốc (OptiVision Head Office)")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(adminRole)))
                .build());

        // 3. Customers
        userRepository.save(User.builder()
                .username("cus_duy")
                .email("duy.nguyen@example.com")
                .passwordHash(defaultPassHash)
                .fullName("Nguyễn Hoàng Duy")
                .phoneNumber("0918732998")
                .employeeCode("VIP-01")
                .branchName("120 Hai Bà Trưng, P. Đa Kao, Q.1, TP.HCM")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build());

        userRepository.save(User.builder()
                .username("cus_kieu")
                .email("thuykieu.le@gmail.com")
                .passwordHash(defaultPassHash)
                .fullName("Lê Thúy Kiều")
                .phoneNumber("0988124551")
                .employeeCode("VIP-02")
                .branchName("45 Lê Duẩn, P. Bến Nghé, Q.1, TP.HCM")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build());

        userRepository.save(User.builder()
                .username("cus_quang")
                .email("quang.trinh@gmail.com")
                .passwordHash(defaultPassHash)
                .fullName("Trịnh Minh Quang")
                .phoneNumber("0912449002")
                .employeeCode("MEM-03")
                .branchName("88 Nguyễn Đình Chiểu, Q.3, TP.HCM")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build());

        userRepository.save(User.builder()
                .username("cus_ngoc")
                .email("bichngoc.pham@gmail.com")
                .passwordHash(defaultPassHash)
                .fullName("Phạm Bích Ngọc")
                .phoneNumber("0933778811")
                .employeeCode("MEM-04")
                .branchName("204 Pasteur, P. Võ Thị Sáu, Q.3, TP.HCM")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build());

        log.info("Nạp seed data db_identity hoàn tất thành công!");
    }
}
