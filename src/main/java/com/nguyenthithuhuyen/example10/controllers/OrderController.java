package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.dto.CreateOrderRequest;
import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.security.services.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    /* =====================================================
       USER / ADMIN – TẠO ORDER
       (NHÂN VIÊN KHÔNG ĐƯỢC TẠO)
       ===================================================== */
@PostMapping
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public ResponseEntity<Order> createOrder(
        @Valid @RequestBody CreateOrderRequest request
) {
    String username = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

    log.info("🛒 Create order request = {}", request);

    Order created = orderService.createOrder(request, username);
    return ResponseEntity.status(201).body(created);
}

    /* =====================================================
       USER / ADMIN – XEM ĐƠN CỦA CHÍNH MÌNH
       ===================================================== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/my")
    public ResponseEntity<?> getMyOrders() {
        try {
            String username = SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getName();

            List<Order> orders = orderService.getOrdersByUsername(username);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("❌ Error getting orders", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /* =====================================================
       USER / ADMIN – XEM CHI TIẾT ĐƠN CỦA MÌNH
       ===================================================== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/my/{orderId}")
    public ResponseEntity<Order> getMyOrderDetail(
            @PathVariable Long orderId
    ) {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Order order = orderService.getOrderById(orderId);

        // 🔐 chỉ được xem đơn của mình
        if (!order.getUser().getUsername().equals(username)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(order);
    }

    /* =====================================================
       STAFF / ADMIN – XEM TẤT CẢ ĐƠN
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /* =====================================================
       STAFF / ADMIN – UPDATE TRẠNG THÁI ĐƠN
       (CHECK, XÁC NHẬN, HOÀN THÀNH...)
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {

        OrderStatus newStatus =
                OrderStatus.valueOf(status.toUpperCase());

        Order updated = orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok(updated);
    }

    /* =====================================================
       STATS – TOP SELLING PRODUCTS
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @GetMapping("/stats/top-selling")
    public ResponseEntity<List<?>> getTopSellingProducts(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(
                orderService.getTopSellingProducts(limit)
        );
    }

    /* =====================================================
       STATS – DAILY REVENUE
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @GetMapping("/stats/daily-revenue")
    public ResponseEntity<List<?>> getDailyRevenue() {
        return ResponseEntity.ok(
                orderService.getRevenueByDay()
        );
    }

    /* =====================================================
       STATS – REVENUE BY CATEGORY
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @GetMapping("/stats/revenue-by-category")
    public ResponseEntity<List<?>> getRevenueByCategory() {
        return ResponseEntity.ok(
                orderService.getRevenueByCategory()
        );
    }
}
