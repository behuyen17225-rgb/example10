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

    // Client gửi vào /app/chat.send
    @MessageMapping("/chat.send")
    public void send(WsChatMessage message) {

        /* ================== 1️⃣ SAVE DB ================== */
        chatService.saveMessage(
                message.getConversationId(),
                message.getSender(),
                message.getContent()
        );

        /* ================== 2️⃣ UPDATE CONVERSATION ================== */
        Conversation c = conversationRepo
                .findById(message.getConversationId())
                .orElseThrow();
if ("STAFF".equals(message.getSender()) && c.getStaffId() == null) {

    Authentication auth = SecurityContextHolder
            .getContext()
            .getAuthentication();

    if (auth != null && auth.isAuthenticated()) {
        Object principal = auth.getPrincipal();

        if (principal instanceof UserDetailsImpl userDetails) {
            Long staffId = userDetails.getId(); // ✅ ĐÚNG CLASS
            c.setStaffId(staffId);
        }
    }
}

        /* ================== 3️⃣ SEND CHAT ================== */
        messagingTemplate.convertAndSend(
                "/topic/chat/" + message.getConversationId(),
                message
        );

        /* ================== 4️⃣ 🔔 NOTIFY STAFF ================== */
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
