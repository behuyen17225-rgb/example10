package com.nguyenthithuhuyen.example10.dto;

import java.math.BigDecimal;

import com.nguyenthithuhuyen.example10.entity.OrderItem;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String productImage,
        String size,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
) {

    public static OrderItemResponse fromEntity(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),                                   // id
                item.getProduct().getId(),                      // productId
                item.getProduct().getName(),                    // productName
                item.getProduct().getImageUrl(),                   // productImage
                item.getSize(),                    // size
                item.getQuantity(),                             // quantity
                item.getPrice(),                                // price
                item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())) // subtotal
        );
    }
}
