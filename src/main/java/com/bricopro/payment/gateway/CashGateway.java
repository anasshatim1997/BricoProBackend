package com.bricopro.payment.gateway;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class CashGateway implements PaymentGateway {

    @Override
    public GatewayResult initiate(Long taskId, BigDecimal amount, String clientRef) {
        String ref = "CASH-" + clientRef + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new GatewayResult(ref, false, null);
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        return true;
    }

    @Override
    public String gatewayName() { return "CASH"; }
}