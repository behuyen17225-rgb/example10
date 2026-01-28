package com.nguyenthithuhuyen.example10.dto;
import com.nguyenthithuhuyen.example10.dto.OrderItemRequest;
import com.nguyenthithuhuyen.example10.entity.enums.OrderType;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateOrderRequest {

    private String orderType; // DINE_IN | TAKE_AWAY | PRE_ORDER
    private Long tableId;      // nullable
    private LocalDateTime pickupTime; // chỉ dùng cho PRE_ORDER

    private String customerName;
    private String address;
    private String phone;
    private String paymentMethod;
    private String note;

    private List<OrderItemRequest> items;
}
