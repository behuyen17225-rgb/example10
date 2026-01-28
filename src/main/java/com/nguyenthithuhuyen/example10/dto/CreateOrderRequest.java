package com.nguyenthithuhuyen.example10.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "orderType is required")
    private String orderType;

    private Long tableId;

    private LocalDateTime pickupTime;

    @NotBlank(message = "customerName is required")
    private String customerName;

    @NotBlank(message = "phone is required")
    private String phone;

    private String address;

    @NotBlank(message = "paymentMethod is required")
    private String paymentMethod;

    @NotEmpty(message = "items must not be empty")
    private List<OrderItemRequest> items;
}
