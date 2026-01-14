package com.nguyenthithuhuyen.example10.security.services;

import com.nguyenthithuhuyen.example10.entity.*;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.entity.enums.Status;
import com.nguyenthithuhuyen.example10.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /* ==========================================================
       KHÁCH TẠO ORDER (CHECKOUT)
       ========================================================== */
    @Transactional
    public Order createOrder(Order orderRequest, String username) {

        /* ===== USER ===== */
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + username));

        /* ===== VALIDATE ITEMS ===== */
        if (orderRequest.getOrderItems() == null
                || orderRequest.getOrderItems().isEmpty()) {
            throw new RuntimeException("Order must contain items");
        }

        /* ===== SET ORDER BASIC ===== */
        orderRequest.setUser(user);
        orderRequest.setStatus(OrderStatus.PENDING);

        // ===== DEFAULT CUSTOMER INFO =====
        if (orderRequest.getCustomerName() == null
                || orderRequest.getCustomerName().isBlank()) {
            orderRequest.setCustomerName(user.getUsername());
        }

        if (orderRequest.getAddress() == null
                || orderRequest.getAddress().isBlank()) {
            orderRequest.setAddress("Tại quán");
        }

        if (orderRequest.getPhone() == null
                || orderRequest.getPhone().isBlank()) {
            orderRequest.setPhone("0000000000");
        }

        if (orderRequest.getPaymentMethod() == null
                || orderRequest.getPaymentMethod().isBlank()) {
            orderRequest.setPaymentMethod("CASH");
        }

        if (orderRequest.getDiscount() == null) {
            orderRequest.setDiscount(BigDecimal.ZERO);
        }

        /* ===== PROCESS ITEMS ===== */
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : orderRequest.getOrderItems()) {

            Product product = productRepository
                    .findById(item.getProduct().getId())
                    .orElseThrow(() ->
                            new RuntimeException("Product not found"));

            item.setOrder(orderRequest);
            item.setProduct(product);

            int qty = (item.getQuantity() == null || item.getQuantity() <= 0)
                    ? 1
                    : item.getQuantity();

            BigDecimal price = getPriceBySize(product, item.getSize());
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));

            item.setQuantity(qty);
            item.setPrice(price);
            item.setSubtotal(subtotal);

            total = total.add(subtotal);
        }

        /* ===== TOTAL ===== */
        orderRequest.setTotalAmount(total);
        orderRequest.setFinalAmount(
                total.subtract(orderRequest.getDiscount())
        );

        /* ===== SAVE ===== */
        Order saved = orderRepository.save(orderRequest);

        /* ===== SOCKET ===== */
        messagingTemplate.convertAndSend("/topic/orders", saved);

        return saved;
    }

    /* ==========================================================
       NHÂN VIÊN TẠO ORDER
       ========================================================== */
    @Transactional
    public Order staffCreateOrder(Order orderRequest, String staffUsername) {
        return createOrder(orderRequest, staffUsername);
    }

    /* ==========================================================
       UPDATE STATUS
       ========================================================== */
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        order.setStatus(status);
        return orderRepository.save(order);
    }

    /* ==========================================================
       LẤY GIÁ THEO SIZE
       ========================================================== */
    private BigDecimal getPriceBySize(Product product, String size) {

        if (size == null || size.isBlank())
            throw new RuntimeException("Size is required");

        return product.getPrices().stream()
                .filter(p -> p.getSize().equalsIgnoreCase(size))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Price not found for size: " + size))
                .getPrice();
    }

    /* ==========================================================
       OTHER APIs
       ========================================================== */
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Map<String, Object>> getTopSellingProducts(int topN) {
        return orderRepository.findTopSellingProducts(
                OrderStatus.PAID,
                PageRequest.of(0, topN)
        );
    }

    public List<Map<String, Object>> getRevenueByCategory() {
        return orderRepository.findRevenueByCategory(OrderStatus.PAID);
    }

    public List<Map<String, Object>> getRevenueByDay() {
        return orderRepository.findRevenueByDay(OrderStatus.PAID);
    }
}
