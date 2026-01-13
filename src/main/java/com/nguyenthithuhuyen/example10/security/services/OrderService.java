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

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TableRepository tableRepository;
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ==========================================================
    // API CHUNG (KHÁCH + NHÂN VIÊN)
    // ==========================================================
    @Transactional
    public Order processOrder(Order orderRequest, String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        orderRequest.setUser(user);

        if (orderRequest.getTable() == null || orderRequest.getTable().getId() == null)
            throw new RuntimeException("Table is required");

        Long tableId = orderRequest.getTable().getId();

        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found: " + tableId));

        Order existingOrder = orderRepository
                .findFirstByTable_IdAndStatusIn(
                        tableId,
                        Arrays.asList(OrderStatus.PENDING, OrderStatus.PREPARING)
                ).orElse(null);

        if (existingOrder == null) {
            return createNewOrder(orderRequest, user, table);
        }

        return addItemsToOrder(existingOrder, orderRequest.getOrderItems());
    }

    // ==========================================================
    // TẠO ORDER MỚI
    // ==========================================================
    private Order createNewOrder(Order order, User user, TableEntity table) {

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty())
            throw new RuntimeException("Order must contain items");

        order.setUser(user);
        order.setTable(table);

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : order.getOrderItems()) {

            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            item.setOrder(order);
            item.setProduct(product);

            int qty = (item.getQuantity() == null || item.getQuantity() <= 0)
                    ? 1
                    : item.getQuantity();

            // ✅ GIÁ THEO SIZE
            BigDecimal price = getPriceBySize(product, item.getSize());
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));

            item.setPrice(price);
            item.setSubtotal(subtotal);

            total = total.add(subtotal);
        }

        order.setTotalAmount(total);
        order.setDiscount(BigDecimal.ZERO);
        order.setFinalAmount(total);
        order.setStatus(OrderStatus.PREPARING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

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

            int qty = (item.getQuantity() == null || item.getQuantity() <= 0)
                    ? 1
                    : item.getQuantity();

            // ✅ GIÁ THEO SIZE
            BigDecimal price = getPriceBySize(product, item.getSize());
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));

            item.setPrice(price);
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
    // HÀM LẤY GIÁ THEO SIZE (QUAN TRỌNG)
    // ==========================================================
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

    // ==========================================================
    // KHÁCH TẠO ORDER (CÓ SOCKET)
    // ==========================================================
    @Transactional
    public Order createOrder(Order orderRequest, String username) {
        Order saved = processOrder(orderRequest, username);
        messagingTemplate.convertAndSend("/topic/orders", saved);
        return saved;
    }

    // ==========================================================
    // NHÂN VIÊN TẠO ORDER (KHÔNG SOCKET)
    // ==========================================================
    @Transactional
    public Order staffCreateOrder(Order orderRequest, String staffUsername) {
        return processOrder(orderRequest, staffUsername);
    }

    // ==========================================================
    // CÁC API KHÁC (GIỮ NGUYÊN)
    // ==========================================================
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);

        if (status == OrderStatus.PAID || status == OrderStatus.CANCELLED) {
            TableEntity table = order.getTable();
            if (table != null) {
                table.setStatus(Status.FREE);
                tableRepository.save(table);
            }
        }

        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
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
