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
        log.info("Khởi tạo và đồng bộ seed data danh sách nhân viên tinh gọn...");

        // 1. Roles
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").description("Quản trị viên toàn hệ thống").build()));
        Role staffRole = roleRepository.findByName("ROLE_SALES_STAFF")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_SALES_STAFF").description("Nhân viên bán hàng & Thu ngân POS").build()));
        Role optomRole = roleRepository.findByName("ROLE_OPTOMETRIST")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OPTOMETRIST").description("Bác sĩ / KTV đo khám khúc xạ có CCHN").build()));
        Role labRole = roleRepository.findByName("ROLE_LAB_TECH")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_LAB_TECH").description("Kỹ thuật viên xưởng gia công mài lắp kính").build()));
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("Khách hàng mua sắm đa kênh").build()));

        String defaultPassHash = passwordEncoder.encode("Password2026@");

        // 2. Clean 4 Staff Members
        // 2.1 Admin
        User admin = userRepository.findByUsername("admin").orElse(User.builder().username("admin").build());
        admin.setEmail("adminoptic@gmail.com");
        admin.setFullName("Admin");
        admin.setPasswordHash(defaultPassHash);
        admin.setPhoneNumber("0901000001");
        admin.setEmployeeCode("ADM-001");
        admin.setBranchName("Trụ Sở Chính (HQ TP.HCM)");
        admin.setStatus("ACTIVE");
        admin.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
        userRepository.save(admin);

        // 2.2 Thu ngân / Bán hàng POS (Nguyễn Văn Nam)
        User pos = userRepository.findByUsername("pos")
                .or(() -> userRepository.findByUsername("nv_pos"))
                .or(() -> userRepository.findByUsername("staff"))
                .orElse(User.builder().username("pos").build());
        pos.setUsername("pos");
        pos.setEmail("posoptic@gmail.com");
        pos.setFullName("Nguyễn Văn Nam");
        pos.setPasswordHash(defaultPassHash);
        pos.setPhoneNumber("0903222333");
        pos.setEmployeeCode("NV-001");
        pos.setBranchName("Chi nhánh 01 - Lê Thánh Tôn, Quận 1");
        pos.setStatus("ACTIVE");
        pos.setRoles(new HashSet<>(Collections.singletonList(staffRole)));
        userRepository.save(pos);

        // 2.3 Khúc xạ (Lê Thị Ánh Tuyết)
        User optometry = userRepository.findByUsername("optometry")
                .or(() -> userRepository.findByUsername("bs_tuyet"))
                .or(() -> userRepository.findByUsername("optometrist"))
                .orElse(User.builder().username("optometry").build());
        optometry.setUsername("optometry");
        optometry.setEmail("optometryoptic@gmail.com");
        optometry.setFullName("Lê Thị Ánh Tuyết");
        optometry.setPasswordHash(defaultPassHash);
        optometry.setPhoneNumber("0903444555");
        optometry.setEmployeeCode("KTV-001");
        optometry.setBranchName("Phòng Khúc Xạ 01 - Lê Thánh Tôn, Quận 1");
        optometry.setStatus("ACTIVE");
        optometry.setRoles(new HashSet<>(Collections.singletonList(optomRole)));
        userRepository.save(optometry);

        // 2.4 KTV Lab (Trần Văn Tuấn)
        User lab = userRepository.findByUsername("lab")
                .or(() -> userRepository.findByUsername("ktv_lab"))
                .or(() -> userRepository.findByUsername("labtech"))
                .orElse(User.builder().username("lab").build());
        lab.setUsername("lab");
        lab.setEmail("laboptic@gmail.com");
        lab.setFullName("Trần Văn Tuấn");
        lab.setPasswordHash(defaultPassHash);
        lab.setPhoneNumber("0903666777");
        lab.setEmployeeCode("LAB-001");
        lab.setBranchName("Xưởng Gia Công Lab Kính - Chi nhánh Quận 1");
        lab.setStatus("ACTIVE");
        lab.setRoles(new HashSet<>(Collections.singletonList(labRole)));
        userRepository.save(lab);

        // 3. Khách hàng mẫu
        if (userRepository.findByUsername("cus_duy").isEmpty()) {
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
        }

        log.info("Nạp & đồng bộ seed data nhân sự tinh gọn thành công (Admin, Nguyễn Văn Nam, Lê Thị Ánh Tuyết, Trần Văn Tuấn)!");
    }
}
