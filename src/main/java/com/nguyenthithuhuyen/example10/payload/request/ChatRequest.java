package com.nguyenthithuhuyen.example10.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
public class ChatRequest {
    private String conversationId;
    private String sender; // CUSTOMER | STAFF
    private String content;
}
