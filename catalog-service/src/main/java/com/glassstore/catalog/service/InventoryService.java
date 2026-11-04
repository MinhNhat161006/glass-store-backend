package com.glassstore.catalog.service;

import com.glassstore.catalog.dto.*;
import com.glassstore.catalog.entity.*;
import com.glassstore.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {
    @Transactional(readOnly = true)
    public void checkAvailability(StockCheckRequest request) {
        if (!Set.of(BRANCH_01, BRANCH_02).contains(request.branchCode())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi nhánh POS không hợp lệ.");
        Map<String, Integer> quantities = new TreeMap<>();
        request.items().forEach(item -> quantities.merge(item.sku(), item.quantity(), Math::addExact));
        quantities.forEach((sku, quantity) -> {
            InventoryBalance balance = balanceRepository.findBySkuAndLocationCode(sku, request.branchCode())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "SKU không có ở chi nhánh: " + sku));
            if (balance.getQuantity() < quantity || balance.getVariant().getStockQuantity() < quantity)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Không đủ tồn tại chi nhánh cho SKU " + sku);
        });
    }

    public static final String BRANCH_01 = "BRANCH_01";
    public static final String BRANCH_02 = "BRANCH_02";
    public static final String LAB = "LAB";

    private final InventoryLocationRepository locationRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryDeductionRepository deductionRepository;
    private final StockTransferRepository transferRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional(readOnly = true)
    public List<InventoryLocationDTO> getLocations() {
        return locationRepository.findAllByActiveTrueOrderByCodeAsc().stream()
                .map(location -> new InventoryLocationDTO(location.getCode(), location.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryRowDTO> getInventory() {
        Map<String, InventoryRowDTO> rows = new TreeMap<>();
        List<InventoryLocation> locations = locationRepository.findAllByActiveTrueOrderByCodeAsc();
        for (InventoryBalance balance : balanceRepository.findAllWithDetails()) {
            ProductVariant variant = balance.getVariant();
            InventoryRowDTO row = rows.computeIfAbsent(variant.getSku(), sku ->
                    InventoryRowDTO.builder()
                            .sku(sku)
                            .productName(variant.getProduct().getName())
                            .productType(variant.getProduct().getProductType().name())
                            .quantities(locations.stream()
                                    .collect(Collectors.toMap(
                                            InventoryLocation::getCode,
                                            ignored -> 0,
                                            (left, right) -> left,
                                            LinkedHashMap::new)))
                            .build());
            row.getQuantities().put(balance.getLocation().getCode(), balance.getQuantity());
        }
        return new ArrayList<>(rows.values());
    }

    @Transactional(readOnly = true)
    public List<StockTransferDTO> getTransfers() {
        return transferRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toTransferDto)
                .toList();
    }

    @Transactional
    public StockTransferDTO createTransfer(CreateStockTransferRequest request) {
        String destinationCode = request.getDestinationLocationCode().trim().toUpperCase(Locale.ROOT);
        if (!BRANCH_01.equals(destinationCode) && !BRANCH_02.equals(destinationCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kho nhận phải là Chi nhánh 01 hoặc Chi nhánh 02.");
        }

        InventoryLocation source = locationRepository.findByCode(LAB)
                .orElseThrow(() -> new IllegalStateException("Lab inventory location is not initialized."));
        InventoryLocation destination = locationRepository.findByCode(destinationCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kho nhận không hợp lệ."));

        Set<String> seenSkus = new HashSet<>();
        List<StockTransferItem> items = new ArrayList<>();
        for (CreateStockTransferRequest.Item requestedItem : request.getItems()) {
            String sku = requestedItem.getSku().trim();
            if (!seenSkus.add(sku)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi SKU chỉ được thêm một lần vào phiếu.");
            }
            ProductVariant variant = variantRepository.findBySku(sku)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy SKU " + sku + "."));
            InventoryBalance sourceBalance = balanceRepository.findBySkuAndLocationCode(sku, LAB)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "SKU " + sku + " chưa có số liệu tồn tại kho Lab."));
            if (requestedItem.getQuantity() > sourceBalance.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Số lượng SKU " + sku + " vượt tồn kho Lab hiện tại.");
            }
            items.add(StockTransferItem.builder()
                    .variant(variant)
                    .sku(sku)
                    .productName(variant.getProduct().getName())
                    .quantity(requestedItem.getQuantity())
                    .build());
        }

        StockTransfer transfer = StockTransfer.builder()
                .transferCode("TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT))
                .sourceLocation(source)
                .destinationLocation(destination)
                .reason(request.getReason() == null ? null : request.getReason().trim())
                .status(StockTransferStatus.PENDING)
                .items(items)
                .build();
        items.forEach(item -> item.setTransfer(transfer));
        return toTransferDto(transferRepository.save(transfer));
    }

    @Transactional
    public StockTransferDTO dispatchTransfer(Long id) {
        StockTransfer transfer = transferRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu điều chuyển."));
        if (transfer.getStatus() != StockTransferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu điều chuyển không còn ở trạng thái chờ xuất kho.");
        }

        for (StockTransferItem item : sortedItems(transfer.getItems())) {
            int balanceUpdated = balanceRepository.decrementIfAvailable(
                    item.getSku(), transfer.getSourceLocation().getCode(), item.getQuantity());
            int productUpdated = variantRepository.decrementStockIfAvailable(item.getSku(), item.getQuantity());
            if (balanceUpdated != 1 || productUpdated != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Không đủ tồn khả dụng để xuất SKU " + item.getSku() + ".");
            }
            movementRepository.save(InventoryMovement.builder()
                    .movementType("TRANSFER_DISPATCH")
                    .sku(item.getSku())
                    .sourceLocationCode(transfer.getSourceLocation().getCode())
                    .destinationLocationCode(transfer.getDestinationLocation().getCode())
                    .referenceType("STOCK_TRANSFER")
                    .referenceId(transfer.getTransferCode())
                    .quantity(item.getQuantity())
                    .performedBy("INTERNAL_PORTAL")
                    .build());
        }

        transfer.setStatus(StockTransferStatus.IN_TRANSIT);
        transfer.setDispatchedAt(LocalDateTime.now());
        return toTransferDto(transfer);
    }

    @Transactional
    public StockTransferDTO receiveTransfer(Long id) {
        StockTransfer transfer = transferRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu điều chuyển."));
        if (transfer.getStatus() != StockTransferStatus.IN_TRANSIT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ phiếu đang vận chuyển mới được xác nhận nhận hàng.");
        }

        for (StockTransferItem item : sortedItems(transfer.getItems())) {
            int balanceUpdated = balanceRepository.increment(
                    item.getSku(), transfer.getDestinationLocation().getCode(), item.getQuantity());
            int productUpdated = variantRepository.incrementStock(item.getSku(), item.getQuantity());
            if (balanceUpdated != 1 || productUpdated != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể cập nhật tồn kho cho SKU " + item.getSku() + ".");
            }
            movementRepository.save(InventoryMovement.builder()
                    .movementType("TRANSFER_RECEIVE")
                    .sku(item.getSku())
                    .sourceLocationCode(transfer.getSourceLocation().getCode())
                    .destinationLocationCode(transfer.getDestinationLocation().getCode())
                    .referenceType("STOCK_TRANSFER")
                    .referenceId(transfer.getTransferCode())
                    .quantity(item.getQuantity())
                    .performedBy("INTERNAL_PORTAL")
                    .build());
        }

        transfer.setStatus(StockTransferStatus.RECEIVED);
        transfer.setReceivedAt(LocalDateTime.now());
        return toTransferDto(transfer);
    }

    @Transactional
    public StockDeductionResponse deductPaidOrder(StockDeductionRequest request) {
        String locationCode = resolveLocationCode(request.getBranchName());
        TreeMap<String, Integer> quantitiesBySku = new TreeMap<>();
        for (StockDeductionRequest.Item item : request.getItems()) {
            quantitiesBySku.merge(item.getSku().trim(), item.getQuantity(), Integer::sum);
        }

        Optional<InventoryDeduction> previous = deductionRepository.findByOrderId(request.getOrderId())
                .or(() -> deductionRepository.findByIdempotencyKey(request.getIdempotencyKey()));
        if (previous.isPresent()) {
            InventoryDeduction deduction = previous.get();
            if (Boolean.TRUE.equals(deduction.getCancelled())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đã hủy, không được trừ kho lại.");
            if (!deduction.getOrderId().equals(request.getOrderId())
                    || !deduction.getIdempotencyKey().equals(request.getIdempotencyKey())
                    || !deduction.getTransactionCode().equals(request.getTransactionCode())
                    || !deduction.getLocationCode().equals(locationCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key or order was already used for a different payment.");
            }
            return new StockDeductionResponse(request.getOrderId(), locationCode, true);
        }

        for (String sku : quantitiesBySku.keySet()) {
            if (!variantRepository.existsBySku(sku)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy SKU " + sku + ".");
            }
            if (balanceRepository.findBySkuAndLocationCode(sku, locationCode).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU " + sku + " chưa được cấu hình tồn tại địa điểm " + locationCode + ".");
            }
        }

        deductionRepository.saveAndFlush(InventoryDeduction.builder()
                .orderId(request.getOrderId())
                .idempotencyKey(request.getIdempotencyKey())
                .transactionCode(request.getTransactionCode())
                .locationCode(locationCode)
                .build());

        for (Map.Entry<String, Integer> entry : quantitiesBySku.entrySet()) {
            String sku = entry.getKey();
            int quantity = entry.getValue();
            int balanceUpdated = balanceRepository.decrementIfAvailable(sku, locationCode, quantity);
            int productUpdated = variantRepository.decrementStockIfAvailable(sku, quantity);
            if (balanceUpdated != 1 || productUpdated != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Không đủ tồn khả dụng cho SKU " + sku + " tại " + locationCode + ".");
            }
            movementRepository.save(InventoryMovement.builder()
                    .movementType("SALE")
                    .sku(sku)
                    .sourceLocationCode(locationCode)
                    .referenceType("PAYMENT")
                    .referenceId(request.getTransactionCode())
                    .quantity(quantity)
                    .performedBy("ORDER_SERVICE")
                    .build());
        }
        return new StockDeductionResponse(request.getOrderId(), locationCode, false);
    }

    @Transactional
    public void cancelSale(StockDeductionRequest request) {
        InventoryDeduction deduction = deductionRepository.findByOrderId(request.getOrderId()).orElse(null);
        if (deduction == null) {
            deduction = InventoryDeduction.builder().orderId(request.getOrderId()).idempotencyKey(request.getIdempotencyKey())
                    .transactionCode(request.getTransactionCode()).locationCode("NONE").build();
            deduction.setCancelled(true); deductionRepository.saveAndFlush(deduction); return;
        }
        if (!deduction.getIdempotencyKey().equals(request.getIdempotencyKey()) || !deduction.getTransactionCode().equals(request.getTransactionCode()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Thông tin hoàn kho không khớp giao dịch gốc.");
        if (Boolean.TRUE.equals(deduction.getCancelled())) return;
        TreeMap<String,Integer> items = new TreeMap<>(); request.getItems().forEach(item -> items.merge(item.getSku(),item.getQuantity(),Math::addExact));
        for (Map.Entry<String,Integer> item : items.entrySet()) {
            if (balanceRepository.increment(item.getKey(),deduction.getLocationCode(),item.getValue()) != 1 || variantRepository.incrementStock(item.getKey(),item.getValue()) != 1)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Không tìm thấy tồn gốc để hoàn SKU " + item.getKey());
            movementRepository.save(InventoryMovement.builder().movementType("SALE_CANCEL").sku(item.getKey())
                    .destinationLocationCode(deduction.getLocationCode()).referenceType("ORDER_CANCEL")
                    .referenceId(request.getOrderId().toString()).quantity(item.getValue()).performedBy("ORDER_SERVICE").build());
        }
        deduction.setCancelled(true); deductionRepository.save(deduction);
    }

    private String resolveLocationCode(String branchName) {
        String normalized = Normalizer.normalize(branchName.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        if (normalized.contains("lab") || normalized.contains("tong kho")) {
            return LAB;
        }
        if (normalized.contains("branch 01") || normalized.contains("chi nhanh 01")
                || normalized.contains("cn 01") || normalized.contains("flagship")
                || normalized.contains("le thanh ton")) {
            return BRANCH_01;
        }
        if (normalized.contains("branch 02") || normalized.contains("chi nhanh 02")
                || normalized.contains("cn 02") || normalized.contains("hai ba trung")
                || normalized.contains("trang tien")) {
            return BRANCH_02;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không xác định được kho bán hàng từ tên chi nhánh.");
    }

    private List<StockTransferItem> sortedItems(List<StockTransferItem> items) {
        return items.stream().sorted(Comparator.comparing(StockTransferItem::getSku)).toList();
    }

    private StockTransferDTO toTransferDto(StockTransfer transfer) {
        return StockTransferDTO.builder()
                .id(transfer.getId())
                .transferCode(transfer.getTransferCode())
                .sourceLocationCode(transfer.getSourceLocation().getCode())
                .sourceLocationName(transfer.getSourceLocation().getName())
                .destinationLocationCode(transfer.getDestinationLocation().getCode())
                .destinationLocationName(transfer.getDestinationLocation().getName())
                .reason(transfer.getReason())
                .status(transfer.getStatus().name())
                .createdAt(transfer.getCreatedAt())
                .dispatchedAt(transfer.getDispatchedAt())
                .receivedAt(transfer.getReceivedAt())
                .items(transfer.getItems().stream()
                        .map(item -> StockTransferItemDTO.builder()
                                .sku(item.getSku())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();
    }
}
