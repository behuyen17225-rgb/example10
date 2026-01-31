package com.nguyenthithuhuyen.example10.payload.websocket;

import lombok.Data;

@Data
public class WsChatMessage {
    private String conversationId;
    private String sender;   // CUSTOMER | STAFF
    private String content;
}
