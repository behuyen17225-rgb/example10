package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.payload.request.SePayWebhookRequest;
import com.nguyenthithuhuyen.example10.security.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.Base64;

@RestController
@RequestMapping("/api/sepay")
@RequiredArgsConstructor
public class SePayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SePayWebhookController.class);

    private final OrderService orderService;

    @Value("${sepay.api-secret}")
    private String sepayApiSecret;

    // 🔍 RAW body handler - debug webhook payload format
    @PostMapping("/webhook/raw")
    public ResponseEntity<String> sepayWebhookRaw(@RequestBody String rawBody) {
        log.debug("========================================");
        log.debug("🔔 RAW WEBHOOK BODY (STRING):");
        log.debug(rawBody);
        log.debug("========================================");
        return ResponseEntity.ok("Received RAW: " + rawBody.substring(0, Math.min(100, rawBody.length())));
    }

    // 🔍 DEBUG endpoint - log tất cả fields
    @PostMapping("/webhook/debug")
    public ResponseEntity<String> sepayWebhookDebug(@RequestBody SePayWebhookRequest req) {
        log.debug("📋 WEBHOOK DEBUG - All fields:");
        log.debug("  - content: {}", req.getContent());
        log.debug("  - description: {}", req.getDescription());
        log.debug("  - amount: {}", req.getAmount());
        log.debug("  - transactionDate: {}", req.getTransactionDate());
        log.debug("  - referenceCode: {}", req.getReferenceCode());
        log.debug("  - senderName: {}", req.getSenderName());
        log.debug("  - senderAccount: {}", req.getSenderAccount());
        log.debug("  - otherFields: {}", req.getOtherFields());
        return ResponseEntity.ok("Debug logged");
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> sepayWebhook(
            @RequestBody SePayWebhookRequest req,
            @RequestHeader(value = "X-Sepay-Signature", required = false) String signature) {

        log.debug("🔔 WEBHOOK received from SePay");
        log.debug("  - content: {}", req.getContent());
        log.debug("  - amount: {}", req.getAmount());
        log.debug("  - signature: {}", signature);
        
        // ===== STEP 1: VERIFY SIGNATURE =====
        if (signature == null || signature.isBlank()) {
            log.error("❌ MISSING SIGNATURE - This might be fake data!");
            return ResponseEntity.status(401).body("Signature required");
        }
        
        // Tạo data string để verify
        String dataToSign = req.getContent() + "|" + req.getAmount();
        String expectedSignature = generateHmacSha256(dataToSign, sepayApiSecret);
        
        if (!signature.equals(expectedSignature)) {
            log.error("❌ INVALID SIGNATURE - FAKE DATA DETECTED!");
            log.error("   Expected: {}", expectedSignature);
            log.error("   Got: {}", signature);
            return ResponseEntity.status(401).body("Invalid signature");
        }
        
        log.info("✅ Signature verified - Data from SePay is authentic");
        
        // ===== STEP 2: VALIDATE CONTENT =====
        if (req.getContent() == null || req.getContent().isBlank()) {
            log.error("❌ Content is empty!");
            return ResponseEntity.status(400).body("Content required");
        }
        
        // ===== STEP 3: PROCESS WEBHOOK =====
        try {
            log.debug("Processing order payment...");
            orderService.markOrderPaidByWebhook(
                    req.getContent(),
                    req.getAmount()
            );
            log.info("✅ Webhook processed successfully");
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("❌ Webhook error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("ERROR");
        }
    }
    
    /* ===== HELPER: Generate HMAC SHA256 Signature ===== */
    private String generateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] hashBytes = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            log.error("❌ Error generating signature: {}", e.getMessage());
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    // 🧪 TEST endpoint - GET & POST
    @GetMapping("/webhook/test")
    @PostMapping("/webhook/test")
    public ResponseEntity<String> testWebhook(
            @RequestParam Long orderId,
            @RequestParam(required = false) BigDecimal amount) {

        try {
            String content = "ORDER_" + orderId;
            BigDecimal testAmount = amount != null ? amount : new BigDecimal(50000);

            orderService.markOrderPaidByWebhook(content, testAmount);

            log.debug("✅ Test OK");
            return ResponseEntity.ok("✅ Order " + orderId + " PAID");
        } catch (Exception e) {
            log.error("❌ Test error: {}", e.getMessage());
            return ResponseEntity.status(500).body("ERROR: " + e.getMessage());
        }
    }
}

