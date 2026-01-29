package com.nguyenthithuhuyen.example10.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
         String productImage,
        String size,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {}
