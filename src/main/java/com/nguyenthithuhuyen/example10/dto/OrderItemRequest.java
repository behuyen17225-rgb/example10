package com.nguyenthithuhuyen.example10.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {

    private Long productId;   // ID sản phẩm
    private Integer quantity; // số lượng
    private String size;  // giá tại thời điểm đặt
}
