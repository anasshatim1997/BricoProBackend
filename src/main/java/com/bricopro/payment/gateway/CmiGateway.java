package com.bricopro.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * CMI (Centre Monétique Interbancaire) — Morocco's main card payment gateway.
 *
 * ══════════════════════════════════════════════════════════════════════════
 *  HOW TO GET A CMI MERCHANT ACCOUNT (free to apply, takes 1-3 weeks):
 *  1. Go to https://www.cmi.co.ma  →  "Espace Commerçant"
 *  2. Download the "Demande d'adhésion e-commerce" form
 *  3. Submit: company registration (RC), bank RIB, ID
 *  4. CMI gives you:
 *       - clientid      (your merchant ID)
 *       - storekey      (your secret key for HMAC signing)
 *       - test endpoint: https://testpayment.cmi.co.ma/fim/est3Dgate
 *       - live endpoint: https://payment.cmi.co.ma/fim/est3Dgate
 *
 *  Set in your .env:
 *       CMI_CLIENT_ID=your_merchant_id
 *       CMI_STORE_KEY=your_secret_key
 *       CMI_CALLBACK_URL=https://api.yourapp.com/api/v1/payments/webhook/cmi
 *       CMI_OK_URL=https://yourapp.com/payment/success
 *       CMI_FAIL_URL=https://yourapp.com/payment/fail
 *       CMI_TEST_MODE=true  (false when going live)
 * ══════════════════════════════════════════════════════════════════════════
 *
 * CMI uses a 3D-Secure hosted payment page (HPP) flow:
 *   1. You POST a signed form to CMI's URL → user is redirected to their page
 *   2. User enters card details on CMI's secure server
 *   3. CMI POSTs back to your callbackUrl with the result
 *   4. You verify the HMAC signature and mark the payment complete
 */
@Component
@Slf4j
public class CmiGateway implements PaymentGateway {

    @Value("${payment.cmi.client-id:}")
    private String clientId;

    @Value("${payment.cmi.store-key:}")
    private String storeKey;

    @Value("${payment.cmi.callback-url:}")
    private String callbackUrl;

    @Value("${payment.cmi.ok-url:}")
    private String okUrl;

    @Value("${payment.cmi.fail-url:}")
    private String failUrl;

    @Value("${payment.cmi.test-mode:true}")
    private boolean testMode;

    private static final String TEST_ENDPOINT = "https://testpayment.cmi.co.ma/fim/est3Dgate";
    private static final String LIVE_ENDPOINT = "https://payment.cmi.co.ma/fim/est3Dgate";

    @Override
    public GatewayResult initiate(Long taskId, BigDecimal amount, String clientRef) {
        if (!isConfigured()) {
            log.warn("CMI not configured — falling back to cash reference. " +
                    "Set payment.cmi.client-id and payment.cmi.store-key in .env");
            return new GatewayResult("CMI-PENDING-" + clientRef, false, null);
        }

        // CMI HPP parameters
        // Amount must be in centimes (MAD × 100) as a string without decimal
        String amountStr = amount.multiply(BigDecimal.valueOf(100))
                .toBigInteger().toString();

        TreeMap<String, String> params = new TreeMap<>();
        params.put("clientid",    clientId);
        params.put("amount",      amountStr);
        params.put("currency",    "504");        // ISO 4217 for MAD
        params.put("oid",         clientRef);    // your order ID
        params.put("okUrl",       okUrl);
        params.put("failUrl",     failUrl);
        params.put("callbackUrl", callbackUrl);
        params.put("trantype",    "PreAuth");
        params.put("storetype",   "3d_pay_hosting");
        params.put("lang",        "fr");
        params.put("rnd",         String.valueOf(System.currentTimeMillis()));

        // HMAC-SHA512 signature — CMI requires this
        String hash = computeHash(params);
        params.put("hash", hash);

        // Build the HTML redirect form URL (the frontend will POST to this)
        // In practice your mobile app opens a WebView to this URL
        String endpoint = testMode ? TEST_ENDPOINT : LIVE_ENDPOINT;
        String formParams = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        // Return the redirect URL — the mobile app opens this in a WebView
        String redirectUrl = endpoint + "?" + formParams;

        log.info("CMI payment initiated: oid={} amount={} MAD mode={}",
                clientRef, amount, testMode ? "TEST" : "LIVE");

        return new GatewayResult(clientRef, false, redirectUrl);
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        if (!isConfigured()) return false;

        // CMI sends back a "HASH" in the callback — verify it
        String receivedHash = params.get("HASH");
        if (receivedHash == null) {
            log.error("CMI callback missing HASH parameter");
            return false;
        }

        // Rebuild the hash from the received params (excluding HASH itself)
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("HASH");
        String expectedHash = computeHash(sortedParams);

        boolean hashesMatch = MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                receivedHash.toUpperCase().getBytes(StandardCharsets.UTF_8));

        if (!hashesMatch) {
            log.error("CMI callback HASH mismatch — possible tampering. received={} expected={}",
                    receivedHash, expectedHash);
            return false;
        }

        // Check the response code — "00" = success
        String response = params.getOrDefault("Response", "");
        boolean success = "00".equals(response) || "Approved".equalsIgnoreCase(response);
        log.info("CMI callback verified: oid={} response={} success={}",
                params.get("oid"), response, success);
        return success;
    }

    @Override
    public String gatewayName() { return "CMI"; }

    private boolean isConfigured() {
        return !clientId.isBlank() && !storeKey.isBlank();
    }

    /**
     * CMI HMAC-SHA512 signature.
     * Concatenate all sorted param values + storeKey, then SHA-512 hex.
     */
    private String computeHash(TreeMap<String, String> params) {
        try {
            StringBuilder sb = new StringBuilder();
            params.values().forEach(sb::append);
            sb.append(storeKey);

            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("CMI hash computation failed", e);
        }
    }
}