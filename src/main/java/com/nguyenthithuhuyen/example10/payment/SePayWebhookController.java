package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.payload.request.SePayWebhookRequest;
import com.nguyenthithuhuyen.example10.security.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sepay")
@RequiredArgsConstructor
public class SePayWebhookController {

    private final OrderService orderService;

    @PostMapping("/webhook")
    public ResponseEntity<String> sepayWebhook(
            @RequestBody SePayWebhookRequest req) {

        orderService.markOrderPaidByWebhook(
                req.getContent(),
                req.getAmount()
        );

        return ResponseEntity.ok("OK");
    }
}
