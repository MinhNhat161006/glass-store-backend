package com.glassstore.order.payment;

import com.glassstore.order.domain.PaymentMethod;

import java.math.BigDecimal;

/**
 * Abstraction over a real payment provider (FR-PAY-01/02/06/07).
 *
 * <p>Order and payment state never change because the client said so — only a verified
 * result from an implementation of this interface may mark money as received (BR-16).</p>
 */
public interface PaymentGateway {

    boolean supports(PaymentMethod method);

    /**
     * Starts a charge.
     *
     * @throws com.glassstore.common.exception.ApiException with
     *         {@code PAYMENT_PROVIDER_NOT_CONFIGURED} when no provider is wired up,
     *         so the caller cannot mistake "not configured" for "paid".
     */
    InitiationResult initiate(InitiationRequest request);

    /**
     * Verifies a provider callback.
     *
     * @param rawPayload the exact bytes received, needed for signature verification
     * @param signature  the provider's signature header
     */
    VerificationResult verifyCallback(String rawPayload, String signature);

    record InitiationRequest(String orderCode, BigDecimal amount, String description) {
    }

    record InitiationResult(String provider, String externalTransactionId, String paymentUrl,
                            String qrContent, boolean requiresCustomerAction) {
    }

    record VerificationResult(boolean valid, String provider, String externalTransactionId,
                              String orderCode, BigDecimal amount, boolean success, String message) {
    }
}