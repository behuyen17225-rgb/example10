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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TableRepository tableRepository;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ==========================================================
    // HÀM CHUNG DÙNG CHUNG CHO KHÁCH VÀ NHÂN VIÊN (KHÔNG SOCKET)
    // ==========================================================
    @Transactional
    public Order processOrder(Order orderRequest, String username) {

        // 1) XÁC ĐỊNH USER
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        orderRequest.setUser(user);

        // 2) KIỂM TRA BÀN
        if (orderRequest.getTable() == null || orderRequest.getTable().getId() == null)
            throw new RuntimeException("Table is required");

        Long tableId = orderRequest.getTable().getId();

        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found: " + tableId));

        // 3) KIỂM TRA ORDER ĐANG MỞ
        Order existing = orderRepository.findFirstByTable_IdAndStatusIn(
                tableId,
                Arrays.asList(OrderStatus.PENDING, OrderStatus.PREPARING)
        ).orElse(null);

        if (existing == null) {
            // TẠO MỚI
            return createNewOrder(orderRequest, user, table);
        }

        // BÀN ĐANG CÓ ORDER → THÊM MÓN
        return addItemsToOrder(existing, orderRequest.getOrderItems());
    }

    // ==========================================================
    // TẠO MỚI ORDER
    // ==========================================================
    private Order createNewOrder(Order order, User user, TableEntity table) {

        order.setUser(user);
        order.setTable(table);

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty())
            throw new RuntimeException("Order must contain items");

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : order.getOrderItems()) {

            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            item.setOrder(order);
            item.setProduct(product);

            int qty = (item.getQuantity() == null || item.getQuantity() <= 0) ? 1 : item.getQuantity();
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(qty));

            item.setPrice(product.getPrice());
            item.setSubtotal(subtotal);

            total = total.add(subtotal);
        }

        order.setTotalAmount(total);
        order.setDiscount(BigDecimal.ZERO);
        order.setFinalAmount(total);

        order.setStatus(OrderStatus.PREPARING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // CẬP NHẬT TRẠNG THÁI BÀN
        table.setStatus(Status.OCCUPIED);
        tableRepository.save(table);

        return orderRepository.save(order);
    }

    // ==========================================================
    // THÊM MÓN VÀO ORDER ĐANG MỞ
    // ==========================================================
    private Order addItemsToOrder(Order existing, List<OrderItem> newItems) {

        if (newItems == null || newItems.isEmpty())
            throw new RuntimeException("Order must contain items");

        BigDecimal total = existing.getTotalAmount();

        for (OrderItem item : newItems) {

            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            item.setOrder(existing);
            item.setProduct(product);

            int qty = (item.getQuantity() == null || item.getQuantity() <= 0) ? 1 : item.getQuantity();
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(qty));

            item.setPrice(product.getPrice());
            item.setSubtotal(subtotal);

            existing.getOrderItems().add(item);
            total = total.add(subtotal);
        }

        existing.setTotalAmount(total);
        existing.setFinalAmount(total);
        existing.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(existing);
    }

    // ==========================================================
    // KHÁCH TẠO ORDER — GIỮ NGUYÊN TÊN PHƯƠNG THỨC
    // ==========================================================
    @Transactional
    public Order createOrder(Order orderRequest, String username, boolean isStaff) {
        Order saved = processOrder(orderRequest, username);

        // KHÁCH → CÓ SOCKET
        messagingTemplate.convertAndSend("/topic/orders", saved);

        return saved;
    }

    // ==========================================================
    // NHÂN VIÊN TẠO ORDER — GIỮ NGUYÊN TÊN PHƯƠNG THỨC
    // ==========================================================
    @Transactional
    public Order staffCreateOrder(Order orderRequest, String staffUsername) {
        // NHÂN VIÊN → KHÔNG SOCKET
        return processOrder(orderRequest, staffUsername);
    }

    // ==========================================================
    // CÁC HÀM KHÁC GIỮ NGUYÊN
    // ==========================================================

    public Order getOrderByIdAndCheckOwner(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Set<String> roleNames = currentUser.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        if (!roleNames.contains("ROLE_ADMIN") && !roleNames.contains("ROLE_MODERATOR")) {
            if (!order.getUser().getUsername().equals(username)) {
                throw new RuntimeException("Access denied");
            }
        }
        return order;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.PAID || newStatus == OrderStatus.CANCELLED) {
            TableEntity table = order.getTable();
            if (table != null) {
                table.setStatus(Status.FREE);
                tableRepository.save(table);
            }
        }

        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Map<String, Object>> getTopSellingProducts(int topN) {
        return orderRepository.findTopSellingProducts(OrderStatus.PAID, PageRequest.of(0, topN));
    }

    public List<Map<String, Object>> getRevenueByCategory() {
        return orderRepository.findRevenueByCategory(OrderStatus.PAID);
    }

    public List<Map<String, Object>> getRevenueByDay() {
        return orderRepository.findRevenueByDay(OrderStatus.PAID);
    }

    public Order getOpenOrderByTableId(Long tableId) {
        return orderRepository.findFirstByTable_IdAndStatusIn(
                tableId,
                Arrays.asList(OrderStatus.PENDING, OrderStatus.PREPARING)
        ).orElse(null);
    }

    public List<Order> findByTableIdAndStatus(Long tableId, OrderStatus status) {
        if (status == null)
            return orderRepository.findByTable_Id(tableId);

        return orderRepository.findByTable_IdAndStatus(tableId, status);
    }
}
