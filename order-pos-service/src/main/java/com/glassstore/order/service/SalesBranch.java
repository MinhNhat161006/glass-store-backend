package com.glassstore.order.service;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record SalesBranch(String code, String name) {
    public static SalesBranch resolve(String code, String name) {
        if (code != null && !code.isBlank()) {
            return switch (code) {
                case "BRANCH_01" -> new SalesBranch(code, "Chi nhánh 01 - Lê Thánh Tôn");
                case "BRANCH_02" -> new SalesBranch(code, "Chi nhánh 02 - Tràng Tiền (HN)");
                default -> throw invalid();
            };
        }
        String text = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replace('đ', 'd');
        if (text.contains("chi nhanh 01") || text.contains("le thanh ton") || text.contains("flagship q.1")) return resolve("BRANCH_01", null);
        if (text.contains("chi nhanh 02") || text.contains("trang tien") || text.contains("hai ba trung")) return resolve("BRANCH_02", null);
        throw invalid();
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn chi nhánh bán hàng hợp lệ; trụ sở và Lab không phải quầy POS.");
    }
}
