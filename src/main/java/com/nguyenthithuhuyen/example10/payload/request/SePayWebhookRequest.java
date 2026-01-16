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

    private BigDecimal amount;
    private String content;
    private String description;
    private String transactionDate;
    private String referenceCode;
    private String senderName;
    private String senderAccount;
    private Map<String, Object> otherFields = new HashMap<>();
    
    @JsonAnySetter
    public void setOtherFields(String name, Object value) {
        otherFields.put(name, value);
    }

    // Explicit getters
    public BigDecimal getAmount() { return amount; }
    public String getContent() { return content; }
    public String getDescription() { return description; }
    public String getTransactionDate() { return transactionDate; }
    public String getReferenceCode() { return referenceCode; }
    public String getSenderName() { return senderName; }
    public String getSenderAccount() { return senderAccount; }
    public Map<String, Object> getOtherFields() { return otherFields; }

    // Explicit setters
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setContent(String content) { this.content = content; }
    public void setDescription(String description) { this.description = description; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setSenderAccount(String senderAccount) { this.senderAccount = senderAccount; }
}

