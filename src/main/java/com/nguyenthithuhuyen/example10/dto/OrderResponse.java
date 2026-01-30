package com.nguyenthithuhuyen.example10.dto;

import com.nguyenthithuhuyen.example10.entity.Order;
import com.nguyenthithuhuyen.example10.entity.OrderItem;
import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.entity.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        String phone,
        Long tableId,
        OrderStatus status,
        OrderType orderType,
        String paymentMethod,
        String paymentRef,
        String adress,
        BigDecimal totalAmount,
        BigDecimal finalAmount,
        LocalDateTime pickupTime,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {

    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getUsername(),
                order.getPhone(),
                order.getTable() != null ? order.getTable().getId() : null,
                order.getStatus(),
                order.getOrderType(),
                order.getPaymentMethod(),
                order.getPaymentRef(),
                order.getAddress(),
                order.getTotalAmount(),
                order.getFinalAmount(),
                order.getPickupTime(),
                order.getCreatedAt(),
                order.getOrderItems().stream()
                        .map(OrderItemResponse::fromEntity)
                        .toList()
        );
    }
}
