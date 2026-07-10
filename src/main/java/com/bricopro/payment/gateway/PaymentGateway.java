package com.bricopro.payment.gateway;

import java.math.BigDecimal;

/**
 * Every payment gateway implements this interface.
 * Today: CashGateway (always succeeds) and CmiGateway (stub, ready for real CMI).
 * Tomorrow: just implement this and register the bean.
 */
public interface PaymentGateway {

    /**
     * Initiates a payment and returns a gateway reference string.
     * For redirect-based gateways (CMI) this is the URL to redirect the user to.
     * For immediate gateways (CASH) this is just a receipt reference.
     *
     * @param taskId     internal task id
     * @param amount     gross amount in MAD
     * @param clientRef  your internal reference (e.g. "BRICO-{paymentId}")
     * @return GatewayResult with the reference and whether it's synchronous
     */
    GatewayResult initiate(Long taskId, BigDecimal amount, String clientRef);

    /**
     * Verifies a callback/webhook notification from the gateway.
     * Returns true if the payment is confirmed successful.
     */
    boolean verifyCallback(java.util.Map<String, String> params);

    String gatewayName();

    record GatewayResult(
            String reference,
            boolean synchronous,   // true = payment done immediately, false = async (redirect)
            String redirectUrl     // null for synchronous gateways
    ) {}
}