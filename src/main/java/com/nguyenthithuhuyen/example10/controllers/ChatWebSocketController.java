package com.nguyenthithuhuyen.example10.controllers;

import com.nguyenthithuhuyen.example10.payload.websocket.WsChatMessage;
import com.nguyenthithuhuyen.example10.security.services.SecurityChatService;
import com.nguyenthithuhuyen.example10.security.services.UserDetailsImpl;
import com.nguyenthithuhuyen.example10.entity.Conversation;
import com.nguyenthithuhuyen.example10.repository.ConversationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.nio.file.attribute.UserPrincipal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityChatService chatService;
    private final ConversationRepository conversationRepo;

@MessageMapping("/chat.send")
public void send(@Payload WsChatMessage message) {

    System.out.println("🔥 WS MESSAGE: " + message);

    chatService.saveMessage(
            message.getConversationId(), // ✅ Long
            message.getSender(),
            message.getContent()
    );

    Conversation c = conversationRepo
            .findById(message.getConversationId())
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

    if ("STAFF".equals(message.getSender())
            && c.getStaffId() == null
            && message.getStaffId() != null) {

        c.setStaffId(message.getStaffId());
    }

    c.setUpdatedAt(LocalDateTime.now());
    conversationRepo.save(c);

    messagingTemplate.convertAndSend(
            "/topic/chat/" + c.getId(),
            message
    );

    if ("CUSTOMER".equals(message.getSender())) {
        messagingTemplate.convertAndSend(
                "/topic/staff/notify",
                Map.of(
                        "conversationId", c.getId(),
                        "customerId", c.getCustomerId(),
                        "content", message.getContent()
                )
        );
    }
}


}
