package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.security.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    /* =====================================================
       ADMIN / MODERATOR – LẤY TẤT CẢ ORDER
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /* =====================================================
       KHÁCH TẠO / THÊM ORDER (CÓ SOCKET)
       ===================================================== */
@PostMapping
public ResponseEntity<?> createOrder(@RequestBody Order order) {
    String username = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();
    try {
        Order created = orderService.createOrder(order, username);
        return ResponseEntity.status(201).body(created);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
    /* =====================================================
       NHÂN VIÊN TẠO / THÊM ORDER (KHÔNG SOCKET)
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @PostMapping("/staff")
    public ResponseEntity<?> staffCreateOrder(@RequestBody Order orderRequest) {
        String staffUsername = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        try {
            Order created = orderService.staffCreateOrder(orderRequest, staffUsername);

            // gửi riêng cho staff (nếu cần realtime)
            messagingTemplate.convertAndSend("/topic/staff/orders", created);

            return ResponseEntity.status(201).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /* =====================================================
       LẤY ORDER ĐANG MỞ THEO BÀN
       ===================================================== */

    /* =====================================================
       UPDATE TRẠNG THÁI ORDER
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        try {
            OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
            Order updated = orderService.updateOrderStatus(id, newStatus);

            messagingTemplate.convertAndSend("/topic/orders", updated);
            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Trạng thái không hợp lệ");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    /* =====================================================
       REPORT
       ===================================================== */
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/report/top-products")
    public ResponseEntity<List<Map<String, Object>>> getTopSellingProducts(
            @RequestParam(defaultValue = "5") int topN
    ) {
        return ResponseEntity.ok(orderService.getTopSellingProducts(topN));
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/report/revenue-by-category")
    public ResponseEntity<List<Map<String, Object>>> getRevenueByCategory() {
        return ResponseEntity.ok(orderService.getRevenueByCategory());
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/report/revenue-by-day")
    public ResponseEntity<List<Map<String, Object>>> getRevenueByDay() {
        return ResponseEntity.ok(orderService.getRevenueByDay());
    }

    /* =====================================================
       LẤY ORDER THEO BÀN + STATUS
       ===================================================== */
}
