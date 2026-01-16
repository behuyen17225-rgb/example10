package com.nguyenthithuhuyen.example10.payload.request;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public class SePayWebhookRequest {

    private BigDecimal amount;     // số tiền nhận
    private String content;        // nội dung chuyển khoản (paymentRef)
    private String description;    // mô tả giao dịch
    private String transactionDate; // ngày giao dịch
    private String referenceCode;   // mã tham chiếu
    private String senderName;      // tên người gửi
    private String senderAccount;   // tài khoản người gửi
    
    // Capture any other fields Sepay might send
    private Map<String, Object> otherFields = new HashMap<>();
    
    @JsonAnySetter
    public void setOtherFields(String name, Object value) {
        otherFields.put(name, value);
    }
}
