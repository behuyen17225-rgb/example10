package com.nguyenthithuhuyen.example10.security.services;

import com.nguyenthithuhuyen.example10.dto.CreateOrderRequest;
import com.nguyenthithuhuyen.example10.dto.OrderItemRequest;
import com.nguyenthithuhuyen.example10.dto.OrderItemResponse;
import com.nguyenthithuhuyen.example10.dto.OrderResponse;
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

    if (req.getTableId() == null) {
        throw new RuntimeException("tableId is required");
    }

    TableEntity table = tableRepo.findById(req.getTableId())
            .orElseThrow(() -> new RuntimeException("Table not found"));

    Order order = new Order();
    order.setUser(user);
    order.setCreatedAt(LocalDateTime.now());
    order.setPaymentMethod(req.getPaymentMethod());

    /* ================= TABLE LOGIC ================= */
    if (req.getTableId() == 1) {

        order.setOrderType(OrderType.TAKE_AWAY);
        order.setTable(table);
        order.setStatus(OrderStatus.PENDING);
        order.setAddress(
                req.getAddress() != null ? req.getAddress() : "Mang về"
        );

    } else {

        if (req.getPickupTime() == null && table.getStatus() != Status.FREE) {
            throw new RuntimeException("Table not available");
        }

        order.setTable(table);
        order.setAddress("Tại quán");

        if (req.getPickupTime() != null) {

            order.setOrderType(OrderType.PREORDER);
            order.setPickupTime(req.getPickupTime());
            order.setStatus(OrderStatus.PENDING);

            if (table.getStatus() == Status.FREE) {
                table.setStatus(Status.RESERVED);
                tableRepo.save(table);
            }

        } else {

            order.setOrderType(OrderType.DINE_IN);
            order.setStatus(OrderStatus.PREPARING);

            table.setStatus(Status.OCCUPIED);
            tableRepo.save(table);
        }
    }

    /* ================= CUSTOMER ================= */
    order.setCustomerName(req.getCustomerName());
    order.setPhone(req.getPhone());

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
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());

        total = total.add(
                price.multiply(BigDecimal.valueOf(itemReq.getQuantity()))
        );

        orderItems.add(item);
    }

    order.setOrderItems(orderItems);
    order.setTotalAmount(total);
    order.setDiscount(BigDecimal.ZERO);
    order.setFinalAmount(total);

    /* ================= SAVE LẦN 1 ================= */
    Order savedOrder = orderRepository.save(order);

    /* ================= SET PAYMENT_REF ================= */
    if ("CARD".equalsIgnoreCase(savedOrder.getPaymentMethod())) {
        savedOrder.setPaymentRef("ORDER_" + savedOrder.getId());
        savedOrder = orderRepository.save(savedOrder);
    }

    return savedOrder;
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
public List<OrderResponse> getAllOrderResponses() {
    return orderRepository.findAll()
            .stream()
            .map(this::mapOrderResponse)
            .toList();
}


public List<OrderResponse> getMyOrderResponses(String username) {
    return orderRepository.findByUser_Username(username)
            .stream()
            .map(this::mapOrderResponse)
            .toList();
}


private OrderResponse mapOrderResponse(Order order) {

    List<OrderItemResponse> items = order.getOrderItems()
            .stream()
            .map(this::mapOrderItem)
            .toList();

    return new OrderResponse(
            order.getId(),
            order.getCustomerName(),
            order.getPhone(),            
            order.getTable().getId(),
            order.getStatus(),
            order.getOrderType(),
            order.getPaymentMethod(),
            order.getPaymentRef(),
            order.getAddress(),
            order.getTotalAmount(),
            order.getFinalAmount(),
            order.getPickupTime(),
            order.getCreatedAt(),
            items
    );
}

    public List<Order> getOrdersByUsername(String username) {
        return orderRepository.findByUser_Username(username);
    }
private OrderItemResponse mapOrderItem(OrderItem item) {

    return new OrderItemResponse(
            item.getId(),
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getProduct().getImageUrl(),
            item.getSize(),
            item.getQuantity(),
            item.getPrice(),
            item.getSubtotal()
    );
}

    public List<Map<String, Object>> getTopSellingProducts(int limit) {

        return orderRepository
                .findTopSellingProducts(
                        OrderStatus.PAID,
                        PageRequest.of(0, limit))
                .stream()
                .map(r -> Map.of(
                        "productId", r[0],
                        "productName", r[1],
                        "quantitySold", r[2]))
                .toList();
    }

    public List<Map<String, Object>> getRevenueByCategory() {

        return orderRepository
                .findRevenueByCategory(OrderStatus.PAID)
                .stream()
                .map(r -> Map.of(
                        "category", r[0],
                        "revenue", r[1]))
                .toList();
    }

    public List<Map<String, Object>> getRevenueByDay() {

        return orderRepository
                .findRevenueByDay(OrderStatus.PAID)
                .stream()
                .map(r -> Map.of(
                        "date", r[0],
                        "revenue", r[1]))
                .toList();
    }

}
