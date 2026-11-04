package com.glassstore.lab.service;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;
import com.glassstore.lab.dto.RepairServiceCreateRequest;
import com.glassstore.lab.dto.RepairServiceResponse;
import com.glassstore.lab.dto.RepairServiceStatusHistoryDTO;
import com.glassstore.lab.dto.RepairServiceStatusRequest;
import com.glassstore.lab.entity.RepairService;
import com.glassstore.lab.entity.RepairServiceStatusHistory;
import com.glassstore.lab.repository.RepairServiceRepository;
import com.glassstore.lab.repository.RepairServiceStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RepairServiceWorkflow {

    private static final Set<String> VALID_SERVICES = Set.of(
            "NOSE_PAD_REPLACEMENT", "SCREW_TIGHTENING", "SCREW_REPLACEMENT",
            "FRAME_ADJUSTMENT", "TITANIUM_FRAME_ADJUSTMENT", "ULTRASONIC_CLEANING",
            "OTHER_TECHNICAL_SERVICE");

    private final RepairServiceRepository repairRepository;
    private final RepairServiceStatusHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public List<RepairServiceResponse> search(String code, String phone, String status) {
        String normalizedStatus = normalizeStatus(status);
        return repairRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> code == null || code.isBlank()
                        || item.getServiceCode().equalsIgnoreCase(code.trim()))
                .filter(item -> phone == null || phone.isBlank()
                        || java.util.Objects.equals(item.getCustomerPhone(), phone.trim()))
                .filter(item -> normalizedStatus == null || item.getStatus().equals(normalizedStatus))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RepairServiceResponse get(Long id) {
        return repairRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND, "Không tìm thấy phiếu tiếp nhận"));
    }

    @Transactional
    public RepairServiceResponse create(RepairServiceCreateRequest request) {
        LocalDateTime receivedAt = LocalDateTime.now();
        if (request.expectedPickupAt().isBefore(receivedAt)) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR,
                    "Thời gian hẹn trả không được trước thời điểm tiếp nhận");
        }
        if (request.requestedServices().stream().anyMatch(service -> !VALID_SERVICES.contains(service))) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Loại dịch vụ không hợp lệ");
        }
        RepairService service = RepairService.builder()
                .customerName(request.customerName().trim())
                .customerPhone(request.customerPhone().trim())
                .productName(request.productName().trim())
                .productType(trimToNull(request.productType()))
                .productBarcode(trimToNull(request.productBarcode()))
                .frameCondition(request.frameCondition().trim())
                .lensCondition(request.lensCondition().trim())
                .reportedProblem(request.reportedProblem().trim())
                .physicalCondition(request.physicalCondition().trim())
                .requestedServices(new ArrayList<>(request.requestedServices().stream().distinct().toList()))
                .serviceType(request.requestedServices().get(0))
                .assignedTechnician(trimToNull(request.assignedTechnician()))
                .receptionEmployee(request.receptionEmployee().trim())
                .receivedAt(receivedAt)
                .expectedPickupAt(request.expectedPickupAt())
                .status("RECEIVED")
                .notes(trimToNull(request.notes()))
                .customerNotes(trimToNull(request.customerNotes()))
                .build();
        RepairService saved = repairRepository.save(service);
        historyRepository.save(RepairServiceStatusHistory.builder()
                .repairServiceId(saved.getId())
                .newStatus("RECEIVED")
                .changedBy(saved.getReceptionEmployee())
                .changedAt(receivedAt)
                .notes("Tiếp nhận ban đầu")
                .build());
        return toResponse(saved);
    }

    @Transactional
    public RepairServiceResponse updateStatus(Long id, RepairServiceStatusRequest request) {
        String newStatus = normalizeStatus(request.status());
        RepairService service = repairRepository.findById(id)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND, "Không tìm thấy phiếu tiếp nhận"));
        String previous = service.getStatus();
        boolean validTransition = ("RECEIVED".equals(previous) && "IN_PROGRESS".equals(newStatus))
                || ("IN_PROGRESS".equals(previous) && "READY_FOR_PICKUP".equals(newStatus));
        if (!validTransition) {
            throw ApiException.of(ErrorCode.REPAIR_SERVICE_INVALID_TRANSITION,
                    "Không thể chuyển trạng thái từ " + previous + " sang " + newStatus);
        }
        LocalDateTime now = LocalDateTime.now();
        service.setStatus(newStatus);
        if ("READY_FOR_PICKUP".equals(newStatus)) {
            service.setCompletedAt(now);
        }
        repairRepository.save(service);
        historyRepository.save(RepairServiceStatusHistory.builder()
                .repairServiceId(service.getId())
                .previousStatus(previous)
                .newStatus(newStatus)
                .changedBy(request.changedBy().trim())
                .changedAt(now)
                .notes(trimToNull(request.notes()))
                .build());
        return toResponse(service);
    }

    private RepairServiceResponse toResponse(RepairService service) {
        List<RepairServiceStatusHistoryDTO> history =
                historyRepository.findByRepairServiceIdOrderByChangedAtAsc(service.getId()).stream()
                        .map(entry -> new RepairServiceStatusHistoryDTO(entry.getPreviousStatus(), entry.getNewStatus(),
                                entry.getChangedBy(), entry.getChangedAt(), entry.getNotes()))
                        .toList();
        return new RepairServiceResponse(
                service.getId(), service.getServiceCode(), service.getCustomerName(), service.getCustomerPhone(),
                service.getProductName(), service.getProductType(), service.getProductBarcode(),
                service.getFrameCondition(), service.getLensCondition(), service.getReportedProblem(),
                service.getPhysicalCondition(), service.getRequestedServices(), service.getAssignedTechnician(),
                service.getReceptionEmployee(), service.getReceivedAt(), service.getExpectedPickupAt(),
                service.getCompletedAt(), service.getStatus(), service.getNotes(), service.getCustomerNotes(), history);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("RECEIVED", "IN_PROGRESS", "READY_FOR_PICKUP").contains(normalized)) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Trạng thái phiếu sửa chữa không hợp lệ");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
