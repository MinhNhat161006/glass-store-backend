package com.glassstore.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLabJobRequest {

    @NotNull(message = "Mã đơn hàng (orderId) không được để trống")
    private Long orderId;

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    @NotNull(message = "Mã toa kính (prescriptionId) không được để trống")
    private Long prescriptionId;

    @NotBlank(message = "Mã tròng kính (lensSku) không được để trống")
    private String lensSku;

    private String lensName;

    @NotBlank(message = "Mã gọng kính (frameSku) không được để trống")
    private String frameSku;

    private String frameName;

    private Long technicianId;

    private String technicianName;

    private String technicianNotes;
}

