package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.security.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /* =====================================================
       USER / ADMIN – TẠO ORDER
       ===================================================== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Order created = orderService.createOrder(order, username);
        return ResponseEntity.status(201).body(created);
    }

    /* =====================================================
       USER / ADMIN – XEM TẤT CẢ ĐƠN CỦA CHÍNH MÌNH
       ===================================================== */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/my")
    public ResponseEntity<List<Order>> getMyOrders() {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return ResponseEntity.ok(
                orderService.getOrdersByUsername(username)
        );
    }

    /* =====================================================
       USER / ADMIN – XEM CHI TIẾT 1 ĐƠN CỦA CHÍNH MÌNH
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

        // 🔐 BẢO MẬT: chỉ xem đơn của mình
        if (!order.getUser().getUsername().equals(username)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(order);
    }

    /* =====================================================
       ADMIN / MODERATOR – XEM TẤT CẢ ORDER
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /* =====================================================
       ADMIN / MODERATOR – UPDATE STATUS
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {

        OrderStatus newStatus =
                OrderStatus.valueOf(status.toUpperCase());

        return ResponseEntity.ok(
                orderService.updateOrderStatus(id, newStatus)
        );
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
