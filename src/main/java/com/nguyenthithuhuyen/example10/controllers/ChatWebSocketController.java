package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.payload.websocket.WsChatMessage;
import com.nguyenthithuhuyen.example10.security.services.SecurityChatService;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityChatService chatService;

    // Client gửi vào /app/chat.send
    @MessageMapping("/chat.send")
    public void send(WsChatMessage message) {

        // 1️⃣ Lưu DB (chia luồng)
        chatService.saveMessage(
                message.getConversationId(),
                message.getSender(),
                message.getContent()
        );

        // 2️⃣ Broadcast đúng conversation
        messagingTemplate.convertAndSend(
                "/topic/chat/" + message.getConversationId(),
                message
        );
    }
}
