package com.nguyenthithuhuyen.example10.security.services;

import com.nguyenthithuhuyen.example10.dto.CreateOrderRequest;
import com.nguyenthithuhuyen.example10.dto.OrderItemRequest;
import com.nguyenthithuhuyen.example10.entity.*;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.entity.enums.OrderType;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final TableRepository tableRepo;
    private final SimpMessagingTemplate messagingTemplate;

    /*
     * ==========================================================
     * USER / STAFF CREATE ORDER
     * ==========================================================
     */
    
@Transactional
public Order createOrder(CreateOrderRequest req, String username) {

    User user = userRepo.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    Order order = new Order();
    order.setUser(user);

    // ===== ORDER TYPE =====
    OrderType orderType = OrderType.valueOf(req.getOrderType());
    order.setOrderType(orderType);
    order.setCreatedAt(LocalDateTime.now());
    order.setStatus(OrderStatus.PENDING);

    /* ================= TABLE LOGIC ================= */
    if (orderType == OrderType.DINE_IN) {

        if (req.getTableId() == null)
            throw new RuntimeException("Table is required for DINE_IN");

        TableEntity table = tableRepo.findById(req.getTableId())
                .orElseThrow(() -> new RuntimeException("Table not found"));

        if (table.getStatus() != Status.FREE)
            throw new RuntimeException("Table not available");

        order.setTable(table);

        if (req.getPickupTime() != null) {
            // đặt trước
            order.setPickupTime(req.getPickupTime());
            table.setStatus(Status.RESERVED);
            order.setStatus(OrderStatus.PENDING);
        } else {
            // ăn tại chỗ
            table.setStatus(Status.OCCUPIED);
            order.setStatus(OrderStatus.PREPARING);
        }

        tableRepo.save(table);
    }

    /* ================= CUSTOMER INFO ================= */
    order.setCustomerName(req.getCustomerName());
    order.setPhone(req.getPhone());
    order.setAddress(
            orderType == OrderType.TAKE_AWAY
                    ? req.getAddress()
                    : "Tại quán"
    );

    /* ================= ORDER ITEMS ================= */
    BigDecimal total = BigDecimal.ZERO;
    List<OrderItem> orderItems = new ArrayList<>();

    for (OrderItemRequest itemReq : req.getItems()) {

        Product product = productRepo.findById(itemReq.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        BigDecimal price = resolvePrice(product, itemReq.getSize());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setSize(itemReq.getSize());
        item.setQuantity(itemReq.getQuantity());
        item.setPrice(price);

        BigDecimal itemTotal =
                price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
        total = total.add(itemTotal);

        orderItems.add(item);
    }

    order.setOrderItems(orderItems);
    order.setTotalAmount(total);
    order.setDiscount(BigDecimal.ZERO);
    order.setFinalAmount(total);

    return orderRepository.save(order);
}



/*
     * ==========================================================
     * STAFF CREATE ORDER
     * ==========================================================
     */

    /*
     * ==========================================================
     * PRICE RESOLUTION (FIX LỖI resolvePrice)
     * ==========================================================
     */
    private BigDecimal resolvePrice(Product product, String size) {

        if (product.getPrices() == null || product.getPrices().isEmpty()) {
            throw new RuntimeException(
                    "Product has no price configuration: " + product.getName());
        }

        if (size == null || size.isBlank()) {
            // Nếu không truyền size → lấy giá đầu tiên (mặc định)
            return product.getPrices().get(0).getPrice();
        }

        return product.getPrices().stream()
                .filter(pp -> pp.getSize().equalsIgnoreCase(size))
                .findFirst()
                .map(ProductPrice::getPrice)
                .orElseThrow(() -> new RuntimeException(
                        "Price not found for product "
                                + product.getName()
                                + " with size " + size));
    }

    /*
     * ==========================================================
     * PAYMENT WEBHOOK
     * ==========================================================
     */
    @Transactional
    public void markOrderPaidByWebhook(String content, BigDecimal amount) {

        Pattern pattern = Pattern.compile("ORDER[_]?(\\d+)");
        Matcher matcher = pattern.matcher(content);

        if (!matcher.find())
            throw new RuntimeException("Invalid payment content");

        Long orderId = Long.parseLong(matcher.group(1));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() == OrderStatus.PAID)
            return;

        if (amount != null &&
                amount.compareTo(order.getFinalAmount()) != 0) {
            log.warn("Amount mismatch: webhook={}, order={}",
                    amount, order.getFinalAmount());
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());

        orderRepository.save(order);
    }

    /*
     * ==========================================================
     * UPDATE STATUS
     * ==========================================================
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);
        return orderRepository.save(order);
    }

    /*
     * ==========================================================
     * QUERY APIs
     * ==========================================================
     */
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByUsername(String username) {
        return orderRepository.findByUser_Username(username);
    }

public List<Map<String, Object>> getTopSellingProducts(int limit) {

    return orderRepository
            .findTopSellingProducts(
                    OrderStatus.PAID,
                    PageRequest.of(0, limit)
            )
            .stream()
            .map(r -> Map.of(
                    "productId", r[0],
                    "productName", r[1],
                    "quantitySold", r[2]
            ))
            .toList();
}
public List<Map<String, Object>> getRevenueByCategory() {

    return orderRepository
            .findRevenueByCategory(OrderStatus.PAID)
            .stream()
            .map(r -> Map.of(
                    "category", r[0],
                    "revenue", r[1]
            ))
            .toList();
}
public List<Map<String, Object>> getRevenueByDay() {

    return orderRepository
            .findRevenueByDay(OrderStatus.PAID)
            .stream()
            .map(r -> Map.of(
                    "date", r[0],
                    "revenue", r[1]
            ))
            .toList();
}

}
