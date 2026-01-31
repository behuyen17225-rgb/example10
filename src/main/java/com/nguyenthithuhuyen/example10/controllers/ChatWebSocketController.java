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
    public void send(WsChatMessage message) {

        // 1️⃣ SAVE MESSAGE
        chatService.saveMessage(
                message.getConversationId(),
                message.getSender(),
                message.getContent()
        );

        // 2️⃣ UPDATE CONVERSATION
        Conversation c = conversationRepo
                .findById(message.getConversationId())
                .orElseThrow();

        // ✅ STAFF reply lần đầu → gán staffId
        if ("STAFF".equals(message.getSender()) && c.getStaffId() == null) {
            c.setStaffId(message.getStaffId());
        }

        c.setUpdatedAt(LocalDateTime.now());
        conversationRepo.save(c);

        // 3️⃣ SEND CHAT
        messagingTemplate.convertAndSend(
                "/topic/chat/" + c.getId(),
                message
        );

        // 4️⃣ NOTIFY STAFF
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
