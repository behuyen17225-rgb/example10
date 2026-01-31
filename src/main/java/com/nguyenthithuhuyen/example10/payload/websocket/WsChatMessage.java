package com.nguyenthithuhuyen.example10.payload.websocket;

import lombok.Data;

@Data
public class WsChatMessage {
    private Long conversationId;   // ✅ Long
    private String sender;          // CUSTOMER | STAFF
    private String content;
    private Long staffId;           // nullable
}


