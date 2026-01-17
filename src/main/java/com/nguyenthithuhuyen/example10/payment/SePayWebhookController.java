package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.payload.request.SePayWebhookRequest;
import com.nguyenthithuhuyen.example10.repository.OrderRepository;
import com.nguyenthithuhuyen.example10.security.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/sepay")
@RequiredArgsConstructor
public class SePayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SePayWebhookController.class);

    private final OrderService orderService;
    private final OrderRepository orderRepository;

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
            @RequestBody SePayWebhookRequest req) {

        log.info("🔔 WEBHOOK received from SePay");
        log.debug("  - content: {}", req.getContent());
        log.debug("  - amount: {}", req.getAmount());
        
        // ===== VALIDATE CONTENT =====
        if (req.getContent() == null || req.getContent().isBlank()) {
            log.error("❌ Content is empty!");
            return ResponseEntity.status(400).body("Content required");
        }
        
        // ===== PROCESS WEBHOOK =====
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

    // 🧪 TEST endpoint - GET & POST
    @GetMapping("/webhook/test")
    @PostMapping("/webhook/test")
    public ResponseEntity<String> testWebhook(
            @RequestParam Long orderId,
            @RequestParam(required = false) BigDecimal amount) {

        try {
            String content = "ORDER" + orderId;  // Test with ORDER23 format
            BigDecimal testAmount = amount != null ? amount : new BigDecimal(50000);

            orderService.markOrderPaidByWebhook(content, testAmount);

            log.debug("✅ Test OK");
            return ResponseEntity.ok("✅ Order " + orderId + " PAID");
        } catch (Exception e) {
            log.error("❌ Test error: {}", e.getMessage());
            return ResponseEntity.status(500).body("ERROR: " + e.getMessage());
        }
    }

    // ===== API: Check payment status =====
    @GetMapping("/check-payment/{orderId}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            log.debug("Checking payment status for order {}: {}", orderId, order.getStatus());

            boolean isPaid = order.getStatus() == OrderStatus.PAID;

            return ResponseEntity.ok(Map.of(
                    "orderId", order.getId(),
                    "status", order.getStatus().toString(),
                    "isPaid", isPaid,
                    "totalAmount", order.getFinalAmount(),
                    "paidAt", order.getPaidAt() != null ? order.getPaidAt() : ""
            ));
        } catch (Exception e) {
            log.error("❌ Error checking payment status: {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Order not found",
                    "orderId", orderId
            ));
        }
    }
}

