package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderRepository orderRepository;
    private final VnPayService vnPayService;

    // TẠO LINK THANH TOÁN
    @PostMapping("/vnpay/{orderId}")
    public Map<String, String> payByVnPay(@PathVariable Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order not payable");
        }

        order.setPaymentMethod("VNPAY");
        orderRepository.save(order);

        String paymentUrl = vnPayService.createPaymentUrl(order);

        return Map.of("paymentUrl", paymentUrl);
    }

    // CALLBACK VNPAY
    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> params) {

        Long orderId = Long.valueOf(params.get("vnp_TxnRef"));
        String responseCode = params.get("vnp_ResponseCode");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if ("00".equals(responseCode)) {
            order.setStatus(OrderStatus.PAID);
        } else {
            order.setStatus(OrderStatus.CANCELLED);
        }

        orderRepository.save(order);

        return "Payment result: " + order.getStatus();
    }
}
