package com.nguyenthithuhuyen.example10.controllers;
import com.nguyenthithuhuyen.example10.entity.Chat;
import com.nguyenthithuhuyen.example10.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatRepository repo;

    @MessageMapping("/chat.send.socket")
    public void send(Chat message) {

        repo.save(message);

        messagingTemplate.convertAndSend(
            "/topic/chat/" + message.getConversationId(),
            message
        );
    }
}
