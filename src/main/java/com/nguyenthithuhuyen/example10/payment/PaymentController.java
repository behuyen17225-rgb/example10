package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.payload.response.QrResponse;
import com.nguyenthithuhuyen.example10.security.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final SePayService sePayService;

    @PostMapping("/{id}/pay/online")
    public QrResponse payOnline(@PathVariable Long id) {

        Order order = orderService.getOrderById(id);

        if (!"CARD".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new RuntimeException("Order is not CARD payment");
        }

if (
    order.getStatus() != OrderStatus.PENDING &&
    order.getStatus() != OrderStatus.PREPARING
) {
    throw new RuntimeException(
        "Order cannot be paid in status: " + order.getStatus()
    );
}
        return sePayService.createQr(order);
    }

    // 🔍 CHECK order status
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> checkOrderStatus(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getId());
        response.put("status", order.getStatus());
        response.put("finalAmount", order.getFinalAmount());
        response.put("paymentRef", order.getPaymentRef());
        response.put("paidAt", order.getPaidAt());
        response.put("isPaid", order.getStatus() == OrderStatus.PAID);
        
        return ResponseEntity.ok(response);
    }
}

