package com.nguyenthithuhuyen.example10.payload.response;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class QrResponse {
    private String qrUrl;
    private String paymentRef;
    private BigDecimal amount;
}
