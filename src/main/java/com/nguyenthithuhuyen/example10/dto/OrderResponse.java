package com.nguyenthithuhuyen.example10.dto;

import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.entity.enums.OrderType;  
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        String phone,
        OrderStatus status,
        OrderType orderType,
        String paymentMethod,
        String adress,
        BigDecimal totalAmount,
        BigDecimal finalAmount,
        LocalDateTime pickupTime,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {}
