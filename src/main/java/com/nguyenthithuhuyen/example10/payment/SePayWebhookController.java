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

    @PostMapping("/webhook")
    public ResponseEntity<String> sepayWebhook(
            @RequestBody SePayWebhookRequest req) {

        log.info("🔔 WEBHOOK RECEIVED: content={}, amount={}", req.getContent(), req.getAmount());
        log.info("Full webhook payload: {}", req.toString());
        
        try {
            orderService.markOrderPaidByWebhook(
                    req.getContent(),
                    req.getAmount()
            );
            log.info("✅ Webhook processed successfully");
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("❌ Webhook error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("ERROR: " + e.getMessage());
        }
    }

    // 🧪 TEST endpoint - không cần thanh toán thực tế
    @PostMapping("/webhook/test")
    public ResponseEntity<String> testWebhook(
            @RequestParam Long orderId,
            @RequestParam(required = false) BigDecimal amount) {

        log.info("🧪 TEST WEBHOOK: orderId={}, amount={}", orderId, amount);

        try {
            String content = "ORDER_" + orderId;
            BigDecimal testAmount = amount != null ? amount : new BigDecimal(50000);

            orderService.markOrderPaidByWebhook(content, testAmount);

            log.info("✅ Test webhook success");
            return ResponseEntity.ok("✅ Test passed! Order " + orderId + " marked as PAID");
        } catch (Exception e) {
            log.error("❌ Test webhook error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("❌ Test failed: " + e.getMessage());
        }
    }
}
