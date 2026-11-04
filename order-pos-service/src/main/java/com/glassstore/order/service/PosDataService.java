package com.glassstore.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.math.BigDecimal;

@Service
public class PosDataService {
    @Value("${services.catalog-service.url:http://localhost:8082}") private String catalogUrl;
    @Value("${services.identity-service.url:http://localhost:8081}") private String identityUrl;

    private JsonNode get(String url, String token) {
        try { return RestClient.create().get().uri(url).header("Authorization", token == null ? "" : token).retrieve().body(JsonNode.class); }
        catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 || error.getStatusCode().value() == 403) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ.");
            if (error.getStatusCode().value() == 404) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy khách hàng.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chưa kết nối được dữ liệu sản phẩm / thành viên.");
        } catch (org.springframework.web.client.RestClientException error) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ dữ liệu chưa sẵn sàng."); }
    }

    public JsonNode actor(String token, boolean adminOnly) {
        if (token == null || !token.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        JsonNode user = get(identityUrl + "/api/auth/me", token).path("data");
        Set<String> roles = new HashSet<>(); user.path("roles").forEach(role -> roles.add(role.asText()));
        if (!roles.contains("ROLE_ADMIN") && (adminOnly || !roles.contains("ROLE_SALES_STAFF"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ thu ngân / quản lý được thực hiện thao tác này.");
        return user;
    }

    public JsonNode customer(String token, Long id) {
        JsonNode customer = get(identityUrl + "/api/users/" + id, token).path("data");
        boolean member = false;
        for (JsonNode role : customer.path("roles")) if ("ROLE_CUSTOMER".equals(role.asText())) member = true;
        if (!member || !"ACTIVE".equals(customer.path("status").asText())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khách hàng chưa có hồ sơ thành viên đang hoạt động.");
        return customer;
    }

    public Map<String, BigDecimal> catalogPrices() {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (JsonNode product : get(catalogUrl + "/api/products", null).path("data")) {
            for (JsonNode variant : product.path("variants")) {
                if (!variant.path("price").isNumber()) continue;
                prices.put(variant.path("sku").asText(), variant.path("price").decimalValue());
            }
        }
        return prices;
    }
}
