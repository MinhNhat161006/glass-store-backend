package com.glassstore.order.domain;

import com.glassstore.common.exception.ApiException;
import com.glassstore.common.exception.ErrorCode;

/**
 * Guard for order status changes (SRS §4.6, BR-14, BR-17).
 *
 * <p>Every mutation of {@code orders.status} must pass through here so that no endpoint can
 * bypass business validation by writing the column directly.</p>
 */
public final class OrderStateMachine {

    private OrderStateMachine() {
    }

    /**
     * Validates a transition or throws.
     *
     * @param current the persisted status
     * @param target  the requested status
     */
    public static void requireTransition(OrderStatus current, OrderStatus target) {
        if (current == null) {
            throw ApiException.of(ErrorCode.ORDER_NOT_FOUND, "Đơn hàng chưa có trạng thái hợp lệ");
        }
        if (target == null) {
            throw ApiException.of(ErrorCode.VALIDATION_ERROR, "Trạng thái đích không được để trống");
        }
        if (current == target) {
            throw ApiException.of(ErrorCode.ORDER_INVALID_TRANSITION,
                    "Đơn hàng đã ở trạng thái " + current);
        }
        if (!current.canTransitionTo(target)) {
            throw ApiException.of(ErrorCode.ORDER_INVALID_TRANSITION,
                    "Không thể chuyển đơn hàng từ " + current + " sang " + target);
        }
    }

    /** Validates a customer-initiated cancellation request (OQ-10). */
    public static void requireCustomerCancellable(OrderStatus current) {
        if (!current.isCustomerCancellable()) {
            throw ApiException.of(ErrorCode.ORDER_NOT_CANCELLABLE,
                    "Đơn hàng ở trạng thái " + current + " không thể gửi yêu cầu hủy");
        }
    }

    /** Validates a staff-initiated cancellation (FR-POS-14). */
    public static void requireStaffCancellable(OrderStatus current) {
        if (!current.isStaffCancellable()) {
            throw ApiException.of(ErrorCode.ORDER_NOT_CANCELLABLE,
                    "Đơn hàng ở trạng thái " + current + " không thể hủy");
        }
    }
}