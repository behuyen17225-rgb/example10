package com.nguyenthithuhuyen.example10.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SePayWebhookRequest {
    private String transaction_id;
    private String bank;
    private BigDecimal amount;
    private String description;
    private String status;
    private String reference_code;

    // getters & setters
}
