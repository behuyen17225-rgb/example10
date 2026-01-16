package com.nguyenthithuhuyen.example10.payload.request;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SePayWebhookRequest {

    private BigDecimal amount; // số tiền nhận
    private String content;    // nội dung chuyển khoản (paymentRef)
}
