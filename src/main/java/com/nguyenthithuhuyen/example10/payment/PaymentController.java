package com.nguyenthithuhuyen.example10.payment;

import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.payload.response.QrResponse;
import com.nguyenthithuhuyen.example10.security.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final SePayService sePayService;

    @PostMapping("/{id}/pay/online")
    public QrResponse payOnline(@PathVariable Long id) {

        Order order = orderService.getOrderById(id);

        if (!"BANK".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new RuntimeException("Order is not BANK payment");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order already paid");
        }

        return sePayService.createQr(order);
    }
}
