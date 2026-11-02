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

        log.info("Bắt đầu nạp seed data tinh gọn (4 Nhân sự nội bộ + 1 Khách hàng mẫu)...");

        // 1. Roles
        Role adminRole = roleRepository.save(Role.builder().name("ROLE_ADMIN").description("Quản trị viên toàn hệ thống").build());
        Role staffRole = roleRepository.save(Role.builder().name("ROLE_SALES_STAFF").description("Nhân viên bán hàng & Thu ngân POS").build());
        Role optomRole = roleRepository.save(Role.builder().name("ROLE_OPTOMETRIST").description("Bác sĩ / KTV đo khám khúc xạ có CCHN").build());
        Role labRole = roleRepository.save(Role.builder().name("ROLE_LAB_TECH").description("Kỹ thuật viên xưởng gia công mài lắp kính").build());
        Role customerRole = roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("Khách hàng mua sắm đa kênh").build());

        String defaultPassHash = passwordEncoder.encode("Password2026@");

        // 2. Exact 4 Staff Members (1 Admin, 1 Thu ngân, 1 Bác sĩ, 1 KTV Lab)
        // 2.1 Quản trị viên (Admin)
        userRepository.save(User.builder()
                .username("admin")
                .email("admin@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Phạm Quản Trị Hệ Thống")
                .phoneNumber("0901000001")
                .employeeCode("ADM-OPT-001099")
                .branchName("Trụ Sở Chính (HQ TP.HCM)")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(adminRole)))
                .build());

        // 2.2 Thu ngân / Bán hàng POS
        userRepository.save(User.builder()
                .username("nv_pos")
                .email("pos.banhang@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Nguyễn Bán Hàng (Thu Ngân)")
                .phoneNumber("0903222333")
                .employeeCode("NV-OPT-202688")
                .branchName("Chi nhánh 01 - Flagship Lê Thánh Tôn, Quận 1")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(staffRole)))
                .build());

        // 2.3 Bác sĩ / KTV Khúc xạ
        userRepository.save(User.builder()
                .username("bs_tuyet")
                .email("bs.tuyet@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("BS. Lê Thị Ánh Tuyết (Khúc Xạ)")
                .phoneNumber("0903444555")
                .employeeCode("KTV-OPT-104592")
                .branchName("Phòng Khúc Xạ 01 - Lê Thánh Tôn, Quận 1")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(optomRole)))
                .build());

        // 2.4 Kỹ thuật viên mài lắp kính Lab
        userRepository.save(User.builder()
                .username("ktv_lab")
                .email("ktv.lab@optivision.vn")
                .passwordHash(defaultPassHash)
                .fullName("Trần Văn Kỹ Thuật (Lab Lens)")
                .phoneNumber("0903666777")
                .employeeCode("LAB-OPT-308115")
                .branchName("Xưởng Gia Công Lab Kính - Chi nhánh Quận 1")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(labRole)))
                .build());

        // 3. Khách hàng mẫu duy nhất (1 Customer)
        userRepository.save(User.builder()
                .username("cus_duy")
                .email("duy.nguyen@example.com")
                .passwordHash(defaultPassHash)
                .fullName("Nguyễn Hoàng Duy")
                .phoneNumber("0918732998")
                .employeeCode("KH-VIP-001")
                .branchName("120 Hai Bà Trưng, P. Đa Kao, Q.1, TP.HCM")
                .status("ACTIVE")
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build());

        log.info("Nạp seed data db_identity tinh gọn thành công (4 Nhân sự + 1 Khách hàng)!");
    }
}
