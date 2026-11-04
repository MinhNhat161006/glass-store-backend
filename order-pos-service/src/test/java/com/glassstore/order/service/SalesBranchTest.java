package com.glassstore.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class SalesBranchTest {
    @Test void explicitCodeWinsOverStaffHeadquarters() {
        assertEquals("Chi nhánh 01 - Lê Thánh Tôn", SalesBranch.resolve("BRANCH_01", "Trụ sở chính").name());
    }
    @Test void acceptsLegacyBranchName() {
        assertEquals("BRANCH_02", SalesBranch.resolve(null, "Chi nhánh 02 - Tràng Tiền (HN)").code());
    }
    @Test void rejectsHeadquartersLabAndUnknownCode() {
        assertThrows(ResponseStatusException.class, () -> SalesBranch.resolve(null, "Trụ Sở Chính (Headquarter)"));
        assertThrows(ResponseStatusException.class, () -> SalesBranch.resolve("LAB", null));
    }
}
