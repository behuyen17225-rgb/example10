package com.nguyenthithuhuyen.example10.security.services;

import com.nguyenthithuhuyen.example10.entity.*;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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

    // ===== USER =====
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    if (orderRequest.getOrderItems() == null || orderRequest.getOrderItems().isEmpty()) {
        throw new RuntimeException("Order must contain items");
    }

    Order order = new Order();
    order.setUser(user);

    // ===== STATUS =====
    order.setStatus(OrderStatus.PENDING);

    // ===== PAYMENT METHOD =====
    String paymentMethod = Optional.ofNullable(orderRequest.getPaymentMethod())
            .map(String::toUpperCase)
            .orElseThrow(() -> new RuntimeException("Payment method is required"));

    if (!List.of("CASH", "BANK", "MOMO").contains(paymentMethod)) {
        throw new RuntimeException("Invalid payment method");
    }

    order.setPaymentMethod(paymentMethod);

    // ===== TABLE =====
    if (orderRequest.getTable() != null) {
        order.setTable(orderRequest.getTable());
    }

    // ===== CUSTOMER INFO =====
    order.setCustomerName(
            Optional.ofNullable(orderRequest.getCustomerName())
                    .filter(s -> !s.isBlank())
                    .orElse(user.getUsername())
    );

    order.setPhone(
            Optional.ofNullable(orderRequest.getPhone())
                    .filter(s -> !s.isBlank())
                    .orElse("0000000000")
    );

    order.setAddress(
            Optional.ofNullable(orderRequest.getAddress())
                    .filter(s -> !s.isBlank())
                    .orElse("Tại quán")
    );

    order.setNote(orderRequest.getNote());

    // ===== DISCOUNT =====
    BigDecimal discount = Optional.ofNullable(orderRequest.getDiscount())
            .filter(d -> d.compareTo(BigDecimal.ZERO) >= 0)
            .orElse(BigDecimal.ZERO);

    order.setDiscount(discount);

    // ===== ITEMS & TOTAL =====
    BigDecimal total = BigDecimal.ZERO;
    List<OrderItem> items = new ArrayList<>();

    for (OrderItem reqItem : orderRequest.getOrderItems()) {

        if (reqItem.getProduct() == null || reqItem.getProduct().getId() == null) {
            throw new RuntimeException("Product id is required");
        }

        Product product = productRepository.findById(reqItem.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int qty = reqItem.getQuantity() != null && reqItem.getQuantity() > 0
                ? reqItem.getQuantity()
                : 1;

        BigDecimal price = resolvePrice(product, reqItem.getSize());
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(qty);
        item.setSize(reqItem.getSize());
        item.setPrice(price);
        item.setSubtotal(subtotal);

        items.add(item);
        total = total.add(subtotal);
    }

    order.setOrderItems(items);
    order.setTotalAmount(total);

    BigDecimal finalAmount = total.subtract(discount);
    if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
        throw new RuntimeException("Final amount cannot be negative");
    }

    order.setFinalAmount(finalAmount);

    Order saved = orderRepository.save(order);

    // ===== PAYMENT REF (Set sau khi có ID) =====
    if (!paymentMethod.equals("CASH")) {
        saved.setPaymentRef("ORDER_" + saved.getId());
        saved = orderRepository.save(saved);
    }

    // ===== WS NOTIFY =====
    messagingTemplate.convertAndSend("/topic/orders", saved);

    return saved;
}

private BigDecimal resolvePrice(Product product, String size) {

    if (product.getPrices() == null || product.getPrices().isEmpty()) {
        throw new RuntimeException("Product has no prices");
    }

    if (size == null || size.isBlank()) {
        return product.getPrices().get(0).getPrice(); // default size
    }

    return product.getPrices().stream()
            .filter(p -> p.getSize().equalsIgnoreCase(size))
            .findFirst()
            .orElseThrow(() ->
                    new RuntimeException("Price not found for size: " + size))
            .getPrice();
}


@Transactional
public void markOrderPaidByWebhook(String content, BigDecimal amount) {

    log.debug("SePay webhook received: content={}, amount={}", content, amount);

    // 1️⃣ Tách ORDER_ID từ content (format: ORDER_123 hoặc ORDER23)
    // Accept both: ORDER_123 or ORDER123
    Pattern pattern = Pattern.compile("ORDER[_]?(\\d+)");
    Matcher matcher = pattern.matcher(content);

    if (!matcher.find()) {
        log.error("❌ CANNOT PARSE! Content format invalid: '{}'. Expected ORDER123 or ORDER_123", content);
        throw new RuntimeException("Invalid payment content format. Expected ORDER_ID, got: " + content);
    }

    Long orderId = Long.parseLong(matcher.group(1));
    log.info("✅ Extracted ORDER_ID from webhook: {}", orderId);

    // 2️⃣ Tìm order
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.error("❌ Order not found: {}", orderId);
                return new RuntimeException("Order not found: " + orderId);
            });

    // 3️⃣ Check đã thanh toán chưa
    if (order.getStatus() == OrderStatus.PAID) {
        log.warn("⚠️ Order {} already PAID - ignoring duplicate webhook", orderId);
        return;
    }

    // 4️⃣ Check số tiền (nếu SePay gửi amount)
    if (amount != null) {
        BigDecimal orderAmount = order.getFinalAmount();
        log.debug("Amount comparison: webhook={}, order={}", amount, orderAmount);
        
        if (amount.compareTo(orderAmount) != 0) {
            log.warn("⚠️ Amount mismatch! webhook={}, order={} - but still marking as PAID", amount, orderAmount);
        }
    } else {
        log.debug("⚠️ SePay did not send amount - skipping amount verification");
    }

    // 5️⃣ Update order status
    order.setStatus(OrderStatus.PAID);
    order.setPaidAt(LocalDateTime.now());
    orderRepository.save(order);
    
    log.info("✅ Order {} marked as PAID successfully", orderId);

    log.debug("Order {} marked as PAID", orderId);
}


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
    public List<Order> getOrdersByUsername(String username) {
    return orderRepository.findByUser_Username(username);
}


    public List<Map<String, Object>> getRevenueByDay() {
        return orderRepository.findRevenueByDay(OrderStatus.PAID);
    }
}
