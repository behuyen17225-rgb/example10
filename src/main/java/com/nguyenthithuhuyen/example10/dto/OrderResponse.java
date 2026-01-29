package com.nguyenthithuhuyen.example10.dto;

import com.nguyenthithuhuyen.example10.entity.enums.OrderStatus;
import com.nguyenthithuhuyen.example10.entity.enums.OrderType;  
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String customerName,
        String phone,
        OrderStatus status,
        OrderType orderType,
        BigDecimal totalAmount,
        BigDecimal finalAmount,
        LocalDateTime createdAt
) {}
