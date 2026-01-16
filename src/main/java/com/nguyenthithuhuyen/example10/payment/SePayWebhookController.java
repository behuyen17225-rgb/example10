package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.payload.request.SePayWebhookRequest;
import com.nguyenthithuhuyen.example10.security.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/sepay")
@RequiredArgsConstructor
public class SePayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SePayWebhookController.class);

    private final OrderService orderService;

    // 🔍 RAW body handler - debug webhook payload format
    @PostMapping("/webhook/raw")
    public ResponseEntity<String> sepayWebhookRaw(@RequestBody String rawBody) {
        log.info("🔔 RAW WEBHOOK BODY: {}", rawBody);
        return ResponseEntity.ok("Received");
    }

    // 🔍 DEBUG endpoint - log tất cả fields
    @PostMapping("/webhook/debug")
    public ResponseEntity<String> sepayWebhookDebug(@RequestBody SePayWebhookRequest req) {
        log.info("📋 WEBHOOK DEBUG - All fields:");
        log.info("  - description: {}", req.getDescription());
        log.info("  - amount: {}", req.getAmount());
        log.info("  - transaction_id: {}", req.getTransaction_id());
        log.info("  - bank: {}", req.getBank());
        log.info("  - status: {}", req.getStatus());
        log.info("  - reference_code: {}", req.getReference_code());
        return ResponseEntity.ok("Debug logged");
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> sepayWebhook(
            @RequestBody SePayWebhookRequest req) {

        log.info("🔔 WEBHOOK: description={}, amount={}", req.getDescription(), req.getAmount());
        
        if (req.getDescription() == null || req.getDescription().isBlank()) {
            log.error("❌ Description is empty!");
            return ResponseEntity.status(400).body("Description required");
        }
        
        try {
            orderService.markOrderPaidByWebhook(
                    req.getDescription(),
                    req.getAmount()
            );
            log.info("✅ Webhook OK");
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("❌ Webhook error: {}", e.getMessage());
            return ResponseEntity.status(500).body("ERROR");
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

            log.info("✅ Test OK");
            return ResponseEntity.ok("✅ Order " + orderId + " PAID");
        } catch (Exception e) {
            log.error("❌ Test error: {}", e.getMessage());
            return ResponseEntity.status(500).body("ERROR: " + e.getMessage());
        }
    }
}
